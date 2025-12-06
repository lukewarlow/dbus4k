package dev.lukewarlow.dbus4k.runtime

import cnames.structs.sd_bus_message
import kotlinx.cinterop.CPointer

class DBusReplyMessage(
    pointer: CPointer<sd_bus_message>,
) : DBusMessage(pointer)