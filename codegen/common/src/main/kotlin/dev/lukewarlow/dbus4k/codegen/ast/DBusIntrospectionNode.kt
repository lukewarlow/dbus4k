package dev.lukewarlow.dbus4k.codegen.ast

internal data class DBusIntrospectionNode(
    val name: String,
    val interfaces: List<DBusInterface>,
    val childNodes: List<DBusIntrospectionNode> = emptyList(),
)