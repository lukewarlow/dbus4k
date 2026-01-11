import dev.lukewarlow.dbus4k.codegen.ast.DBusArg
import dev.lukewarlow.dbus4k.codegen.ast.DBusArgDirection
import dev.lukewarlow.dbus4k.codegen.ast.DBusInterface
import dev.lukewarlow.dbus4k.codegen.ast.DBusIntrospectionNode
import dev.lukewarlow.dbus4k.codegen.ast.DBusMethod
import dev.lukewarlow.dbus4k.codegen.ast.DBusProperty
import dev.lukewarlow.dbus4k.codegen.ast.DBusPropertyAccess
import dev.lukewarlow.dbus4k.codegen.ast.DBusSignal
import dev.lukewarlow.dbus4k.codegen.ast.DBusSignature
import dev.lukewarlow.dbus4k.codegen.processDBusIntrospectionNode
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessorTest {
    @Test
    fun testWithSettingsPortal() {
        val fileName = "org.freedesktop.portal.Secret.xml"
        val packageName = "dev.lukewarlow.dbus4k.generated"

        val ast = DBusIntrospectionNode(
            name = "/",
            interfaces = listOf(
                DBusInterface(
                    name = "org.freedesktop.portal.Secret",
                    methods = listOf(
                        DBusMethod(
                            name = "RetrieveSecret",
                            inArgs = listOf(
                                DBusArg(
                                    name = "fd",
                                    type = DBusSignature.Primitive('h'),
                                    direction = DBusArgDirection.IN,
                                    annotations = emptyList(), // TODO add annotations
                                ),
                                DBusArg(
                                    name = "options",
                                    type = DBusSignature.Array(
                                        DBusSignature.DictionaryEntry(
                                            key = DBusSignature.Primitive('s'),
                                            value = DBusSignature.Variant
                                        )
                                    ),
                                    direction = DBusArgDirection.IN,
                                    annotations = emptyList(), // TODO add annotations
                                )
                            ),
                            outArgs = listOf(
                                DBusArg(
                                    name = "handle",
                                    type = DBusSignature.Primitive('o'),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                ),
                            ),
                            annotations = emptyList(), // TODO add annotations
                        )
                    ),
                    signals = emptyList(),
                    properties = listOf(
                        DBusProperty(
                            name = "version",
                            type = DBusSignature.Primitive('u'),
                            access = DBusPropertyAccess.READ,
                            annotations = emptyList()
                        )
                    )
                ),
            ),
            childNodes = emptyList(),
        )
        val kotlinCode = processDBusIntrospectionNode(fileName, packageName, ast)
        assertEquals(
            """
                package dev.lukewarlow.dbus4k.generated

                import dev.lukewarlow.dbus4k.runtime.DBusConnection
                import kotlin.Any
                import kotlin.Int
                import kotlin.String
                import kotlin.UInt
                import kotlin.collections.Map

                public class SecretPortal(
                    private val bus: DBusConnection,
                    private val destination: String,
                    private val path: String,
                ) {
                    private val interfaceName: String = "org.freedesktop.portal.Secret"

                    public suspend fun retrieveSecret(fd: Int, options: Map<String, Any?>): String {
                        val message = bus.newMethodCall(destination, path, interfaceName, "RetrieveSecret")
                        message.writeFileDescriptor(fd)
                        message.writeMapVariant(options)
                        val reply = message.awaitReply(bus)
                        return reply.readObjectPath()
                    }

                    public suspend fun getVersion(): UInt = bus.getProperty<UInt>(destination, path, interfaceName, "version")
                }
                """.trimIndent(), kotlinCode.trim())
    }

    @Test
    fun testWithUsbPortal() {
        val fileName = "org.freedesktop.portal.Usb.xml"
        val packageName = "dev.lukewarlow.dbus4k.generated"

        val ast = DBusIntrospectionNode(
            name = "/",
            interfaces = listOf(
                DBusInterface(
                    name = "org.freedesktop.portal.Usb",
                    methods = listOf(
                        DBusMethod(
                            name = "CreateSession",
                            inArgs = listOf(
                                DBusArg(
                                    name = "options",
                                    type = DBusSignature.Array(
                                        DBusSignature.DictionaryEntry(
                                            key = DBusSignature.Primitive('s'),
                                            value = DBusSignature.Variant
                                        )
                                    ),
                                    direction = DBusArgDirection.IN,
                                    annotations = emptyList(), // TODO add annotations
                                )
                            ),
                            outArgs = listOf(
                                DBusArg(
                                    name = "session_handle",
                                    type = DBusSignature.Primitive('o'),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                ),
                            ),
                            annotations = emptyList(),
                        ),
                        DBusMethod(
                            name = "EnumerateDevices",
                            inArgs = listOf(
                                DBusArg(
                                    name = "options",
                                    type = DBusSignature.Array(
                                        DBusSignature.DictionaryEntry(
                                            key = DBusSignature.Primitive('s'),
                                            value = DBusSignature.Variant
                                        )
                                    ),
                                    direction = DBusArgDirection.IN,
                                    annotations = emptyList(), // TODO add annotations
                                )
                            ),
                            outArgs = listOf(
                                DBusArg(
                                    name = "devices",
                                    type = DBusSignature.Array(
                                        DBusSignature.Struct(
                                            listOf(
                                                DBusSignature.Primitive('s'),
                                                DBusSignature.Array(
                                                    DBusSignature.DictionaryEntry(
                                                        key = DBusSignature.Primitive('s'),
                                                        value = DBusSignature.Variant
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                ),
                            ),
                            annotations = emptyList(),
                        ),
                        DBusMethod(
                            name = "AcquireDevices",
                            inArgs = listOf(
                                DBusArg(
                                    name = "parent_window",
                                    type = DBusSignature.Primitive('s'),
                                    direction = DBusArgDirection.IN,
                                    annotations = emptyList(),
                                ),
                                DBusArg(
                                    name = "devices",
                                    type = DBusSignature.Array(
                                        DBusSignature.Struct(
                                            listOf(
                                                DBusSignature.Primitive('s'),
                                                DBusSignature.Array(
                                                    DBusSignature.DictionaryEntry(
                                                        key = DBusSignature.Primitive('s'),
                                                        value = DBusSignature.Variant
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    direction = DBusArgDirection.IN,
                                    annotations = emptyList(), // TODO add annotations
                                ),
                                DBusArg(
                                    name = "options",
                                    type = DBusSignature.Array(
                                        DBusSignature.DictionaryEntry(
                                            key = DBusSignature.Primitive('s'),
                                            value = DBusSignature.Variant
                                        )
                                    ),
                                    direction = DBusArgDirection.IN,
                                    annotations = emptyList(), // TODO add annotations
                                )
                            ),
                            outArgs = listOf(
                                DBusArg(
                                    name = "handle",
                                    type = DBusSignature.Primitive('o'),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                ),
                            ),
                            annotations = emptyList(),
                        ),
                        DBusMethod(
                            name = "FinishAcquireDevices",
                            inArgs = listOf(
                                DBusArg(
                                    name = "handle",
                                    type = DBusSignature.Primitive('o'),
                                    direction = DBusArgDirection.IN,
                                    annotations = emptyList(),
                                ),
                                DBusArg(
                                    name = "options",
                                    type = DBusSignature.Array(
                                        DBusSignature.DictionaryEntry(
                                            key = DBusSignature.Primitive('s'),
                                            value = DBusSignature.Variant
                                        )
                                    ),
                                    direction = DBusArgDirection.IN,
                                    annotations = emptyList(), // TODO add annotations
                                )
                            ),
                            outArgs = listOf(
                                DBusArg(
                                    name = "results",
                                    type = DBusSignature.Array(
                                        DBusSignature.Struct(
                                            listOf(
                                                DBusSignature.Primitive('s'),
                                                DBusSignature.Array(
                                                    DBusSignature.DictionaryEntry(
                                                        key = DBusSignature.Primitive('s'),
                                                        value = DBusSignature.Variant
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(), // TODO add annotations
                                ),
                                DBusArg(
                                    name = "finished",
                                    type = DBusSignature.Primitive('b'),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                ),
                            ),
                            annotations = emptyList(),
                        ),
                        DBusMethod(
                            name = "ReleaseDevices",
                            inArgs = listOf(
                                DBusArg(
                                    name = "devices",
                                    type = DBusSignature.Array(
                                        DBusSignature.Primitive('s')
                                    ),
                                    direction = DBusArgDirection.IN,
                                    annotations = emptyList(),
                                ),
                                DBusArg(
                                    name = "options",
                                    type = DBusSignature.Array(
                                        DBusSignature.DictionaryEntry(
                                            key = DBusSignature.Primitive('s'),
                                            value = DBusSignature.Variant
                                        )
                                    ),
                                    direction = DBusArgDirection.IN,
                                    annotations = emptyList(), // TODO add annotations
                                )
                            ),
                            outArgs = emptyList(),
                            annotations = emptyList(),
                        )
                    ),
                    signals = listOf(
                        DBusSignal(
                            name = "DeviceEvents",
                            args = listOf(
                                DBusArg(
                                    name = "session_handle",
                                    type = DBusSignature.Primitive('o'),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                ),
                                DBusArg(
                                    name = "events",
                                    type = DBusSignature.Array(
                                        DBusSignature.Struct(
                                            listOf(
                                                DBusSignature.Primitive('s'),
                                                DBusSignature.Primitive('s'),
                                                DBusSignature.Array(
                                                    DBusSignature.DictionaryEntry(
                                                        key = DBusSignature.Primitive('s'),
                                                        value = DBusSignature.Variant
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                )
                            )
                        )
                    ),
                    properties = listOf(
                        DBusProperty(
                            name = "version",
                            type = DBusSignature.Primitive('u'),
                            access = DBusPropertyAccess.READ,
                            annotations = emptyList()
                        )
                    )
                ),
            ),
            childNodes = emptyList(),
        )
        val kotlinCode = processDBusIntrospectionNode(fileName, packageName, ast)
        assertEquals(
            """
                package dev.lukewarlow.dbus4k.generated

                import dev.lukewarlow.dbus4k.runtime.DBusConnection
                import kotlin.Any
                import kotlin.Boolean
                import kotlin.Pair
                import kotlin.String
                import kotlin.Triple
                import kotlin.UInt
                import kotlin.collections.List
                import kotlin.collections.Map
                import kotlinx.coroutines.flow.Flow
                import kotlinx.coroutines.flow.map

                public class UsbPortal(
                    private val bus: DBusConnection,
                    private val destination: String,
                    private val path: String,
                ) {
                    private val interfaceName: String = "org.freedesktop.portal.Usb"

                    public suspend fun createSession(options: Map<String, Any?>): String {
                        val message = bus.newMethodCall(destination, path, interfaceName, "CreateSession")
                        message.writeMapVariant(options)
                        val reply = message.awaitReply(bus)
                        return reply.readObjectPath()
                    }

                    public suspend fun enumerateDevices(options: Map<String, Any?>): List<Pair<String, Map<String, Any?>>> {
                        val message = bus.newMethodCall(destination, path, interfaceName, "EnumerateDevices")
                        message.writeMapVariant(options)
                        val reply = message.awaitReply(bus)
                        return reply.readStructPairArray("a(sa{sv})", { readString() }, { readStringVariantDictionary() })
                    }

                    public suspend fun acquireDevices(
                        parentWindow: String,
                        devices: List<Pair<String, Map<String, Any?>>>,
                        options: Map<String, Any?>,
                    ): String {
                        val message = bus.newMethodCall(destination, path, interfaceName, "AcquireDevices")
                        message.writeString(parentWindow)
                        message.writeStructPairArray("(sa{sv})", devices, { writeString(it) }, { writeMapVariant(it) })
                        message.writeMapVariant(options)
                        val reply = message.awaitReply(bus)
                        return reply.readObjectPath()
                    }

                    public suspend fun finishAcquireDevices(handle: String, options: Map<String, Any?>): FinishAcquireDevicesResult {
                        val message = bus.newMethodCall(destination, path, interfaceName, "FinishAcquireDevices")
                        message.writeObjectPath(handle)
                        message.writeMapVariant(options)
                        val reply = message.awaitReply(bus)
                        return FinishAcquireDevicesResult(reply.readStructPairArray("a(sa{sv})", { readString() }, { readStringVariantDictionary() }), reply.readBoolean())
                    }

                    public suspend fun releaseDevices(devices: List<String>, options: Map<String, Any?>) {
                        val message = bus.newMethodCall(destination, path, interfaceName, "ReleaseDevices")
                        message.writeStringArray(devices)
                        message.writeMapVariant(options)
                        message.awaitReply(bus)
                        return
                    }

                    public fun deviceEvents(): Flow<DeviceEventsEvent> = bus.signalFlow(interfaceName, "DeviceEvents", path, destination).map { signal -> DeviceEventsEvent(signal.readObjectPath(), signal.readStructTripleArray("a(ssa{sv})", { readString() }, { readString() }, { readStringVariantDictionary() })) }

                    public suspend fun getVersion(): UInt = bus.getProperty<UInt>(destination, path, interfaceName, "version")

                    public data class FinishAcquireDevicesResult(
                        public val results: List<Pair<String, Map<String, Any?>>>,
                        public val finished: Boolean,
                    )

                    public data class DeviceEventsEvent(
                        public val sessionHandle: String,
                        public val events: List<Triple<String, String, Map<String, Any?>>>,
                    )
                }
                """.trimIndent(), kotlinCode.trim())
    }

    @Test
    fun testWithObjectManager() {
        val fileName = "org.freedesktop.DBus.ObjectManager.xml"
        val packageName = "dev.lukewarlow.dbus4k.generated"

        val ast = DBusIntrospectionNode(
            name = "",
            interfaces = listOf(
                DBusInterface(
                    name = "org.freedesktop.DBus.ObjectManager",
                    methods = listOf(
                        DBusMethod(
                            name = "GetManagedObjects",
                            inArgs = emptyList(),
                            outArgs = listOf(
                                DBusArg(
                                    name = "objects",
                                    type = DBusSignature.Array(
                                        DBusSignature.DictionaryEntry(
                                            key = DBusSignature.Primitive('o'),
                                            value = DBusSignature.Array(
                                                DBusSignature.DictionaryEntry(
                                                    key = DBusSignature.Primitive('s'),
                                                    value = DBusSignature.Array(
                                                        DBusSignature.DictionaryEntry(
                                                            key = DBusSignature.Primitive('s'),
                                                            value = DBusSignature.Variant
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                ),
                            ),
                            annotations = emptyList(),
                        ),
                    ),
                    signals = listOf(
                        DBusSignal(
                            name = "InterfacesAdded",
                            args = listOf(
                                DBusArg(
                                    name = "object",
                                    type = DBusSignature.Primitive('o'),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                ),
                                DBusArg(
                                    name = "interfaces",
                                    type = DBusSignature.Array(
                                        DBusSignature.DictionaryEntry(
                                            key = DBusSignature.Primitive('s'),
                                            value = DBusSignature.Array(
                                                DBusSignature.DictionaryEntry(
                                                    key = DBusSignature.Primitive('s'),
                                                    value = DBusSignature.Variant
                                                )
                                            )
                                        )
                                    ),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                )
                            )
                        ),
                        DBusSignal(
                            name = "InterfacesRemoved",
                            args = listOf(
                                DBusArg(
                                    name = "object",
                                    type = DBusSignature.Primitive('o'),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                ),
                                DBusArg(
                                    name = "interfaces",
                                    type = DBusSignature.Array(
                                        DBusSignature.Primitive('s')
                                    ),
                                    direction = DBusArgDirection.OUT,
                                    annotations = emptyList(),
                                )
                            )
                        )
                    ),
                    properties = emptyList(),
                ),
            ),
            childNodes = emptyList(),
        )
        val kotlinCode = processDBusIntrospectionNode(fileName, packageName, ast)
        assertEquals(
            """
                package dev.lukewarlow.dbus4k.generated
                
                import dev.lukewarlow.dbus4k.runtime.DBusConnection
                import kotlin.Any
                import kotlin.String
                import kotlin.collections.List
                import kotlin.collections.Map
                import kotlinx.coroutines.flow.Flow
                import kotlinx.coroutines.flow.map
                
                public class ObjectManager(
                    private val bus: DBusConnection,
                    private val destination: String,
                    private val path: String,
                ) {
                    private val interfaceName: String = "org.freedesktop.DBus.ObjectManager"
                
                    public suspend fun getManagedObjects(): Map<String, Map<String, Map<String, Any?>>> {
                        val message = bus.newMethodCall(destination, path, interfaceName, "GetManagedObjects")
                        val reply = message.awaitReply(bus)
                        return reply.readDictionary("a{oa{sa{sv}}}", { readObjectPath() }, { readDictionary("a{sa{sv}}", { readString() }, { readDictionary("a{sv}", { readString() }, { readVariant() }) }) })
                    }
                
                    public fun interfacesAdded(): Flow<InterfacesAddedEvent> = bus.signalFlow(interfaceName, "InterfacesAdded", path, destination).map { signal -> InterfacesAddedEvent(signal.readObjectPath(), signal.readDictionary("a{sa{sv}}", { readString() }, { readDictionary("a{sv}", { readString() }, { readVariant() }) })) }
                
                    public fun interfacesRemoved(): Flow<InterfacesRemovedEvent> = bus.signalFlow(interfaceName, "InterfacesRemoved", path, destination).map { signal -> InterfacesRemovedEvent(signal.readObjectPath(), signal.readStringArray()) }
                
                    public data class InterfacesAddedEvent(
                        public val `object`: String,
                        public val interfaces: Map<String, Map<String, Any?>>,
                    )
                
                    public data class InterfacesRemovedEvent(
                        public val `object`: String,
                        public val interfaces: List<String>,
                    )
                }
                """.trimIndent(), kotlinCode.trim())
    }
}