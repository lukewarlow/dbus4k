package dev.lukewarlow.dbus4k.runtime

import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DBusProperties(
    private val bus: DBusConnection,
    private val destination: String,
    private val path: String,
) {
    private val interfaceName: String = "org.freedesktop.DBus.Properties"

    suspend fun getProperty(interfaceName: String, property: String): Any? {
        val message = bus.newMethodCall(destination, path, interfaceName, "Get")
        message.writeString(interfaceName)
        message.writeString(property)
        val reply = message.awaitReply(bus)
        return reply.readVariant()
    }

    suspend inline fun <reified T> getProperty(interfaceName: String, property: String): T {
        val value = getProperty(interfaceName, property)
        return value as? T ?: error("Failed to get property $property")
    }

    suspend fun setProperty(interfaceName: String, property: String, value: Any?) {
        val message = bus.newMethodCall(destination, path, interfaceName, "Set")
        message.writeString(interfaceName)
        message.writeString(property)
        message.writeVariant(value)
        message.awaitReply(bus)
        return
    }

    suspend fun getAllProperties(interfaceName: String): Map<String, Any?> {
        val message = bus.newMethodCall(destination, path, interfaceName, "GetAll")
        message.writeString(interfaceName)
        val reply = message.awaitReply(bus)
        return reply.readDictionary("a{sv}", { readString() }, { readVariant() })
    }

    fun propertiesChanged(): Flow<PropertiesChangedEvent> = bus.signalFlow(interfaceName, "PropertiesChanged", path, destination)
        .map { signal ->
            PropertiesChangedEvent(
                signal.readString(),
                signal.readDictionary("a{sv}", { readString() }, { readVariant() }),
                signal.readStringArray()
            )
        }

    data class PropertiesChangedEvent(
        val interfaceName: String,
        val changedProperties: Map<String, Any?>,
        val invalidatedProperties: List<String>,
    )
}
