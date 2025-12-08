package dev.lukewarlow.dbus4k.codegen

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import dev.lukewarlow.dbus4k.codegen.ast.DBusInterface
import dev.lukewarlow.dbus4k.codegen.ast.DBusIntrospectionNode
import dev.lukewarlow.dbus4k.codegen.ast.DBusMethod
import dev.lukewarlow.dbus4k.codegen.ast.DBusProperty
import dev.lukewarlow.dbus4k.codegen.ast.DBusPropertyAccess
import dev.lukewarlow.dbus4k.codegen.ast.DBusSignal
import dev.lukewarlow.dbus4k.codegen.ast.DBusSignature
import kotlinx.coroutines.flow.Flow

// TODO evaluate all D-Bus signatures passed to runtime code to ensure they're correct

fun processXML(xmlFile: String, fileName: String, packageName: String): String {
    val ast = parseXML(xmlFile)
    val kotlin = processDBusIntrospectionNode(fileName, packageName, ast)
    return kotlin
}

internal fun processDBusIntrospectionNode(fileName: String, packageName: String, ast: DBusIntrospectionNode): String {
    val fileSpec = FileSpec.builder(packageName, "${fileName}.kt")
        .addImport("dev.lukewarlow.dbus4k.runtime", "DBusConnection")
        .addImport("kotlinx.coroutines.flow", "Flow", "map")
        .indent("    ")
    ast.interfaces.forEach { interfaceObj ->
        fileSpec.processInterface(interfaceObj)
    }
    return fileSpec.build().toString()
}

private fun FileSpec.Builder.processInterface(interfaceObj: DBusInterface) {
    val dbusConnectionType = ClassName.bestGuess("dev.lukewarlow.dbus4k.runtime.DBusConnection")
    val constructor = FunSpec.constructorBuilder()
        .addParameter("bus", dbusConnectionType)
        .addParameter("destination", STRING)
        .addParameter("path", STRING)
        .build()
    val className = getClassName(interfaceObj.name)
    val typeSpec = TypeSpec.classBuilder(className)
        .primaryConstructor(constructor)
        .addProperty(
            PropertySpec.builder("bus", dbusConnectionType)
                .initializer("bus")
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
        .addProperty(
            PropertySpec.builder("destination", STRING)
                .initializer("destination")
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
        .addProperty(
            PropertySpec.builder("path", STRING)
                .initializer("path")
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
        .addProperty(
            PropertySpec.builder("interfaceName", STRING)
                .initializer("\"${interfaceObj.name}\"")
                .addModifiers(KModifier.PRIVATE)
                .build()
        )

    interfaceObj.methods.forEach { method ->
        typeSpec.addMethodCall(packageName, className, method)
    }
    interfaceObj.signals.forEach { signal ->
        typeSpec.addSignalFlow(packageName, className, signal)
    }
    interfaceObj.properties.forEach { property ->
        typeSpec.addPropertyGetter(property)
        typeSpec.addPropertySetter(property)
    }

    this.addType(typeSpec.build())
}

private fun TypeSpec.Builder.addMethodCall(packageName: String, className: String, method: DBusMethod) {
    val kotlinName = method.name.replaceFirstChar { it.lowercase() }

    val funSpec = FunSpec.builder(kotlinName)
        .addModifiers(KModifier.SUSPEND)

    for (arg in method.inArgs) {
        funSpec.addParameter(convertName(arg.name), mapSignatureToKotlinType(arg.type))
    }

    funSpec.addStatement(
        "val message = bus.newMethodCall(destination, path, interfaceName, %S)",
        method.name
    )

    for (arg in method.inArgs) {
        funSpec.addStatement("message.%L", generateWriter(convertName(arg.name), arg.type))
    }

    val outArgsSize = method.outArgs.size
    when (outArgsSize) {
        0 -> {
            funSpec.addStatement("message.awaitReply(bus)")
            funSpec.addStatement("return")
        }
        1 -> {
            funSpec.addStatement("val reply = message.awaitReply(bus)")
            val out = method.outArgs.single()
            funSpec.returns(mapSignatureToKotlinType(method.outArgs[0].type))
            funSpec.addStatement("return reply.%L", generateReader(out.type))
        }
        else -> {
            funSpec.addStatement("val reply = message.awaitReply(bus)")
            val constructor = FunSpec.constructorBuilder()
            val resultClass = TypeSpec.classBuilder("${method.name}Result")
                .addModifiers(KModifier.DATA)
            val readers = mutableListOf<String>()
            method.outArgs.forEach { arg ->
                val type = mapSignatureToKotlinType(arg.type)
                constructor.addParameter(convertName(arg.name), type)
                resultClass.addProperty(PropertySpec.builder(convertName(arg.name), type).initializer(convertName(arg.name)).build())
                readers.add("reply.${generateReader(arg.type)}")
            }
            resultClass.primaryConstructor(constructor.build())
            val returnType = resultClass.build()
            funSpec.returns(ClassName(packageName, className, returnType.name!!))
            // generate a data class for the result
            this.addType(returnType)

            funSpec.addStatement("return %L(%L)", returnType.name!!, readers.joinToString(", "))
        }
    }

    this.addFunction(funSpec.build())
}

private fun TypeSpec.Builder.addSignalFlow(packageName: String, className: String, signal: DBusSignal) {
    val kotlinName = signal.name.replaceFirstChar { it.lowercase() }
	val funSpec = FunSpec.builder(kotlinName)
	if (signal.args.isNotEmpty()) {
		val constructor = FunSpec.constructorBuilder()
		val resultClass = TypeSpec.classBuilder(signal.name.replaceFirstChar { it.uppercase() } + "Event")
			.addModifiers(KModifier.DATA)
		val readers = mutableListOf<String>()
		signal.args.forEach { arg ->
			val type = mapSignatureToKotlinType(arg.type)
			constructor.addParameter(convertName(arg.name), type)
			resultClass.addProperty(PropertySpec.builder(convertName(arg.name), type).initializer(convertName(arg.name)).build())
			readers.add("signal.${generateReader(arg.type)}")
		}
		resultClass.primaryConstructor(constructor.build())
		val returnType = resultClass.build()
		this.addType(returnType)
		val resultTypeName = ClassName(packageName, className, returnType.name!!)
		funSpec.returns(Flow::class.asClassName().parameterizedBy(resultTypeName))
		funSpec.addStatement("return bus.signalFlow(interfaceName, %S, path, destination).map { signal -> %T(%L) }", signal.name, resultTypeName, readers.joinToString(", "))
	} else {
		funSpec.returns(Flow::class.asClassName().parameterizedBy(UNIT))
		funSpec.addStatement("return bus.signalFlow(interfaceName, %S, path, destination).map { Unit }", signal.name)
	}

    this.addFunction(funSpec.build())
}

private fun TypeSpec.Builder.addPropertyGetter(property: DBusProperty) {
    if (property.access == DBusPropertyAccess.WRITE) return

    val kotlinName = property.name.replaceFirstChar { it.uppercaseChar() }
    val returnType = mapSignatureToKotlinType(property.type)

    val funSpec = FunSpec.builder("get${kotlinName}")
        .addModifiers(KModifier.SUSPEND)
        .returns(returnType)
        .addStatement("return bus.getProperty<%T>(destination, path, interfaceName, %S)", returnType, property.name)

    this.addFunction(funSpec.build())
}

private fun TypeSpec.Builder.addPropertySetter(property: DBusProperty) {
    if (property.access == DBusPropertyAccess.READ) return

    val kotlinName = property.name.replaceFirstChar { it.uppercaseChar() }
    val propertyType = mapSignatureToKotlinType(property.type)

    val funSpec = FunSpec.builder("set${kotlinName}")
        .addModifiers(KModifier.SUSPEND)
        .addParameter("newValue", propertyType)
        .addStatement("bus.setProperty(destination, path, interfaceName, %S, newValue)", property.name)

    this.addFunction(funSpec.build())
}

private fun mapSignatureToKotlinType(signature: DBusSignature): TypeName = when(signature) {
    is DBusSignature.Primitive -> when(signature.code) {
        'y' -> BYTE
        'b' -> BOOLEAN
        'n' -> SHORT
        'q' -> U_SHORT
        'i' -> INT
        'u' -> U_INT
        'x' -> LONG
        't' -> U_LONG
        'd' -> DOUBLE
        's' -> STRING
        'o' -> STRING // Object Path
        'h' -> INT // File Descriptor
        else -> ANY.copy(nullable = true)
    }

    is DBusSignature.Variant -> ANY.copy(nullable = true)

    is DBusSignature.Array ->
        when (signature.element) {
            is DBusSignature.Primitive -> when (signature.element.code) {
				'y' -> BYTE_ARRAY
	            else -> LIST.parameterizedBy(mapSignatureToKotlinType(signature.element))
			}
            is DBusSignature.DictionaryEntry -> MAP.parameterizedBy(STRING, ANY.copy(nullable = true))
            is DBusSignature.Struct -> LIST.parameterizedBy(mapSignatureToKotlinType(signature.element))
	        is DBusSignature.Array -> {
		        when (signature.element.element) {
			       is DBusSignature.DictionaryEntry -> LIST.parameterizedBy(MAP.parameterizedBy(STRING, ANY.copy(nullable = true)))
		           else -> LIST.parameterizedBy(ANY.copy(nullable = true))
		        }
			}
            else -> LIST.parameterizedBy(ANY.copy(nullable = true))
        }

    is DBusSignature.DictionaryEntry ->
        MAP.parameterizedBy(STRING, ANY.copy(nullable = true))

    is DBusSignature.Struct -> when(signature.fields.size) {
        2 -> Pair::class.asClassName().parameterizedBy(
            mapSignatureToKotlinType(signature.fields[0]),
            mapSignatureToKotlinType(signature.fields[1])
        )
        3 -> Triple::class.asClassName().parameterizedBy(
            mapSignatureToKotlinType(signature.fields[0]),
            mapSignatureToKotlinType(signature.fields[1]),
            mapSignatureToKotlinType(signature.fields[2])
        )
        else -> {
	        val kotlinTypes = signature.fields.map { mapSignatureToKotlinType(it) }
	        val first = kotlinTypes.first()
	        val allSame = kotlinTypes.all { it == first }
	        if (allSame) {
				LIST.parameterizedBy(first)
	        } else {
		        LIST.parameterizedBy(ANY)
	        }
		}
    }
}

private fun generateWriter(name: String, type: DBusSignature): String {
    return when(type) {
        is DBusSignature.Primitive -> {
            val fn = primitiveWriterName(type.code)
            "$fn(${name})"
        }

        is DBusSignature.Array -> when (val element = type.element) {
            is DBusSignature.DictionaryEntry ->
                "writeMapVariant(${name})"
            is DBusSignature.Primitive -> {
                val fn = arrayWriterName(type.element)
                "$fn(${name})"
            }
            is DBusSignature.Struct -> when (element.fields.size) {
                2 -> {
                    val a = generateWriter("it", element.fields[0])
                    val b = generateWriter("it", element.fields[1])
                    "writeStructPairArray(\"${element.toSignature()}\", ${name}, { $a }, { $b })"
                }
                3 -> {
                    val a = generateWriter("it", element.fields[0])
                    val b = generateWriter("it", element.fields[1])
                    val c = generateWriter("it", element.fields[2])
                    "writeStructTripleArray(\"${element.toSignature()}\", ${name}, { $a }, { $b }, { $c })"
                }
                else -> {
					val fieldWriters = element.fields.mapIndexed { index, field ->
						generateWriter("it[$index] as ${mapSignatureToKotlinType(field)}", field)
					}.joinToString("\n")
					"writeStructArray(\"${element.toSignature()}\", $name) {\n$fieldWriters\n}"
				}
            }
	        is DBusSignature.Array -> when (val innerElement = element.element) {
		        is DBusSignature.DictionaryEntry -> {
					"writeArray(\"${element.toSignature()}\", $name) {\nwriteMapVariant(it)\n}"
				}
		        else -> error("Unsupported nested array element type: $element")
	        }
            else -> error("Unsupported array element type: $element")
        }

        is DBusSignature.DictionaryEntry -> error("Dictionary entries unsupported at top level")

        is DBusSignature.Variant -> "writeVariant(${name})"

        is DBusSignature.Struct -> when (type.fields.size) {
            2 -> {
                val a = generateWriter("it", type.fields[0])
                val b = generateWriter("it", type.fields[1])
                "writeStructPair(${name}, { $a }, { $b })"
            }
            3 -> {
                val a = generateWriter("it", type.fields[0])
                val b = generateWriter("it", type.fields[1])
                val c = generateWriter("it", type.fields[2])
                "writeStructTriple(${name}, { $a }, { $b }, { $c })"
            }
            else -> {
	            val fieldWriters = type.fields.mapIndexed { index, field ->
		            generateWriter("$name[$index] as ${mapSignatureToKotlinType(field)}", field)
	            }.joinToString("\n")
	            return "writeStruct {\n$fieldWriters\n}"
			}
        }
    }
}

private fun primitiveWriterName(c: Char) = when(c) {
    'y' -> "writeByte"
    'b' -> "writeBoolean"
    'n' -> "writeShort"
    'q' -> "writeUShort"
    'i' -> "writeInt"
    'u' -> "writeUInt"
    'x' -> "writeLong"
    't' -> "writeULong"
    'd' -> "writeDouble"
    's' -> "writeString"
    'o' -> "writeObjectPath"
    'h' -> "writeFileDescriptor"
    else -> error("Unsupported primitive $c")
}

private fun generateReader(type: DBusSignature): String {
    val typeSignature = type.toSignature()
    return when(type) {
        is DBusSignature.Primitive -> when(type.code) {
            'y' -> "readByte()"
            'b' -> "readBoolean()"
            'n' -> "readShort()"
            'q' -> "readUShort()"
            'i' -> "readInt()"
            'u' -> "readUInt()"
            'x' -> "readLong()"
            't' -> "readULong()"
            'd' -> "readDouble()"
            's' -> "readString()"
            'o' -> "readObjectPath()"
            'h' -> "readFileDescriptor()"
            else -> error("Unsupported primitive $type")
        }
        is DBusSignature.Array -> when (val element = type.element) {
            is DBusSignature.Primitive -> when (element.code) {
                'y' -> "readByteArray()"
                'b' -> "readBooleanArray()"
                'n' -> "readShortArray()"
                'q' -> "readUShortArray()"
                'i' -> "readIntArray()"
                'u' -> "readUIntArray()"
                'x' -> "readLongArray()"
                't' -> "readULongArray()"
                'd' -> "readDoubleArray()"
                's' -> "readStringArray()"
                'o' -> "readObjectPathArray()"
                else -> error("Unsupported primitive array $element")
            }
            is DBusSignature.DictionaryEntry -> "readStringVariantDictionary()"
            is DBusSignature.Struct -> when (element.fields.size) {
                2 -> {
                    val field1 = element.fields[0]
                    val field2 = element.fields[1]
                    "readStructPairArray(\"${typeSignature}\", { ${generateReader(field1)} }, { ${generateReader(field2)} })"
                }
                3 -> {
                    val field1 = element.fields[0]
                    val field2 = element.fields[1]
                    val field3 = element.fields[2]
                    "readStructTripleArray(\"$typeSignature\", { ${generateReader(field1)} }, { ${generateReader(field2)} }, { ${generateReader(field3)} })"
                }
                else -> {
	                val kotlinTypes = element.fields.map { mapSignatureToKotlinType(it) }
	                val first = kotlinTypes.first()
	                val allSame = kotlinTypes.all { it == first }
	                if (allSame) {
		                val fieldReaders = element.fields.map { field -> generateReader(field) }.joinToString(", ")
						"readStructArray(\"$typeSignature\") { listOf($fieldReaders) }"
	                } else {
		                "readDynamicArray(\"$typeSignature\") as ${mapSignatureToKotlinType(type)}"
	                }
                }
            }
            is DBusSignature.Variant ->
                "readDynamicArray(\"av\")"
            else -> "readDynamicArray(\"${typeSignature}\")"
        }
        is DBusSignature.Variant -> "readVariant()"
        is DBusSignature.Struct ->  when (type.fields.size) {
            2 -> {
                val field1 = type.fields[0]
                val field2 = type.fields[1]
                "readStructPair({ ${generateReader(field1)} }, { ${generateReader(field2)} })"
            }
            3 -> {
                val field1 = type.fields[0]
                val field2 = type.fields[1]
                val field3 = type.fields[2]
                "readStructTriple({ ${generateReader(field1)} }, { ${generateReader(field2)} }, { ${generateReader(field3)} })"
            }
            else -> {
	            val kotlinTypes = type.fields.map { mapSignatureToKotlinType(it) }
	            val first = kotlinTypes.first()
	            val allSame = kotlinTypes.all { it == first }
	            if (allSame) {
		            val fieldReaders = type.fields.map { field -> generateReader(field) }.joinToString(", ")
		            "readStruct { listOf($fieldReaders) }"
	            } else {
		            "readDynamicStruct() as ${mapSignatureToKotlinType(type)}"
	            }
            }
        }
        is DBusSignature.DictionaryEntry -> error("DictionaryEntry cannot appear top-level; must be inside an array")
    }
}

private fun arrayWriterName(element: DBusSignature): String = when (element) {
    is DBusSignature.Primitive -> when (element.code) {
        'y' -> "writeByteArray"
        'b' -> "writeBooleanArray"
        'n' -> "writeShortArray"
        'q' -> "writeUShortArray"
        'i' -> "writeIntArray"
        'u' -> "writeUIntArray"
        'x' -> "writeLongArray"
        't' -> "writeULongArray"
        'd' -> "writeDoubleArray"
        's' -> "writeStringArray"
        'o' -> "writeObjectPathArray"
        'g' -> "writeSignatureArray"
        'h' -> "writeFileDescriptorArray"
        else -> error("Unsupported primitive array element '${element.code}'")
    }

    is DBusSignature.DictionaryEntry -> TODO()

    is DBusSignature.Variant -> TODO()

    is DBusSignature.Array -> TODO()

    is DBusSignature.Struct -> TODO()
}


private fun getClassName(interfaceName: String): String {
    val parts = interfaceName.split(".")
    val namespace = parts.dropLast(1).joinToString(".")
    val name = parts.last()

    return when {
        namespace.startsWith("org.freedesktop.portal") -> "${name}Portal"
        else -> name
    }
}

private fun convertName(raw: String): String =
    raw.split('_')
        .mapIndexed { index, part ->
            if (index == 0) part.lowercase()
            else part.replaceFirstChar { it.uppercase() }
        }
        .joinToString("")