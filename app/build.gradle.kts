plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.softcg.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.softcg.myapplication"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
    buildFeatures{
        viewBinding=true
    }
    hilt{
        enableAggregatingTask=false
    }
    
}

dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    //FRAGMENT
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    //ACTIVITY
    implementation("androidx.activity:activity-ktx:1.8.2")
    //VIEWMODEL
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    //LIVEDATA
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    //DaggerHilt
    implementation("com.google.dagger:hilt-android:2.50")
    implementation("junit:junit:4.12")

    ksp("com.google.dagger:hilt-android-compiler:2.50")
    //API SPLASH SCREEN
    implementation("androidx.core:core-splashscreen:1.0.0")
    //Corrutinas
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
    //ojito
    implementation ("com.google.android.material:material:1.5.0")

    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")

    //Menú
    implementation("de.hdodenhof:circleimageview:3.1.0")

    //Botón flotante
    implementation("com.getbase:floatingactionbutton:1.10.1")

    implementation("com.google.android.material:material:1.11.0")
    //ROOM
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    //mockito
    testImplementation ("junit:junit:4.+")
    testImplementation ("androidx.test:core-ktx:1.5.0")
    testImplementation ("org.mockito:mockito-core:5.11.0")
    testImplementation ("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation ("io.mockk:mockk:1.13.10")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.0")

}