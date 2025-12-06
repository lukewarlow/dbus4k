package dev.lukewarlow.dbus4k.codegen.ast

internal sealed class DBusSignature {
    data class Primitive(val code: Char) : DBusSignature()
    data class Array(val element: DBusSignature) : DBusSignature()
    data class Struct(val fields: List<DBusSignature>) : DBusSignature()
    data class DictionaryEntry(val key: DBusSignature, val value: DBusSignature) : DBusSignature()
    object Variant : DBusSignature()

    fun toSignature(): String = when (this) {
        is Primitive -> code.toString()
        is Array -> "a" + element.toSignature()
        is Struct -> "(" + fields.joinToString("") { it.toSignature() } + ")"
        is DictionaryEntry -> "{" + key.toSignature() + value.toSignature() + "}"
        Variant -> "v"
    }

    companion object {
        fun parse(signature: String): DBusSignature {
            var index = 0

            fun peek(): Char? = signature.getOrNull(index)
            fun consume(): Char = signature[index++]

            fun parseSingle(): DBusSignature {
                return when (val ch = consume()) {
                    in "ybnqiuxtdsogh" -> Primitive(ch)

                    'v' -> Variant

                    'a' -> Array(parseSingle())

                    '(' -> {
                        val fields = mutableListOf<DBusSignature>()
                        while (peek() != ')') {
                            if (peek() == null) error("Unclosed struct: $signature")
                            fields += parseSingle()
                        }
                        consume() // ')'
                        Struct(fields)
                    }

                    '{' -> {
                        val key = parseSingle()
                        val value = parseSingle()
                        if (consume() != '}') error("Unclosed dict entry: $signature")
                        DictionaryEntry(key, value)
                    }

                    else -> error("Unknown signature char: $ch in $signature")
                }
            }

            val result = parseSingle()

            if (index != signature.length) {
                error("Signature contains multiple types or trailing characters: $signature (parsed until index $index)")
            }

            return result
        }
    }
}


