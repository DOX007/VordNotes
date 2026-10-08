plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "com.mhss.app.widget"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    lint {
        disable += "NullSafeMutableLiveData"
    }
}

dependencies {
    implementation("androidx.glance:glance-appwidget:1.1.0")  // eller nyare, t.ex. 1.1.0-rc01
    implementation("androidx.glance:glance-material3:1.1.0")
    implementation(project(":tasks:domain"))
    implementation(project(":calendar:domain"))
    implementation(project(":settings:domain"))
    implementation(project(":core:preferences"))
    
    implementation(project(":core:ui"))
    implementation(project(":core:util"))
    implementation(project(":core:widget"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    implementation(libs.koin.android)
    ksp(libs.koin.ksp.compiler)

    implementation(libs.bundles.androidx.glance)

    implementation(libs.kotlinx.serialization.json)
}