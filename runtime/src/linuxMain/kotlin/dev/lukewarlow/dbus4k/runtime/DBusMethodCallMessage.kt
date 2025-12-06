package dev.lukewarlow.dbus4k.runtime

import cnames.structs.sd_bus_message
import cnames.structs.sd_bus_slot
import kotlinx.cinterop.*
import kotlinx.cinterop.alloc
import kotlinx.coroutines.suspendCancellableCoroutine
import platformx.sdbus.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DBusMethodCallMessage(
    pointer: CPointer<sd_bus_message>,
) : DBusMessage(pointer) {
    fun call(bus: DBusConnection, timeoutMicros: ULong = TIMEOUT_USE_DEFAULT): DBusReplyMessage = memScoped {
        val replyPtr = alloc<CPointerVar<sd_bus_message>>()
        val error = alloc<sd_bus_error>()
        val status = sd_bus_call(bus.pointer, pointer, timeoutMicros, error.ptr, replyPtr.ptr)

        if (status < 0) {
            val message = error.message?.toKString() ?: "Unknown error"
            val name = error.name?.toKString() ?: "org.freedesktop.DBus.Error.Failed"

            sd_bus_error_free(error.ptr)

            throw DBusException(status, "D-Bus call failed: $name: $message")
        }

        sd_bus_error_free(error.ptr)

        return DBusReplyMessage(replyPtr.value!!)
    }

    fun callAsync(bus: DBusConnection, timeoutMicros: ULong = TIMEOUT_USE_DEFAULT, onReply: (DBusAsyncResult) -> Unit): DBusCallHandle {
        memScoped {
            val slotPtr = alloc<CPointerVar<sd_bus_slot>>()

            val ref = StableRef.create(onReply)

            val status = sd_bus_call_async(
                bus.pointer,
                slotPtr.ptr,
                pointer,
                asyncCallbackFunc,
                ref.asCPointer(),
                timeoutMicros
            )

            if (status < 0) {
                ref.dispose()
                dbusCheck(status) { "Failed to issue async D-Bus call" }
            }

            return DBusCallHandle(slotPtr.value)
        }
    }

    suspend fun awaitReply(bus: DBusConnection): DBusReplyMessage = suspendCancellableCoroutine { continuation ->
        val handle = callAsync(bus, TIMEOUT_USE_DEFAULT) { result ->
            if (!continuation.isActive) return@callAsync

            when (result) {
                is DBusAsyncResult.Success -> continuation.resume(result.reply)
                is DBusAsyncResult.Error -> continuation.resumeWithException(result.error)
                DBusAsyncResult.Cancelled -> continuation.cancel()
            }
        }

        continuation.invokeOnCancellation {
            handle.cancel()
        }
    }

    fun send(bus: DBusConnection) {
        memScoped {
            dbusCheck(sd_bus_send(bus.pointer, pointer, null)) { "Failed to send method call" }
        }
    }
}


@CName("dbus4k_async_callback")
private fun asyncCallback(message: CPointer<sd_bus_message>?, userdata: COpaquePointer?, error: CPointer<sd_bus_error>?): Int {
    val ref = userdata!!.asStableRef<(DBusAsyncResult) -> Unit>()
    val callback = ref.get()

    try {
        val isError = error?.pointed?.name != null
        val isCancelled = message == null && !isError

        when {
            isError -> {
                val name = error.pointed.name!!.toKString()
                val message = error.pointed.message?.toKString() ?: ""
                val code = sd_bus_error_get_errno(error)
                callback(DBusAsyncResult.Error(DBusException(code, "$name: $message")))
            }
            isCancelled -> callback(DBusAsyncResult.Cancelled)
            else -> callback(DBusAsyncResult.Success(DBusReplyMessage(message!!)))
        }
    } catch (t: Throwable) {
        println("Error in async DBus callback: $t")
    } finally {
        ref.dispose()
    }
    return 0
}

private val asyncCallbackFunc: sd_bus_message_handler_t = staticCFunction(::asyncCallback)


class DBusCallHandle(
    private val slot: CPointer<sd_bus_slot>?,
) {
    fun cancel() {
        if (slot != null) {
            sd_bus_slot_unref(slot)
        }
    }
}

sealed class DBusAsyncResult {
    data class Success(val reply: DBusReplyMessage) : DBusAsyncResult()
    data class Error(val error: DBusException) : DBusAsyncResult()
    object Cancelled : DBusAsyncResult()
}