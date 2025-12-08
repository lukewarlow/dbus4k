plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "dev.lukewarlow.dbus4k"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
	applyDefaultHierarchyTemplate()
    linuxArm64 {
	    compilations["main"].cinterops {
		    val sdbus by creating {
			    defFile(project.file("src/linuxMain/cinterop/sdbus.def"))

			    compilerOpts("-nostdinc", "-nostdinc++")

			    compilerOpts(
				    "-isystem", "/usr/include",
				    "-isystem", "/usr/include/systemd"
			    )

			    compilerOpts(
				    "-isystem", "/usr/include/aarch64-linux-gnu",
				    "--sysroot=/"
			    )
		    }
	    }
    }
    linuxX64 {
	    compilations["main"].cinterops {
		    val sdbus by creating {
			    defFile(project.file("src/linuxMain/cinterop/sdbus.def"))

			    // First disable bundled sysroot completely
			    compilerOpts("-nostdinc", "-nostdinc++")

			    compilerOpts(
				    "-isystem", "/usr/include",
				    "-isystem", "/usr/include/systemd"
			    )
			    compilerOpts(
				    "-isystem", "/usr/include/x86_64-linux-gnu",
				    "--sysroot=/"
			    )
		    }
	    }
    }

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
}

