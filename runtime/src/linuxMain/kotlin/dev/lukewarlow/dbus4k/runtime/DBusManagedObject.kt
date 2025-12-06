package dev.lukewarlow.dbus4k.runtime

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

data class DBusManagedObject(
    val path: String,
    val interfaces: Map<String, Map<String, Any?>>
)

private fun DBusReplyMessage.decodeManagedObject(): Map<String, DBusManagedObject> {
    val root = readDictionary("oa{sa{sv}}", {readObjectPath()}) {
        readDictionary("sa{sv}", {readString()}) {
            readStringVariantDictionary()
        }
    }

    return root.mapValues { DBusManagedObject(it.key, it.value) }
}

suspend fun DBusConnection.getManagedObjects(destination: String, path: String): Map<String, DBusManagedObject> {
    val message = this.newMethodCall(
        destination,
        path,
        "org.freedesktop.DBus.ObjectManager",
        "GetManagedObjects"
    )

    val reply = message.awaitReply(this)
    return reply.decodeManagedObject()
}

fun DBusConnection.managedObjectFlow(destination: String, path: String): Flow<Map<String, DBusManagedObject>> = channelFlow {
    val state = getManagedObjects(destination, path).toMutableMap()
    send(state.toMap())

    val added = flowOfInterfacesAdded(
        destination,
        path,
    )
    val removed = flowOfInterfacesRemoved(
        destination,
        path,
    )

    val job = launch {
        merge(added, removed).collect { event ->
            when (event) {
                is DBusInterfacesAdded -> {
                    val obj = DBusManagedObject(event.path, event.interfaces)
                    state[event.path] = obj
                }
                is DBusInterfacesRemoved -> {
                    state.remove(event.path)
                }
            }
            trySend(state.toMap())
        }
    }

    awaitClose { job.cancel() }
}