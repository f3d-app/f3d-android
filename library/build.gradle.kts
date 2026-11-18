import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    id("com.android.library")
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }

    packaging {
        // Remove warning "Unable to strip the following libraries, packaging them as they are:"
        jniLibs.keepDebugSymbols.add("**/libf3d-java.so")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    namespace = "app.f3d.F3D.library"
}

dependencies {
    // Java bindings, exposed to consumers of the library
    api(files("libs/f3d.jar"))
}

val libf3dVersion = providers.gradleProperty("libf3dVersion")
if (libf3dVersion.isPresent) {
    apply(plugin = "com.vanniktech.maven.publish")

    configure<MavenPublishBaseExtension> {
        publishToMavenCentral()

        // Signing is only required when publishing, local builds do not need any key
        if (providers.gradleProperty("signingInMemoryKey").isPresent) {
            signAllPublications()
        }

        coordinates("app.f3d", "libf3d", libf3dVersion.get())

        pom {
            name.set("F3D")
            description.set("F3D native library with its Java bindings for Android")
            url.set("https://f3d.app")

            licenses {
                license {
                    name.set("BSD-3-Clause")
                    url.set("https://opensource.org/license/bsd-3-clause")
                }
            }

            developers {
                developer {
                    id.set("f3d-app")
                    name.set("F3D-APP Foundation")
                    url.set("https://www.f3d.app/")
                }
            }

            scm {
                url.set("https://github.com/f3d-app/f3d-android")
                connection.set("scm:git:https://github.com/f3d-app/f3d-android.git")
            }
        }
    }
}
