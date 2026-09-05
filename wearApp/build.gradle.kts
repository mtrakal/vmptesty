import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    // Ze :shared bere jen data a quiz logiku, telefonní UI nepoužívá.
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodelCompose)

    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutinesTest)
}

android {
    namespace = "cz.mtrakal.vmptesty.wear"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "cz.mtrakal.vmptesty.wear"
        // Wear OS 3 a novější.
        minSdk = libs.versions.android.wearMinSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Podepisuje se vychozim ladicim klicem (~/.android/debug.keystore),
            // aby slo release APK rovnou nainstalovat na hodinky bez rucniho
            // apksigneru. Na Google Play by to nestacilo, tam je potreba
            // vlastni klic - ten sem ale nepatri, do repozitare se necommituje.
            signingConfig = signingConfigs.getByName("debug")
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
