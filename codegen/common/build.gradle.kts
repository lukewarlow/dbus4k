plugins {
    alias(libs.plugins.kotlinJVM) apply true
}

group = "dev.lukewarlow"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlin.test)
}

sourceSets {

}