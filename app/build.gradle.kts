import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { inputStream ->
        localProperties.load(inputStream)
    }
}

fun releaseSigningProperty(propertyName: String): String? {
    return providers.gradleProperty(propertyName).orNull
        ?: localProperties.getProperty(propertyName)
}

val releaseStoreFile = releaseSigningProperty("release.store.file")

android {
    namespace = "com.randomingenuity.image_to_directions"
    compileSdk = 35

    signingConfigs {
        create("release") {
            releaseStoreFile?.let { storeFilePath ->
                storeFile = rootProject.file(storeFilePath)
                storePassword = releaseSigningProperty("release.store.password")
                keyAlias = releaseSigningProperty("release.key.alias")
                keyPassword = releaseSigningProperty("release.key.password")
            }
        }
    }

    defaultConfig {
        applicationId = "com.randomingenuity.image_to_directions"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "1.4.0"
    }

    buildTypes {
        release {
            releaseStoreFile?.let {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
