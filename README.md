# F3D Android application

Android application built on top of [F3D](https://github.com/f3d-app/f3d).

## Prerequisites

- Docker (for building native libraries)
- Android SDK >= 36
- Java 17

> [!NOTE]
> Android >= 9 (API level 28) is needed on a device to install the generated .apk.

## Update Native Libraries

The `update_native_libs.sh` script clones F3D, cross-compiles it for Android inside Docker containers, and copies the resulting `.so` and `.jar` files into the project.
A `jniLibs-lock.json` file describes where the native libraries are built from.
Unless a specific F3D version is needed, this step can be skipped.

```bash
# Build for all architectures (arm64-v8a, armeabi-v7a, x86_64, x86) using `jniLibs-lock.json` file
./update_native_libs.sh

# Build a specific release and save a new `jniLibs-lock.json` file
./update_native_libs.sh --branch v3.4.1

# Build only specific architectures using `jniLibs-lock.json` file
./update_native_libs.sh --arch arm64-v8a --arch x86_64

# Use a custom fork or branch and save a new `jniLibs-lock.json` file
./update_native_libs.sh --repo Meakk/f3d --branch my-feature

# Use an existing local clone (skip `jniLibs-lock.json` file logic, only for development purpose)
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

> [!NOTE]
> In order to build the release package, the signing file should be copied to `f3d/f3d-android-key.jks` and the environment variable `ANDROID_KEYSTORE_PASSWORD` should contain the correct password.

The resulting APK files are located in `f3d/build/outputs/apk/`.

## Testing the APK

Use the Gradle wrapper to run the tests:

```bash
./gradlew connectedAndroidTest --info
```

> [!WARNING]
> Some tests does image comparison and requires a specific device resolution. Currently, it's only tested on the following emulated devices:
> - small_phone
> - medium_phone
> - medium_tablet
> - desktop_large
