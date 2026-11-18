import com.android.build.api.variant.FilterConfiguration

plugins {
    id("com.android.application")
}

val supportedAbiCodesMap = mapOf(
    "armeabi-v7a" to 1,
    "arm64-v8a" to 2,
    "x86" to 3,
    "x86_64" to 4,
)

android {
    compileSdk = 36

    defaultConfig {
        applicationId = "app.f3d.F3D.android"
        minSdk = 28
        targetSdk = 36

        // Version code follows the format MAJOR*10000 + MINOR*100 + PATCH*100 + optional EXTRA
        // Examples: v4.0.1   -> 4000100
        //           v4.0.1-1 -> 4000101
        // The extra version should be avoided except if an Android-specific fix is needed
        versionCode = 4000103
        versionName = "v4.0.1-3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["additionalTestOutputDir"] = "/storage/emulated/0/Android/data/app.f3d.F3D.android/files"
    }

    splits {
        abi {
            isEnable = !gradle.startParameter.taskNames.any { it.lowercase().contains("bundle") }
            reset()
            include(*supportedAbiCodesMap.keys.toTypedArray())
            isUniversalApk = false
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("f3d-android-key.jks")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = "release"
            keyPassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
        }
    }

    packaging {
        // Remove warning "Unable to strip the following libraries, packaging them as they are:"
        jniLibs.keepDebugSymbols.add("**/libf3d-java.so")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
    }
    namespace = "app.f3d.F3D.android"
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters.find { it.filterType == FilterConfiguration.FilterType.ABI }?.identifier ?: "universal"
            output.outputFileName.set(output.versionName.map { "f3d-android-${it}-${abi}.apk" })

            val versionCode = output.versionCode.get()
            output.versionCode.set(versionCode * 10 + (supportedAbiCodesMap[abi] ?: 0))
        }
    }
}

dependencies {
    implementation("androidx.activity:activity:1.13.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.browser:browser:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.14.0")

    if (providers.gradleProperty("f3d.use.local.library").getOrElse("false") == "true") {
        implementation(project(":library"))
    } else {
        // we don't necessarily need to match the app version here
        // but let's try to do so when our process will be stable
        implementation("app.f3d:libf3d:4.0.1-2")
    }

    // For UI Tests
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
