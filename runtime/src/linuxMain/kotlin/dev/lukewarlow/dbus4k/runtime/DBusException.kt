package dev.lukewarlow.dbus4k.runtime

class DBusException(val code: Int, override val message: String) : RuntimeException(message)

inline fun dbusCheck(returnedValue: Int, message: () -> String = { "sd-bus error $returnedValue" }) {
    if (returnedValue < 0) throw DBusException(returnedValue, message())
}