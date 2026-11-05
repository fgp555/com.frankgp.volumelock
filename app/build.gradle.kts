import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.frankgp.volumelock"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.frankgp.volumelock"
        minSdk = 24
        targetSdk = 37
        versionCode = 8
        versionName = "26.10.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.media:media:1.8.0")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// app-publisher: release signing from local.properties or environment
val appPublisherLocalProperties = Properties()
val appPublisherLocalPropertiesFile = rootProject.file("local.properties")
if (appPublisherLocalPropertiesFile.exists()) {
    appPublisherLocalPropertiesFile.inputStream().use { appPublisherLocalProperties.load(it) }
}
fun appPublisherSigningValue(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: appPublisherLocalProperties.getProperty(name)?.takeIf { it.isNotBlank() }

val appPublisherKeystoreFile = appPublisherSigningValue("KEYSTORE_FILE")
if (!appPublisherKeystoreFile.isNullOrBlank()) {
    android {
        signingConfigs {
            create("appPublisherRelease") {
                storeFile = file(appPublisherKeystoreFile)
                storePassword = appPublisherSigningValue("KEYSTORE_PASSWORD")
                keyAlias = appPublisherSigningValue("KEY_ALIAS")
                keyPassword = appPublisherSigningValue("KEY_PASSWORD")
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("appPublisherRelease")
            }
        }
    }
}
