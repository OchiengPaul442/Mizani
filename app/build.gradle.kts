plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val mizaniVersionName = providers.gradleProperty("mizaniVersionName").get()
val mizaniVersionParts = Regex("^(0|[1-9]\\d{0,2})\\.(0|[1-9]\\d{0,2})\\.(0|[1-9]\\d{0,2})$")
    .matchEntire(mizaniVersionName)
    ?: throw GradleException("mizaniVersionName must use MAJOR.MINOR.PATCH, with each part from 0 to 999")
val (mizaniMajor, mizaniMinor, mizaniPatch) = mizaniVersionParts.groupValues.drop(1).map(String::toInt)
val mizaniVersionCode = mizaniMajor * 1_000_000 + mizaniMinor * 1_000 + mizaniPatch
if (mizaniVersionCode == 0) {
    throw GradleException("mizaniVersionName must be greater than 0.0.0")
}

android {
    namespace = "com.mizani.money"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mizani.money"
        minSdk = 24
        targetSdk = 37
        versionCode = mizaniVersionCode
        versionName = mizaniVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    androidResources {
        // English-only UI; drop unused library translations (AGP 9.4 replacement for resourceConfigurations).
        localeFilters += listOf("en")
    }

    buildTypes {
        release {
            // R8 code shrinking + resource optimization enabled (AGP 9.3+ DSL).
            // Default Android keep rules were sufficient — Room (KSP), SQLCipher,
            // and MPAndroidChart all kept the code they need with no app-side rules.
            // If a future dependency breaks under shrinking, add keep rules in
            // src/release/keepRules/*.keep and re-run :app:assembleRelease.
            optimization {
                enable = true
            }
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.sqlcipher.android)

    implementation(libs.mpandroidchart)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
