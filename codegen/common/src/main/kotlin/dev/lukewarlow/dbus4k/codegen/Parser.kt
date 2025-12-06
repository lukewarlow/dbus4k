package dev.lukewarlow.dbus4k.codegen

import dev.lukewarlow.dbus4k.codegen.ast.DBusArg
import dev.lukewarlow.dbus4k.codegen.ast.DBusArgDirection
import dev.lukewarlow.dbus4k.codegen.ast.DBusInterface
import dev.lukewarlow.dbus4k.codegen.ast.DBusIntrospectionNode
import dev.lukewarlow.dbus4k.codegen.ast.DBusMethod
import dev.lukewarlow.dbus4k.codegen.ast.DBusProperty
import dev.lukewarlow.dbus4k.codegen.ast.DBusPropertyAccess
import dev.lukewarlow.dbus4k.codegen.ast.DBusSignal
import dev.lukewarlow.dbus4k.codegen.ast.DBusSignature
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

internal fun parseXML(xmlFile: String): DBusIntrospectionNode {
    return parseXML(xmlFile.byteInputStream())
}
internal fun parseXML(xmlFile: File): DBusIntrospectionNode {
    return xmlFile.inputStream().use { parseXML(it) }
}
internal fun parseXML(xmlFile: InputStream): DBusIntrospectionNode {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val document = builder.parse(xmlFile)
    return parseNode(document.documentElement)
}

private fun parseNode(element: Element): DBusIntrospectionNode {
    val name = element.getAttribute("name")

    val interfaces = element.getElementsByTagName("interface").asSequence()
        .map { parseInterface(it as Element) }
        .toList()

    val children = element.getElementsByTagName("node").asSequence()
        .map { parseNode(it as Element) }
        .toList()

    return DBusIntrospectionNode(
        name,
        interfaces,
        children
    )
}

private fun parseInterface(element: Element): DBusInterface {
    val name = element.getAttribute("name")
    val methods = element.getElementsByTagName("method").asSequence()
        .map { parseMethod(it as Element) }
        .toList()
    val signals = element.getElementsByTagName("signal").asSequence()
        .map { parseSignal(it as Element) }
        .toList()
    val properties = element.getElementsByTagName("property").asSequence()
        .map { parseProperty(it as Element) }
        .toList()

    return DBusInterface(
        name,
        methods,
        signals,
        properties
    )
}

private fun parseMethod(element: Element): DBusMethod {
    val name = element.getAttribute("name")
    val args = element.getElementsByTagName("arg").asSequence()
        .map { parseArg(it as Element, DBusArgDirection.IN) }
        .toList()
    val (inArgs, outArgs) = args.partition { it.direction == DBusArgDirection.IN }
    // TODO annotations
    return DBusMethod(
        name,
        inArgs,
        outArgs,
        emptyList()
    )
}

private fun parseSignal(element: Element): DBusSignal {
    val name = element.getAttribute("name")
    val args = element.getElementsByTagName("arg").asSequence()
        .map { parseArg(it as Element, DBusArgDirection.OUT) }
        .toList()
    val (_, outArgs) = args.partition { it.direction == DBusArgDirection.IN }
    // TODO annotations
    return DBusSignal(
        name,
        outArgs,
        emptyList()
    )
}

private fun parseProperty(element: Element): DBusProperty {
    val name = element.getAttribute("name")
    val rawType = element.getAttribute("type")
    val rawAccess = element.getAttribute("access")
    val access = when (rawAccess) {
        "read" -> DBusPropertyAccess.READ
        "readwrite" -> DBusPropertyAccess.READWRITE
        "write" -> DBusPropertyAccess.WRITE
        else -> error("Unexpected access $rawAccess")
    }
    return DBusProperty(
        name,
        DBusSignature.parse(rawType),
        access,
    )
}

private fun parseArg(element: Element, defaultDirection: DBusArgDirection): DBusArg {
    val name = element.getAttribute("name")
    val rawType = element.getAttribute("type")
    val rawDirection = element.getAttribute("direction")
    val direction = when (rawDirection) {
        "in" -> DBusArgDirection.IN
        "out" -> DBusArgDirection.OUT
        "" -> defaultDirection
        else -> error("Unexpected direction $rawDirection")
    }

    // TODO annotations

    return DBusArg(
        name,
        DBusSignature.parse(rawType),
        direction,
        emptyList(),
    )
}

internal fun NodeList.asSequence(): Sequence<Node> = sequence {
    for (i in 0 until length) yield(item(i))
}