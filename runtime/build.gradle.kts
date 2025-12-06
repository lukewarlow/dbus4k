plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "dev.lukewarlow"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    applyDefaultHierarchyTemplate()

    linuxArm64()
    linuxX64()

    sourceSets {
        val linuxMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.experimental.ExperimentalNativeApi",
            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
        )
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        compilations["main"].cinterops {
            val sdbus by creating {
                defFile(project.file("src/linuxMain/cinterop/sdbus.def"))

                // First disable bundled sysroot completely
                compilerOpts("-nostdinc", "-nostdinc++")

                compilerOpts(
                    "-isystem", "/usr/include",
                    "-isystem", "/usr/include/systemd"
                )

                when (targetName) {
                    "linuxX64" -> compilerOpts(
                        "-isystem", "/usr/include/x86_64-linux-gnu",
                        "--sysroot=/"
                    )

                    "linuxArm64" -> {
                        compilerOpts(
                            "-isystem", "/usr/include/aarch64-linux-gnu",
                            "--sysroot=/"
                        )
                    }
                }
            }
        }

        binaries {
            all {
                if (targetName == "linuxX64") {
                    linkerOpts(
                        "-L/usr/lib/x86_64-linux-gnu",
                        "-L/lib/x86_64-linux-gnu"
                    )
                }
                if (targetName == "linuxArm64") {
                    linkerOpts(
                        "-L/usr/lib/aarch64-linux-gnu",
                        "-L/lib/aarch64-linux-gnu",
                    )
                }
            }
        }
    }
}