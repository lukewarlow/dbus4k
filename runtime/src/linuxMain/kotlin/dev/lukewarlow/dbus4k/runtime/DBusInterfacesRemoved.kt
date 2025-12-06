package dev.lukewarlow.dbus4k.runtime

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DBusInterfacesRemoved(
    val path: String,
    val interfaces: List<String>
)

private fun DBusSignalMessage.decodeInterfacesRemoved(): DBusInterfacesRemoved {
    val path = readObjectPath()
    val interfaces = readStringArray()

    return DBusInterfacesRemoved(path, interfaces)
}

fun DBusConnection.flowOfInterfacesRemoved(
    sender: String? = null,
    pathPrefix: String? = null,
    interfaceFilter: String? = null,
): Flow<DBusInterfacesRemoved> = callbackFlow {
    val closeable = onSignal(interfaceName = "org.freedesktop.DBus.ObjectManager", member = "InterfacesRemoved") { message ->
        val event = message.decodeInterfacesRemoved()
        if (sender != null && sender != message.sender()) return@onSignal
        if (pathPrefix != null && !event.path.startsWith(pathPrefix)) return@onSignal
        if (interfaceFilter != null && interfaceFilter !in event.interfaces) return@onSignal

        trySend(event)
    }

    awaitClose { closeable.close() }
}