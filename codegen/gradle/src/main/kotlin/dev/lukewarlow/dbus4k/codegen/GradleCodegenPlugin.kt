package dev.lukewarlow.dbus4k.codegen

import org.gradle.api.Plugin
import org.gradle.api.Project

class GradleCodegenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("dbus4k", DBus4kExtension::class.java)
        val taskProvider = project.tasks.register("generateDBusKotlin", GenerateKotlinFromXmlTask::class.java)
        taskProvider.configure { task ->
            task.packageName
                .convention("dev.lukewarlow.dbus4k.generated")
                .set(extension.packageName)
            task.outputDirectory
                .convention(project.layout.buildDirectory.dir("generated/dbus/kotlin"))
                .set(extension.output)

            val defaultXml = project.layout.projectDirectory
                .dir("src/linuxMain/dbus")
                .asFileTree
                .matching { "include" to "**/*.xml" }
                .files
                .toList()

            task.files.convention(defaultXml)


            val extensionXml = extension.xml
                .asFileTree
                .matching { "include" to "**/*.xml" }
                .files
                .toList()

            if (extensionXml.isNotEmpty()) {
                task.files.set(extensionXml)
            }
        }

        project.tasks.named("compileKotlin").configure {
            it.dependsOn(taskProvider)
        }
    }
}