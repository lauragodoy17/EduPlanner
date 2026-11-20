import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.googleServices)
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

        // Encuesta: aplicación web de Google Apps Script que escribe en la hoja (ver encuesta/LEEME.md).
        // Se configura en local.properties con encuesta.url y encuesta.clave; sin url, la app avisa que no está lista.
        val locales = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        fun textoBuildConfig(valor: String) = "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        buildConfigField("String", "ENCUESTA_URL", textoBuildConfig(locales.getProperty("encuesta.url", "")))
        buildConfigField("String", "ENCUESTA_CLAVE", textoBuildConfig(locales.getProperty("encuesta.clave", "")))
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
        buildConfig=true
    }
    hilt{
        enableAggregatingTask=false
    }
    testOptions {
        // Los métodos de android.jar (p. ej. Log.d) devuelven valores por defecto en los tests unitarios
        unitTests.isReturnDefaultValues = true
    }
    
}

dependencies {

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

    ksp("com.google.dagger:hilt-android-compiler:2.50")
    //API SPLASH SCREEN
    implementation("androidx.core:core-splashscreen:1.0.0")
    //Corrutinas
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    // Inicio de sesión con Google: Credential Manager entrega el ID token que se canjea en Firebase Auth
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    // IA (Firebase AI Logic, Gemini Developer API). Versión fija fuera del BoM: la 16.x está compilada con Kotlin 1.8,
    // compatible con el Kotlin 1.9 del proyecto (las 17+ y los BoM 33.16+ exigen Kotlin 2)
    implementation("com.google.firebase:firebase-ai:16.2.0")
    // App Check: AI Logic rechaza las peticiones sin token. Play Integrity en release, proveedor de depuración en debug
    implementation("com.google.firebase:firebase-appcheck")
    releaseImplementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    // Compartir por QR: ZXing dibuja el código; CameraX y ML Kit (modelo incluido en el APK, funciona sin
    // conexión) lo leen. CameraX 1.4.2: sus .so están alineados a 16 KB (Android 15+; la 1.3 no) y aún admite compileSdk 34 (la 1.5 exige 35)
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    //Menú
    implementation("de.hdodenhof:circleimageview:3.1.0")

    //Botón flotante
    implementation("com.getbase:floatingactionbutton:1.10.1")

    //ROOM
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    //mockito
    testImplementation ("androidx.test:core-ktx:1.5.0")
    testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation ("androidx.arch.core:core-testing:2.2.0")
    testImplementation ("org.mockito:mockito-core:5.11.0")
    testImplementation ("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation ("io.mockk:mockk:1.13.10")
    // org.json real: el de android.jar en los tests de JVM solo devuelve valores por defecto
    testImplementation ("org.json:json:20231013")

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
    //pestañas del perfil
    implementation("androidx.viewpager2:viewpager2:1.0.0")

}