import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// local.properties is untracked, so each machine can point at its own transcription server.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val transcriptionBaseUrl = providers.gradleProperty("transcriptionBaseUrl")
    .getOrElse(localProperties.getProperty("transcriptionBaseUrl").orEmpty())
    .trim()

android {
    namespace = "com.example.cs265final"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.cs265final"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TRANSCRIPTION_BASE_URL", "\"${transcriptionBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.work.runtime)
    implementation(libs.jump3r)
    testImplementation(libs.junit)
    testImplementation(libs.json) // JVM unit tests have no Android org.json implementation.
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
