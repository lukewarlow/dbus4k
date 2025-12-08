package dev.lukewarlow.dbus4k.codegen

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateKotlinFromXmlTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ListProperty<File>

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        val output = outputDirectory.get().asFile
        output.mkdirs()

        files.get().forEach { file ->
			logger.lifecycle("Started processing ${file.path}")
            val xmlContent = file.readText()
            val fileName = file.nameWithoutExtension + ".kt"
            val kotlinSource = processXML(xmlContent, fileName, packageName.get())
            val outFile = output.resolve(fileName)
            outFile.writeText(kotlinSource)
            logger.lifecycle("Generated Kotlin: ${outFile.path}")
        }
    }
}