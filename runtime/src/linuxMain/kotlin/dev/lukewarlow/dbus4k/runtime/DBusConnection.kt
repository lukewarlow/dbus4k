package dev.lukewarlow.dbus4k.runtime

import cnames.structs.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import platformx.sdbus.*

class DBusConnection(
    internal var pointer: CPointer<sd_bus>
) : AutoCloseable {
    private var closed = false

    internal val signalHandlers = mutableMapOf<SignalMatchRule, MutableList<suspend (DBusSignalMessage) -> Unit>>()
    internal val flowHandlers = mutableMapOf<SignalMatchRule, Channel<DBusSignalMessage>>()
    internal val matchSlots = mutableMapOf<SignalMatchRule, DBusMatchHandle>()

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            runEventLoop()
        }
    }

    private suspend fun runEventLoop() = withContext(Dispatchers.IO) {
        try {
            while (!closed) {
                while (true) {
                    val status = sd_bus_process(pointer, null)
                    dbusCheck(status) { "sd_bus_process failed" }

                    if (status <= 0) break
                }

                val waitStatus = sd_bus_wait(pointer, ULong.MAX_VALUE)
                dbusCheck(waitStatus) { "sd_bus_wait failed" }
            }
        } catch (e: CancellationException) {
            return@withContext
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        sd_bus_unref(pointer)
    }

    companion object {
        fun openSystem(): DBusConnection = memScoped {
            val busPtr = alloc<CPointerVar<sd_bus>>()
            dbusCheck(sd_bus_open_system(busPtr.ptr)) { "Failed to open system bus" }
            return DBusConnection(busPtr.value!!)
        }

        fun openUser(): DBusConnection = memScoped {
            val busPtr = alloc<CPointerVar<sd_bus>>()
            dbusCheck(sd_bus_open_user(busPtr.ptr)) { "Failed to open user bus" }
            return DBusConnection(busPtr.value!!)
        }
    }

    fun newMethodCall(
        destination: String,
        path: String,
        interfaceName: String,
        member: String,
    ): DBusMethodCallMessage = memScoped {
        val out = alloc<CPointerVar<sd_bus_message>>()
        dbusCheck(sd_bus_message_new_method_call(
            this@DBusConnection.pointer,
            out.ptr,
            destination,
            path,
            interfaceName,
            member
        )) { "Failed to create method call" }

        DBusMethodCallMessage(out.value!!)
    }

    suspend fun getProperty(
        destination: String,
        path: String,
        interfaceName: String,
        propertyName: String
    ): Any? {
        val message = this.newMethodCall(
            destination,
            path,
            "org.freedesktop.DBus.Properties",
            "Get"
        )

        message.writeString(interfaceName)
        message.writeString(propertyName)

        val reply = message.awaitReply(this)

        return reply.readVariant()
    }

    suspend inline fun <reified T> getProperty(
        destination: String,
        path: String,
        interfaceName: String,
        propertyName: String
    ): T {
        val value = getProperty(destination, path, interfaceName, propertyName)
        return value as? T ?: error("Failed to get property $propertyName")
    }

    suspend fun DBusConnection.getAllProperties(
        destination: String,
        path: String,
        interfaceName: String,
    ): Map<String, Any?> {
        val message = this.newMethodCall(
            destination,
            path,
            "org.freedesktop.DBus.Properties",
            "GetAll"
        )

        message.writeString(interfaceName)

        val reply = message.awaitReply(this)

        return reply.readStringVariantDictionary()
    }

    suspend fun setProperty(
        destination: String,
        path: String,
        interfaceName: String,
        propertyName: String,
        value: Any?
    ) {
        val message = this.newMethodCall(
            destination,
            path,
            "org.freedesktop.DBus.Properties",
            "Set"
        )

        message.writeString(interfaceName)
        message.writeString(propertyName)
        message.writeVariant(value)

        message.awaitReply(this)
    }

    fun signalFlow(
        interfaceName: String,
        member: String,
        path: String? = null,
        sender: String? = null,
    ): Flow<DBusSignalMessage> = channelFlow {
        val rule = SignalMatchRule(sender, path, interfaceName, member)

        registerSignalMatch(rule)

        val channel = Channel<DBusSignalMessage>(Channel.UNLIMITED)
        flowHandlers[rule] = channel

        val forwarder = launch(Dispatchers.Default) {
            for (message in channel) {
                send(message)
            }
        }

        awaitClose {
            forwarder.cancel()
            channel.close()
            flowHandlers.remove(rule)

            // If no callbacks of any kind remain, remove match rule
            val hasCallbacks = signalHandlers[rule]?.isNotEmpty() == true
            val hasFlows = flowHandlers.containsKey(rule)

            if (!hasCallbacks && !hasFlows) {
                unregisterSignalMatch(rule)
            }
        }
    }

    fun DBusConnection.onSignal(
        interfaceName: String,
        member: String,
        path: String? = null,
        sender: String? = null,
        handler: suspend (DBusSignalMessage) -> Unit
    ): AutoCloseable {
        val rule = SignalMatchRule(sender, path, interfaceName, member)

        registerSignalMatch(rule)

        val handlers = signalHandlers.getOrPut(rule) { mutableListOf() }
        handlers += handler

        return object : AutoCloseable {
            override fun close() {
                handlers -= handler
                if (handlers.isEmpty()) {
                    signalHandlers.remove(rule)

                    val hasFlow = flowHandlers.contains(rule)
                    if (!hasFlow)
                        unregisterSignalMatch(rule)
                }
            }
        }
    }

    private fun registerSignalMatch(rule: SignalMatchRule) {
        if (rule in matchSlots) return

        memScoped {
            val slotPtr = alloc<CPointerVar<sd_bus_slot>>()
            val contextRef = StableRef.create(SignalCallbackContext(this@DBusConnection, rule))

            val status = sd_bus_match_signal(
                pointer,
                slotPtr.ptr,
                rule.sender,
                rule.path,
                rule.interfaceName,
                rule.member,
                staticSignalCallbackFunc,
                contextRef.asCPointer()
            )
            dbusCheck(status)

            matchSlots[rule] = DBusMatchHandle(slotPtr.value!!, contextRef)
        }
    }

    private fun DBusConnection.unregisterSignalMatch(rule: SignalMatchRule) {
        matchSlots.remove(rule)?.close()
    }

    internal fun dispatchSignal(rule: SignalMatchRule, message: DBusSignalMessage) {
        val handlers = signalHandlers[rule]?.toList() ?: emptyList()

        for (handler in handlers) {
            scope.launch {
                try {
                    handler(message)
                } catch (t: Throwable) {
                    println("dbus4k: signal handler threw: $t")
                }
            }
        }

        flowHandlers[rule]?.trySend(message)
    }
}

@ConsistentCopyVisibility
data class DBusMatchHandle internal constructor(
    private val slot: CPointer<sd_bus_slot>,
    private val userdata: StableRef<SignalCallbackContext>
): AutoCloseable {
    override fun close() {
        sd_bus_slot_unref(slot)
        userdata.dispose()
    }
}

@CName("dbus4k_signal_callback")
private fun staticSignalCallback(
    message: CPointer<sd_bus_message>?,
    userdata: COpaquePointer?,
    error: CPointer<sd_bus_error>?
): Int {
    if (message == null || userdata == null)
        return 0

    val context = userdata.asStableRef<SignalCallbackContext>().get()
    val connection = context.connection
    val rule = context.rule
    val message = DBusSignalMessage(message)
    connection.dispatchSignal(rule, message)

    return 0
}

private val staticSignalCallbackFunc: sd_bus_message_handler_t = staticCFunction(::staticSignalCallback)

internal data class SignalCallbackContext(
    val connection: DBusConnection,
    val rule: SignalMatchRule,
)