package dev.lukewarlow.dbus4k.runtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DBusObjectManager(
    private val bus: DBusConnection,
    private val destination: String,
    private val path: String,
) {
    private val interfaceName: String = "org.freedesktop.DBus.ObjectManager"

    suspend fun getManagedObjects(): Map<String, DBusManagedObject> {
        val message = bus.newMethodCall(destination, path, interfaceName, "GetManagedObjects")
        val reply = message.awaitReply(bus)
        val raw = reply.readDictionary("a{oa{sa{sv}}}",{ readObjectPath() }) {
            readDictionary("a{sa{sv}}", { readString() }) {
                readDictionary("a{sv}", { readString() }, { readVariant() })
            }
        }

        return raw.mapValues { DBusManagedObject(it.key, it.value) }
    }

    fun interfacesAdded(): Flow<InterfacesAddedEvent> = bus.signalFlow(interfaceName, "InterfacesAdded", path, destination)
        .map { signal ->
            InterfacesAddedEvent(
                signal.readObjectPath(),
                signal.readDictionary("a{sa{sv}}", { readString() }) {
                    readDictionary("a{sv}", { readString() }, { readVariant() })
                }
            )
        }

    fun interfacesRemoved(): Flow<InterfacesRemovedEvent> = bus.signalFlow(interfaceName, "InterfacesRemoved", path, destination)
        .map { signal ->
            InterfacesRemovedEvent(
                signal.readObjectPath(),
                signal.readStringArray()
            )
        }

    data class InterfacesAddedEvent(
        val objectPath: String,
        val interfaces: Map<String, Map<String, Any?>>,
    )

    data class InterfacesRemovedEvent(
        val objectPath: String,
        val interfaces: List<String>,
    )
}

data class DBusManagedObject(
    val objectPath: String,
    val interfaces: Map<String, Map<String, Any?>>
)