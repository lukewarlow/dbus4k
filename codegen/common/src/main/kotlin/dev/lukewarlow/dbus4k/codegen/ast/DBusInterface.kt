package dev.lukewarlow.dbus4k.codegen.ast

internal data class DBusInterface(
    val name: String,
    val methods: List<DBusMethod>,
    val signals: List<DBusSignal>,
    val properties: List<DBusProperty>
)