package dev.lukewarlow.dbus4k.runtime

import cnames.structs.*
import kotlinx.cinterop.*
import platformx.sdbus.*

// TODO evaluate all signatures to ensure they're correct

private const val TYPE_ARRAY: Byte = 'a'.code.toByte()
private const val TYPE_VARIANT: Byte = 'v'.code.toByte()
private const val TYPE_STRUCT: Byte = '('.code.toByte()
private const val TYPE_DICT_ENTRY: Byte = '{'.code.toByte()
internal const val TIMEOUT_USE_DEFAULT: ULong = ULong.MAX_VALUE


sealed class DBusMessage(
    internal val pointer: CPointer<sd_bus_message>
) : AutoCloseable {
    private var closed = false

    override fun close() {
        if (!closed) {
            sd_bus_message_unref(pointer)
            closed = true
        }
    }

    fun enterArray(contents: String) {
        dbusCheck(sd_bus_message_enter_container(pointer, TYPE_ARRAY, contents))
    }

    fun enterStruct() {
        dbusCheck(sd_bus_message_enter_container(pointer, TYPE_STRUCT, null))
    }

    fun enterVariant(contents: String) {
        dbusCheck(sd_bus_message_enter_container(pointer, TYPE_VARIANT, contents))
    }

    fun enterDictionaryEntry(contents: String) {
        dbusCheck(sd_bus_message_enter_container(pointer, TYPE_DICT_ENTRY, contents))
    }

    fun exitContainer() {
        dbusCheck(sd_bus_message_exit_container(pointer))
    }

    fun sender(): String = sd_bus_message_get_sender(pointer)?.toKString() ?: ""

    fun path(): String = sd_bus_message_get_path(pointer)?.toKString() ?: ""

    fun peekType(): DBusType = memScoped {
        val typeOut = alloc<ByteVar>()
        // Unused but needed
        val contentsOut = alloc<CPointerVar<ByteVar>>()

        val status = sd_bus_message_peek_type(pointer, typeOut.ptr, contentsOut.ptr)
        dbusCheck(status)
        if (status == 0) return DBusType.NONE

        return when (val c = typeOut.value.toInt().toChar()) {
            'y' -> DBusType.BYTE
            'b' -> DBusType.BOOLEAN
            'n' -> DBusType.SHORT
            'q' -> DBusType.USHORT
            'i' -> DBusType.INT
            'u' -> DBusType.UINT
            'x' -> DBusType.LONG
            't' -> DBusType.ULONG
            'd' -> DBusType.DOUBLE
            's' -> DBusType.STRING
            'o' -> DBusType.OBJECT_PATH
            'g' -> DBusType.SIGNATURE
            'h' -> DBusType.FILE_DESCRIPTOR
            'a' -> DBusType.ARRAY
            '(' -> DBusType.STRUCT
            '{' -> DBusType.DICTIONARY_ENTRY
            'v' -> DBusType.VARIANT

            else -> error("Unknown D-Bus type code '$c'")
        }
    }


    fun readByte(): Byte = memScoped {
        val out = alloc<ByteVar>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.BYTE.toSignatureString(), out.ptr))
        return out.value
    }

    fun readBoolean(): Boolean = memScoped {
        val out = alloc<IntVar>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.BOOLEAN.toSignatureString(), out.ptr))
        return out.value != 0
    }

    fun readShort(): Short = memScoped {
        val out = alloc<ShortVar>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.SHORT.toSignatureString(), out.ptr))
        return out.value
    }

    fun readUShort(): UShort = memScoped {
        val out = alloc<UShortVar>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.USHORT.toSignatureString(), out.ptr))
        return out.value
    }

    fun readInt(): Int = memScoped {
        val out = alloc<IntVar>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.INT.toSignatureString(), out.ptr))
        return out.value
    }

    fun readUInt(): UInt = memScoped {
        val out = alloc<UIntVar>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.UINT.toSignatureString(), out.ptr))
        return out.value
    }

    fun readLong(): Long = memScoped {
        val out = alloc<LongVar>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.LONG.toSignatureString(), out.ptr))
        return out.value
    }

    fun readULong(): ULong = memScoped {
        val out = alloc<ULongVar>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.ULONG.toSignatureString(), out.ptr))
        return out.value
    }

    fun readDouble(): Double = memScoped {
        val out = alloc<DoubleVar>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.DOUBLE.toSignatureString(), out.ptr))
        return out.value
    }

    fun readString(): String = memScoped {
        val out = alloc<CPointerVar<ByteVar>>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.STRING.toSignatureString(), out.ptr))
        return out.value!!.toKString()
    }

    fun readObjectPath(): String = memScoped {
        val out = alloc<CPointerVar<ByteVar>>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.OBJECT_PATH.toSignatureString(), out.ptr))
        return out.value!!.toKString()
    }

    fun readSignature(): String = memScoped {
        val out = alloc<CPointerVar<ByteVar>>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.SIGNATURE.toSignatureString(), out.ptr))
        return out.value!!.toKString()
    }

    fun readFileDescriptor(): Int = memScoped {
        val out = alloc<IntVar>()
        dbusCheck(sd_bus_message_read(pointer, DBusType.FILE_DESCRIPTOR.toSignatureString(), out.ptr))
        return out.value
    }

    private inline fun <T> readPrimitiveArray(
        type: DBusType,
        reader: () -> T
    ): List<T> {
        enterArray(type.toSignatureString())

        val result = mutableListOf<T>()

        while (peekType() != DBusType.NONE) {
            result += reader()
        }

        exitContainer()
        return result
    }

    fun readByteArray(): ByteArray = readPrimitiveArray(DBusType.BYTE) { readByte() }.toByteArray()

    fun readBooleanArray(): List<Boolean> = readPrimitiveArray(DBusType.BOOLEAN) { readBoolean() }

    fun readShortArray(): List<Short> = readPrimitiveArray(DBusType.SHORT) { readShort() }

    fun readUShortArray(): List<UShort> = readPrimitiveArray(DBusType.USHORT) { readUShort() }

    fun readIntArray(): List<Int> = readPrimitiveArray(DBusType.INT) { readInt() }

    fun readUIntArray(): List<UInt> = readPrimitiveArray(DBusType.UINT) { readUInt() }

    fun readLongArray(): List<Long> = readPrimitiveArray(DBusType.LONG) { readLong() }

    fun readULongArray(): List<ULong> = readPrimitiveArray(DBusType.ULONG) { readULong() }

    fun readDoubleArray(): List<Double> = readPrimitiveArray(DBusType.DOUBLE) { readDouble() }

    fun readStringArray(): List<String> = readPrimitiveArray(DBusType.STRING) { readString() }

    fun readDynamicArray(elementSignature: String): List<Any?> {
        enterArray(elementSignature)

        val result = mutableListOf<Any?>()
        while (peekType() != DBusType.NONE) {
            result += readDynamicValue()
        }

        exitContainer()
        return result
    }

    inline fun <T> readStruct(block: () -> T): T {
        enterStruct()
        val value = block()
        exitContainer()
        return value
    }

    fun <A, B> readStructPair(
        readA: DBusMessage.() -> A,
        readB: DBusMessage.() -> B
    ): Pair<A, B> = readStruct {
        val a = readA()
        val b = readB()
        Pair(a, b)
    }

    fun <A, B, C> readStructTriple(
        readA: DBusMessage.() -> A,
        readB: DBusMessage.() -> B,
        readC: DBusMessage.() -> C
    ): Triple<A, B, C> = readStruct {
        val a = readA()
        val b = readB()
        val c = readC()
        Triple(a, b, c)
    }

    fun <A, B> readStructPairArray(
        signature: String,
        readA: DBusMessage.() -> A,
        readB: DBusMessage.() -> B,
    ): List<Pair<A, B>> {
        val result = mutableListOf<Pair<A, B>>()
        enterArray(signature)
        while (peekType() != DBusType.NONE) {
            val pair = readStructPair(readA, readB)
            result += pair
        }
        exitContainer()
        return result
    }

    fun <A, B, C> readStructTripleArray(
        signature: String,
        readA: DBusMessage.() -> A,
        readB: DBusMessage.() -> B,
        readC: DBusMessage.() -> C
    ): List<Triple<A, B, C>> {
        val result = mutableListOf<Triple<A, B, C>>()
        enterArray(signature)
        while (peekType() != DBusType.NONE) {
            val triple = readStructTriple(readA, readB, readC)
            result += triple
        }
        exitContainer()
        return result
    }

    fun readDynamicStruct(): List<Any?> = readStruct {
        buildList {
            while (peekType() != DBusType.NONE) {
                add(readDynamicValue())
            }
        }
    }

    fun readVariant(): Any? = readStruct {
        val signature = readSignature()
        enterVariant(signature)

        val value = readDynamicValue()

        exitContainer()
        return value
    }

    fun readDynamicDictEntry(signature: String): Pair<Any?, Any?> {
        enterDictionaryEntry(signature)
        val key = readDynamicValue()
        val value = readDynamicValue()
        exitContainer()
        return key to value
    }

    fun readDynamicDictionary(signature: String): Map<Any?, Any?> {
        val result = LinkedHashMap<Any?, Any?>()

        enterArray(signature)

        while (peekType() != DBusType.NONE) {
            val (key, value) = readDynamicDictEntry(signature)
            result[key] = value
        }

        exitContainer()
        return result
    }

    inline fun <K, V> readDictionary(
        signature: String,
        keyReader: () -> K,
        valueReader: () -> V
    ): Map<K, V> {
        require(signature.length >= 2) { "Dictionary-entry signature must be key-value pair" }
        val result = LinkedHashMap<K, V>()
        enterArray(signature)

        while (peekType() != DBusType.NONE) {
            enterDictionaryEntry(signature)

            val key = keyReader()
            val value = valueReader()

            exitContainer()
            result[key] = value
        }

        exitContainer()
        return result
    }

    fun readStringVariantDictionary(): Map<String, Any?> =
        readDictionary("sv", ::readString, ::readVariant)

    fun readStringStringDictionary(): Map<String, String> =
        readDictionary("ss", ::readString, ::readString)

    fun readDynamicValue(): Any? =
        when (peekType()) {
            DBusType.BYTE -> readByte()
            DBusType.BOOLEAN -> readBoolean()
            DBusType.SHORT -> readShort()
            DBusType.USHORT -> readUShort()
            DBusType.INT -> readInt()
            DBusType.UINT -> readUInt()
            DBusType.LONG -> readLong()
            DBusType.ULONG -> readULong()
            DBusType.DOUBLE -> readDouble()
            DBusType.STRING -> readString()
            DBusType.OBJECT_PATH -> readObjectPath()
            DBusType.SIGNATURE -> readSignature()
            DBusType.FILE_DESCRIPTOR -> readFileDescriptor()
            DBusType.VARIANT -> readVariant()
            DBusType.ARRAY -> readDynamicArray(peekSignature())
            DBusType.STRUCT -> readDynamicStruct()
            DBusType.DICTIONARY_ENTRY -> readDynamicDictEntry(peekSignature())
            DBusType.NONE -> null
        }

    fun peekSignature(): String = memScoped {
        val typeOut = alloc<ByteVar>()
        val contentsOut = alloc<CPointerVar<ByteVar>>()
        val r = sd_bus_message_peek_type(pointer, typeOut.ptr, contentsOut.ptr)
        dbusCheck(r)
        contentsOut.value?.toKString() ?: ""
    }


    internal fun writePrimitive(value: Any, type: DBusType) {
        dbusCheck(sd_bus_message_append(pointer, type.toSignatureString(), value))
    }

    fun writeByte(value: Byte) = writePrimitive(value, DBusType.BYTE)

    fun writeBoolean(value: Boolean) = writePrimitive(if (value) 1 else 0, DBusType.BOOLEAN)

    fun writeShort(value: Short) = writePrimitive(value, DBusType.SHORT)

    fun writeUShort(value: UShort) = writePrimitive(value, DBusType.USHORT)

    fun writeInt(value: Int) = writePrimitive(value, DBusType.INT)

    fun writeUInt(value: UInt) = writePrimitive(value, DBusType.UINT)

    fun writeLong(value: Long) = writePrimitive(value, DBusType.LONG)

    fun writeULong(value: ULong) = writePrimitive(value, DBusType.ULONG)

    fun writeDouble(value: Double) = writePrimitive(value, DBusType.DOUBLE)

    fun writeString(value: String) = writePrimitive(value, DBusType.STRING)

    fun writeObjectPath(value: String) = writePrimitive(value, DBusType.OBJECT_PATH)

    fun writeSignature(value: String) = writePrimitive(value, DBusType.SIGNATURE)

    fun writeFileDescriptor(value: Int) = writePrimitive(value, DBusType.FILE_DESCRIPTOR)

    inline fun writeArray(elementSignature: String, writer: () -> Unit) {
        enterArray(elementSignature)
        writer()
        exitContainer()
    }

    private inline fun writePrimitiveArray(type: DBusType, writer: () -> Unit) = writeArray(type.toSignatureString(), writer)

    fun writeByteArray(values: ByteArray) = writePrimitiveArray(DBusType.BYTE) {
        values.forEach(::writeByte)
    }

    fun writeBooleanArray(values: List<Boolean>) = writePrimitiveArray(DBusType.BOOLEAN) {
        values.forEach(::writeBoolean)
    }

    fun writeShortArray(values: List<Short>) = writePrimitiveArray(DBusType.SHORT) {
        values.forEach(::writeShort)
    }

    fun writeUShortArray(values: List<UShort>) = writePrimitiveArray(DBusType.USHORT) {
        values.forEach(::writeUShort)
    }

    fun writeIntArray(values: List<Int>) = writePrimitiveArray(DBusType.INT) {
        values.forEach(::writeInt)
    }

    fun writeUIntArray(values: List<UInt>) = writePrimitiveArray(DBusType.UINT) {
        values.forEach(::writeUInt)
    }

    fun writeLongArray(values: List<Long>) = writePrimitiveArray(DBusType.LONG) {
        values.forEach(::writeLong)
    }

    fun writeULongArray(values: List<ULong>) = writePrimitiveArray(DBusType.ULONG) {
        values.forEach(::writeULong)
    }

    fun writeDoubleArray(values: List<Double>) = writePrimitiveArray(DBusType.DOUBLE) {
        values.forEach(::writeDouble)
    }

    fun writeStringArray(values: List<String>) = writePrimitiveArray(DBusType.STRING) {
        values.forEach(::writeString)
    }

    fun writeObjectPathArray(values: List<String>) = writePrimitiveArray(DBusType.OBJECT_PATH) {
        values.forEach(::writeObjectPath)
    }

    fun writeSignatureArray(values: List<String>) = writePrimitiveArray(DBusType.SIGNATURE) {
        values.forEach(::writeSignature)
    }

    inline fun writeStruct(writer: () -> Unit) {
        enterStruct()
        writer()
        exitContainer()
    }

    inline fun <A, B> writeStructPair(pair: Pair<A, B>, writeA: DBusMessage.(A) -> Unit, writeB: DBusMessage.(B) -> Unit) {
        writeStruct {
            writeA(pair.first)
            writeB(pair.second)
        }
    }

    inline fun <A, B, C> writeStructTriple(triple: Triple<A, B, C>, writeA: DBusMessage.(A) -> Unit, writeB: DBusMessage.(B) -> Unit, writeC: DBusMessage.(C) -> Unit) {
        writeStruct {
            writeA(triple.first)
            writeB(triple.second)
            writeC(triple.third)
        }
    }

    inline fun <A, B> writeStructPairArray(signature: String, value: List<Pair<A, B>>, writeA: DBusMessage.(A) -> Unit, writeB: DBusMessage.(B) -> Unit) {
        writeArray(signature) {
            for (pair in value) {
                writeStructPair(pair, writeA, writeB)
            }
        }
    }

    inline fun <A, B, C> writeStructTripleArray(signature: String, value: List<Triple<A, B, C>>, writeA: DBusMessage.(A) -> Unit, writeB: DBusMessage.(B) -> Unit, writeC: DBusMessage.(C) -> Unit) {
        writeArray(signature) {
            for (triple in value) {
                writeStructTriple(triple, writeA, writeB, writeC)
            }
        }
    }

    fun writeVariant(value: Any?) {
        when (value) {
            null -> {
                writeVariant("v") {
                    writeVariant("") // empty signature → unit-like
                }
            }
            is Byte -> writeVariant(DBusType.BYTE.toString()) { writeByte(value) }
            is Boolean -> writeVariant(DBusType.BOOLEAN.toString()) { writeBoolean(value) }
            is Short -> writeVariant(DBusType.SHORT.toString()) { writeShort(value) }
            is UShort -> writeVariant(DBusType.USHORT.toString()) { writeUShort(value) }
            is Int -> writeVariant(DBusType.INT.toString()) { writeInt(value) }
            is UInt -> writeVariant(DBusType.UINT.toString()) { writeUInt(value) }
            is Long -> writeVariant(DBusType.LONG.toString()) { writeLong(value) }
            is ULong -> writeVariant(DBusType.ULONG.toString()) { writeULong(value) }
            is Double -> writeVariant(DBusType.DOUBLE.toString()) { writeDouble(value) }
            is String -> writeVariant(DBusType.STRING.toString()) { writeString(value) }
            is ByteArray -> writeVariant("ay") { writeByteArray(value) }
            is List<*> -> writeListVariant(value)
            is Map<*, *> -> writeMapVariant(value)
            else -> error("Cannot write variant for unsupport type: $value (${value::class})")
        }
    }

    inline fun writeVariant(signature: String, writer: () -> Unit) {
        enterVariant(signature)
        writer()
        exitContainer()
    }

    fun writeListVariant(values: List<*>) {
        if (values.isEmpty()) {
            writeVariant("av") {
                writeArray("v") {}
            }
            return
        }
        if (values.any { it == null }) error("D-Bus arrays cannot contain null values")
        val first = values.first()
        val elementType = when (first) {
            is Byte -> DBusType.BYTE
            is Boolean -> DBusType.BOOLEAN
            is Short -> DBusType.SHORT
            is UShort -> DBusType.USHORT
            is Int -> DBusType.INT
            is UInt -> DBusType.UINT
            is Long -> DBusType.LONG
            is ULong -> DBusType.ULONG
            is Double -> DBusType.DOUBLE
            is String -> DBusType.STRING
            else -> DBusType.VARIANT
        }

        writeVariant("a${elementType.toSignatureString()}") {
            writeArray(elementType.toSignatureString()) {
                values.forEach { value ->
                    when (value) {
                        is Byte -> writeByte(value)
                        is Boolean -> writeBoolean(value)
                        is Short -> writeShort(value)
                        is UShort -> writeUShort(value)
                        is Int -> writeInt(value)
                        is UInt -> writeUInt(value)
                        is Long -> writeLong(value)
                        is ULong -> writeULong(value)
                        is Double -> writeDouble(value)
                        is String -> writeString(value)
                        else -> writeVariant(value!!)
                    }
                }
            }
        }
    }

    fun writeMapVariant(values: Map<*, *>) {
        writeDictionary(
            "s",
            "v",
            values,
            keyWriter = {
                writeString(it as? String ?: error("D-Bus dictionary keys must be strings, got: $it"))
            },
            valueWriter = {
                writeVariant(it)
            },
        )
    }

    inline fun writeDictionaryEntry(keySignature: String, valueSignature: String, writer: () -> Unit) {
        enterDictionaryEntry("{$keySignature$valueSignature}")
        writer()
        exitContainer()
    }

    inline fun <K, V> writeDictionary(
        keySignature: String,
        valueSignature: String,
        map: Map<K, V>,
        keyWriter: (K) -> Unit,
        valueWriter: (V) -> Unit
    ) = writeArray("{$keySignature$valueSignature}") {
        map.forEach { (k, v) ->
            writeDictionaryEntry(keySignature, valueSignature) {
                keyWriter(k)
                valueWriter(v)
            }
        }
    }
}
