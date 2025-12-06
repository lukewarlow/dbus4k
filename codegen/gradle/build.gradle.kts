plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation(kotlin("stdlib"))
    // Gradle plugin API
    implementation(gradleApi())
    implementation(project(":codegen:common"))
}

gradlePlugin {
    plugins {
        create("dbus4kCodegenPlugin") {
            id = "dev.lukewarlow.dbus4k.codegen"
            implementationClass = "dev.lukewarlow.dbus4k.codegen.GradleCodegenPlugin"
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
