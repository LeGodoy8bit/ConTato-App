plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "contato.app"
    compileSdk {
        version = release(37) { //O app é compilado usando API 37.1
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "contato.app"
        minSdk = 29 //versao minima que vai rodar android 10 (API 29)
        targetSdk = 36 //o app sera redirecionado testado no android 16 (API 36)
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}