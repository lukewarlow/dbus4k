# dbus4k

dbus4k consists of two parts:

1. A minimal D-Bus client library for Kotlin/Native, built around systemd's sd-bus library.
2. A code generator for creating typed wrappers from D-Bus introspection XML.

The runtime library only supports the linuxArm64 and linuxX64 targets for Kotlin/Native.

The codegen runs via Kotlin/JVM.