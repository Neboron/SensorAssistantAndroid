plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.aitavrd"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.aitavrd"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.androidplot:androidplot-core:1.5.10")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.github.MohammedAlaaMorsi:RangeSeekBar:1.0.6")
    //implementation("com.illposed.osc:javaosc-core:0.9")
    implementation("com.illposed.osc:javaosc-core:0.9")
    implementation(files("libs/libStreamSDK_v1.2.0.jar"))
    implementation(files("libs/NskAlgoSdk.jar"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Optional: core library desugaring for using Java 8+ APIs on older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.2")

    // Add NeuroSky Android SDK dependency from JitPack
    implementation("com.github.pwittchen:neurosky-android-sdk:544f19e")
}
