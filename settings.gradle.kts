rootProject.name = "dbus4k"

pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
	}
}

include(":codegen:common")
include(":codegen:gradle")
include(":runtime")