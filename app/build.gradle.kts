plugins {
    alias(libs.plugins.google.services)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "com.mhss.app.mybrain"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mhss.app.mybrain"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "2.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // ← Add this
            resValue("string", "app_name", "VordNotes")
        }

        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            isDebuggable = true

            // Keep or change this one – both variants now define app_name the same way
            resValue("string", "app_name", "VordNotes")
            // or: resValue("string", "app_name", "VordNotes Debug") if you want to visually distinguish debug builds
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
        compose = true
        buildConfig = true
    }
    android {
        packaging {
            resources {
                // Samma stil: en rad per mönster med {…}-gruppering
                excludes.add("/META-INF/{AL2.0,LGPL2.1}")
                excludes.add("/META-INF/{LICENSE,LICENSE.txt,LICENSE.md}")
                excludes.add("/META-INF/{NOTICE,NOTICE.txt,NOTICE.md}")
                excludes.add("/META-INF/{DEPENDENCIES,DEPENDENCY,DEPENDENCY.txt}")
                excludes.add("/META-INF/{INDEX.LIST,INDEX}")
                // (valfritt men vanligt)
                excludes.add("/META-INF/{CHANGES,CHANGELOG,CHANGELOG.md}")
                excludes.add("/META-INF/{README,README.txt,README.md}")
            }
        }
    }
    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
    lint {
        disable.add("MissingTranslation")
        disable.add("NullSafeMutableLiveData")
    }
}

dependencies {
    implementation("com.benasher44:uuid:0.7.0")
    implementation(project(":core:ui"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("androidx.activity:activity-ktx:1.9.3")

    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation(project(":notes:domain"))
    implementation(project(":tasks:domain"))
    implementation(project(":diary:domain"))

    implementation("com.google.accompanist:accompanist-flowlayout:0.30.1")
    implementation(project(":notes:presentation"))
    implementation(project(":tasks:presentation"))
    implementation(project(":bookmarks:presentation"))
    implementation(project(":calendar:presentation"))
    implementation(project(":diary:presentation"))
    implementation(project(":settings:presentation"))
    implementation(project(":ai:presentation"))

    implementation(project(":notes:data"))
    implementation(project(":tasks:data"))
    implementation(project(":bookmarks:data"))
    implementation(project(":diary:data"))
    implementation(project(":calendar:data"))
    implementation(project(":ai:data"))
    implementation(project(":settings:data"))

    implementation(project(":tasks:domain"))
    implementation(project(":calendar:domain"))
    implementation(project(":diary:domain"))

    implementation(project(":core:notification"))
    implementation(project(":core:ui"))
    implementation(project(":core:di"))
    implementation(project(":core:alarm"))
    implementation(project(":core:database"))
    implementation(project(":widget"))
    implementation(project(":core:preferences"))
    implementation(project(":core:util"))
    implementation(project(":core:network"))

    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.test.junit4)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.biometric)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    implementation(libs.koin.android)
    implementation(libs.koin.android.workmanager)
    ksp(libs.koin.ksp.compiler)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.ktor.okhttp)
    implementation(libs.ktor.logging)

    implementation(libs.squircle.shape)
}

