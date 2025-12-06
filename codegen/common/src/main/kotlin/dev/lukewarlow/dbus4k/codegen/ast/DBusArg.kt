package dev.lukewarlow.dbus4k.codegen.ast

internal data class DBusArg(
    val name: String,
    val type: DBusSignature,
    val direction: DBusArgDirection,
    val annotations: List<DBusAnnotation>,
)

internal enum class DBusArgDirection { IN, OUT }
