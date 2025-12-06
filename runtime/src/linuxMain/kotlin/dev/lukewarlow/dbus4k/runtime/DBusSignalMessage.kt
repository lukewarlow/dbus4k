package dev.lukewarlow.dbus4k.runtime

import cnames.structs.sd_bus_message
import cnames.structs.sd_bus_slot
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import kotlinx.coroutines.launch
import platformx.sdbus.sd_bus_error
import platformx.sdbus.sd_bus_match_signal
import platformx.sdbus.sd_bus_message_handler_t
import platformx.sdbus.sd_bus_slot_unref
import kotlin.collections.set

class DBusSignalMessage(
    pointer: CPointer<sd_bus_message>,
) : DBusMessage(pointer)
