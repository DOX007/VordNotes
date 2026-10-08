plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.mhss.app.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/LICENSE"
            )
        }
    }
}

dependencies {
    implementation(project(":core:util"))
    implementation(project(":core:database"))
    implementation(project(":settings:domain"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    implementation(libs.koin.android)
    ksp(libs.koin.ksp.compiler)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.documentfile)
}