package dev.lukewarlow.dbus4k.runtime

enum class DBusType {
    BYTE,
    BOOLEAN,
    SHORT,
    USHORT,
    INT,
    UINT,
    LONG,
    ULONG,
    DOUBLE,
    STRING,
    OBJECT_PATH,
    SIGNATURE,
    FILE_DESCRIPTOR,
    ARRAY,
    STRUCT,
    VARIANT,
    DICTIONARY_ENTRY,
    NONE
}

internal fun DBusType.toSignatureString(): String = when (this) {
    DBusType.BYTE             -> "y"
    DBusType.BOOLEAN          -> "b"
    DBusType.SHORT            -> "n"
    DBusType.USHORT           -> "q"
    DBusType.INT              -> "i"
    DBusType.UINT             -> "u"
    DBusType.LONG             -> "x"
    DBusType.ULONG            -> "t"
    DBusType.DOUBLE           -> "d"
    DBusType.STRING           -> "s"
    DBusType.OBJECT_PATH      -> "o"
    DBusType.SIGNATURE        -> "g"
    DBusType.FILE_DESCRIPTOR  -> "h"
    DBusType.ARRAY            -> "a"
    DBusType.STRUCT           -> "("
    DBusType.DICTIONARY_ENTRY -> "{"
    DBusType.VARIANT          -> "v"
    DBusType.NONE             -> ""
}
