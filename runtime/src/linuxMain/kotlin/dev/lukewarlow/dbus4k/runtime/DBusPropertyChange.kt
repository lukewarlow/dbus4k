package dev.lukewarlow.dbus4k.runtime

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DBusPropertyChange(
    val interfaceName: String,
    val changed: Map<String, Any?>,
    val invalidated: List<String>,
    val path: String,
    val sender: String
)

private fun DBusSignalMessage.decodePropertiesChanged(): DBusPropertyChange {
    val interfaceName = readString()
    val changed = readStringVariantDictionary()
    val invalidated = readStringArray()

    return DBusPropertyChange(
        interfaceName,
        changed,
        invalidated,
        path(),
        sender()
    )
}

fun DBusConnection.flowOfPropertyChanges(path: String? = null, interfaceName: String? = null): Flow<DBusPropertyChange> = callbackFlow {
    val closeable = onSignal(interfaceName = "org.freedesktop.DBus.Properties", member = "PropertiesChanged", path) { message ->
        val event = message.decodePropertiesChanged()
        if (interfaceName != null && event.interfaceName != interfaceName) return@onSignal

        trySend(event)
    }

    awaitClose { closeable.close() }
}