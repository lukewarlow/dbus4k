package dev.lukewarlow.dbus4k.codegen.ast

internal data class DBusMethod(
    val name: String,
    val inArgs: List<DBusArg>,
    val outArgs: List<DBusArg>,
    val annotations: List<DBusAnnotation>
)
