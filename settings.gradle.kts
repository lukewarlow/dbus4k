plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "dbus4k"

include(":codegen:common")
include(":codegen:gradle")
include(":codegen:amper")
include(":runtime")