package dev.lukewarlow.dbus4k.codegen

import org.gradle.api.Plugin
import org.gradle.api.Project

class GradleCodegenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("dbus4k", DBus4kExtension::class.java)
        val taskProvider = project.tasks.register("generateDBusKotlin", GenerateKotlinFromXmlTask::class.java)
        taskProvider.configure {
            this.packageName
                .convention(extension.packageName.orElse("dev.lukewarlow.dbus4k.generated"))
	        this.outputDirectory
                .convention(extension.output.orElse(project.layout.buildDirectory.dir("generated/dbus/kotlin")))

            val defaultXml = project.layout.projectDirectory
                .dir("src/linuxMain/dbus")
                .asFileTree
                .matching { "include" to "**/*.xml" }
                .files
                .toList()

	        this.files.convention(defaultXml)

            val extensionXml = extension.xml
                .asFileTree
                .matching { "include" to "**/*.xml" }
                .files
                .toList()

            if (extensionXml.isNotEmpty()) {
	            this.files.set(extensionXml)
            }
        }

	    project.tasks.configureEach {
		    if (this.name.startsWith("compileKotlin")) {
			    this.dependsOn(taskProvider)
		    }
	    }

    }
}