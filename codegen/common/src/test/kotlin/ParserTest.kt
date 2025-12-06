import dev.lukewarlow.dbus4k.codegen.ast.DBusArg
import dev.lukewarlow.dbus4k.codegen.ast.DBusArgDirection
import dev.lukewarlow.dbus4k.codegen.ast.DBusInterface
import dev.lukewarlow.dbus4k.codegen.ast.DBusIntrospectionNode
import dev.lukewarlow.dbus4k.codegen.ast.DBusMethod
import dev.lukewarlow.dbus4k.codegen.ast.DBusProperty
import dev.lukewarlow.dbus4k.codegen.ast.DBusPropertyAccess
import dev.lukewarlow.dbus4k.codegen.ast.DBusSignal
import dev.lukewarlow.dbus4k.codegen.ast.DBusSignature
import dev.lukewarlow.dbus4k.codegen.parseXML
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun testWithSettingsPortal() {
        val inputFile = """
            <?xml version="1.0"?>
            <node name="/" xmlns:doc="http://www.freedesktop.org/dbus/1.0/doc.dtd">
                <interface name="org.freedesktop.portal.Secret">
                    <method name="RetrieveSecret">
                        <annotation name="org.gtk.GDBus.C.Name" value="retrieve_secret"/>
                        <annotation name="org.gtk.GDBus.C.UnixFD" value="true"/>
                        <arg type="h" name="fd" direction="in"/>
                        <annotation name="org.qtproject.QtDBus.QtTypeName.In1" value="QVariantMap"/>
                        <arg type="a{sv}" name="options" direction="in"/>
                        <arg type="o" name="handle" direction="out"/>
                    </method>
                    <property name="version" type="u" access="read"/>
                </interface>
            </node>
        """.trimIndent()

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
        assertEquals(ast, parseXML(inputFile))
    }

    @Test
    fun testWithUsbPortal() {
        val inputFile = """
            <?xml version="1.0"?>
            <node name="/" xmlns:doc="http://www.freedesktop.org/dbus/1.0/doc.dtd">
                <interface name="org.freedesktop.portal.Usb">
                    <method name="CreateSession">
                        <annotation name="org.qtproject.QtDBus.QtTypeName.In0" value="QVariantMap"/>
                        <arg type="a{sv}" name="options" direction="in"/>
                        <arg type="o" name="session_handle" direction="out"/>
                    </method>
                    <method name="EnumerateDevices">
                        <annotation name="org.qtproject.QtDBus.QtTypeName.In0" value="QVariantMap"/>
                        <arg type="a{sv}" name="options" direction="in"/>
                        <annotation name="org.qtproject.QtDBus.QtTypeName.Out0" value="QList&lt;QPair&lt;QString,QVariantMap&gt;&gt;"/>
                        <arg type="a(sa{sv})" name="devices" direction="out"/>
                    </method>
                    <method name="AcquireDevices">
                        <arg type="s" name="parent_window" direction="in"/>
                        <annotation name="org.qtproject.QtDBus.QtTypeName.In1" value="QList&lt;QPair&lt;QString,QVariantMap&gt;&gt;"/>
                        <arg type="a(sa{sv})" name="devices" direction="in"/>
                        <annotation name="org.qtproject.QtDBus.QtTypeName.In2" value="QVariantMap"/>
                        <arg type="a{sv}" name="options" direction="in"/>
                        <arg type="o" name="handle" direction="out"/>
                    </method>
                    <method name="FinishAcquireDevices">
                        <arg type="o" name="handle" direction="in"/>
                        <annotation name="org.qtproject.QtDBus.QtTypeName.In1" value="QVariantMap"/>
                        <arg type="a{sv}" name="options" direction="in"/>
                        <annotation name="org.qtproject.QtDBus.QtTypeName.Out0" value="QList&lt;QPair&lt;QString,QVariantMap&gt;&gt;"/>
                        <arg type="a(sa{sv})" name="results" direction="out"/>
                        <arg type="b" name="finished" direction="out"/>
                    </method>
                    <method name="ReleaseDevices">
                        <arg type="as" name="devices" direction="in"/>
                        <annotation name="org.qtproject.QtDBus.QtTypeName.In1" value="QVariantMap"/>
                        <arg type="a{sv}" name="options" direction="in"/>
                    </method>
                    <signal name="DeviceEvents">
                        <arg type="o" name="session_handle" direction="out"/>
                        <annotation name="org.qtproject.QtDBus.QtTypeName.Out1" value="QList&lt;std::tuple&lt;QString,QString,QVariantMap&gt;&gt;"/>
                        <arg type="a(ssa{sv})" name="events" direction="out"/>
                    </signal>

                    <property name="version" type="u" access="read"/>
                </interface>
            </node>
        """.trimIndent()

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
        assertEquals(ast, parseXML(inputFile))
    }
}