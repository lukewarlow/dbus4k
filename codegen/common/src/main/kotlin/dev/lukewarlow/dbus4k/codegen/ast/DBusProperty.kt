package dev.lukewarlow.dbus4k.codegen.ast

internal data class DBusProperty(
    val name: String,
    val type: DBusSignature,
    val access: DBusPropertyAccess,
    val annotations: List<DBusAnnotation> = emptyList()
)

internal enum class DBusPropertyAccess {
    READ, WRITE, READWRITE
}
