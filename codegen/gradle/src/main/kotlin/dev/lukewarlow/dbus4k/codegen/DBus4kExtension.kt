package dev.lukewarlow.dbus4k.codegen

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class DBus4kExtension @Inject constructor(objects: ObjectFactory) {
    val xml: SourceDirectorySet = objects.sourceDirectorySet("dbusXml", "D-Bus XML source directories")

    val output = objects.directoryProperty()
    val packageName = objects.property(String::class.java)
}