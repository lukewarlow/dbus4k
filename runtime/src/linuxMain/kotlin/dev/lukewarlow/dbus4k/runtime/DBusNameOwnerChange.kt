package dev.lukewarlow.dbus4k.runtime

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DBusNameOwnerChange(
    val name: String?,
    val oldOwner: String?,
    val newOwner: String?,
)

private fun DBusSignalMessage.decodeNameOwnerChanged(): DBusNameOwnerChange {
    val name = readString()
    val oldOwner = readString()
    val newOwner = readString()

    return DBusNameOwnerChange(
        name = name,
        oldOwner = oldOwner.ifEmpty { null },
        newOwner = newOwner.ifEmpty { null },
    )
}

fun DBusConnection.flowOfNameOwnerChanges(nameFilter: String? = null): Flow<DBusNameOwnerChange> = callbackFlow {
    val closeable = onSignal(interfaceName = "org.freedesktop.DBus", member = "NameOwnerChanged", path = "/org/freedesktop/DBus", sender = null) { message ->
        val event = message.decodeNameOwnerChanged()
        if (nameFilter == null || event.name == nameFilter) trySend(event)
    }

    awaitClose { closeable.close() }
}