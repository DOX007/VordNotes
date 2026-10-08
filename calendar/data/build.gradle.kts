plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
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
}

dependencies {

    implementation(project(":core:alarm"))
    implementation(project(":core:ui"))
    implementation(project(":calendar:domain"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    implementation(libs.koin.android)
    ksp(libs.koin.ksp.compiler)
}
