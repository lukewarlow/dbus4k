package dev.lukewarlow.dbus4k.runtime

data class SignalMatchRule(
    val sender: String?,
    val path: String?,
    val interfaceName: String,
    val member: String
)