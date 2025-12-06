package dev.lukewarlow.dbus4k.codegen.ast

internal data class DBusSignal(
    val name: String,
    val args: List<DBusArg>,
    val annotations: List<DBusAnnotation> = emptyList()
)
