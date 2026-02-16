# F3D Android application

Android application built on top of [F3D](https://github.com/f3d-app/f3d).

## Prerequisites

- Docker (for building native libraries)
- Android SDK
- Java 17

## Update Native Libraries

The `update_native_libs.sh` script clones F3D, cross-compiles it for Android inside Docker containers, and copies the resulting `.so` and `.jar` files into the project.
Unless a specific F3D version is needed, this step can be skipped.

```bash
# Build for all architectures (arm64-v8a, armeabi-v7a, x86_64, x86)
./update_native_libs.sh

# Build a specific release
./update_native_libs.sh --branch v3.4.1

# Build only specific architectures
./update_native_libs.sh --arch arm64-v8a --arch x86_64

# Use a custom fork or branch
./update_native_libs.sh --repo Meakk/f3d --branch my-feature

# Use an existing local clone
./update_native_libs.sh --clone-dir ~/dev/f3d-src
```

Run `./update_native_libs.sh --help` for the full list of options.

## Set Java 17 environment

It's recommended to use Java 17 because the build can break with untested recent Java versions.
If several Java versions are installed, one can set the following variable:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

## Build the APK

Use the Gradle wrapper to build:

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

The resulting APK files are located in `app/build/outputs/apk/`.
