package dev.lukewarlow.dbus4k.runtime

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DBusInterfacesAdded(
    val path: String,
    val interfaces: Map<String, Map<String, Any?>>
)

private fun DBusSignalMessage.decodeInterfacesAdded(): DBusInterfacesAdded {
    val path = readObjectPath()

    val interfaces = readDictionary("sa{sv}", { readString() }) {
        readDictionary("sv", { readString() }, { readVariant() })
    }

    return DBusInterfacesAdded(path, interfaces)
}

fun DBusConnection.flowOfInterfacesAdded(
    sender: String? = null,
    pathPrefix: String? = null,
    interfaceFilter: String? = null
): Flow<DBusInterfacesAdded> = callbackFlow {
    val closeable = onSignal(interfaceName = "org.freedesktop.DBus.ObjectManager", member = "InterfacesAdded") { message ->
        val event = message.decodeInterfacesAdded()
        if (sender != null && sender != message.sender()) return@onSignal
        if (pathPrefix != null && !event.path.startsWith(pathPrefix)) return@onSignal
        if (interfaceFilter != null && interfaceFilter !in event.interfaces) return@onSignal

        trySend(event)
    }

    awaitClose { closeable.close() }
}