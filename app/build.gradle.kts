plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

fun loadAppMeta(): Triple<Int, String, String> {
    val file = rootProject.file("version.properties")
    var name = "1.0.0"
    var code = 1
    var updateUrl = ""
    if (file.exists()) {
        file.readLines().forEach { line ->
            val value = line.substringAfter("=", "").trim()
            when {
                line.startsWith("VERSION_NAME=") -> name = value
                line.startsWith("VERSION_CODE=") -> code = value.toIntOrNull() ?: 1
                line.startsWith("UPDATE_URL=") -> updateUrl = value
            }
        }
    }
    return Triple(code, name, updateUrl)
}

val (appVersionCode, appVersionName, appUpdateUrl) = loadAppMeta()

val keystorePropsFile = rootProject.file("keystore/keystore.properties")
val keystoreProps = linkedMapOf<String, String>()
if (keystorePropsFile.exists()) {
    keystorePropsFile.readLines().forEach { line ->
        val cut = line.indexOf('=')
        if (cut > 0 && !line.trimStart().startsWith("#")) {
            keystoreProps[line.substring(0, cut).trim()] = line.substring(cut + 1).trim()
        }
    }
}

android {
    namespace = "com.makay.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.makay.launcher"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "UPDATE_URL",
            "\"${appUpdateUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"",
        )
    }

    signingConfigs {
        create("release") {
            val store = keystoreProps["storeFile"]
            if (!store.isNullOrBlank()) {
                storeFile = rootProject.file(store)
                storePassword = keystoreProps["storePassword"]
                keyAlias = keystoreProps["keyAlias"]
                keyPassword = keystoreProps["keyPassword"]
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // Kullanılmayan kaynakları kaldır → APK boyutunu küçültür
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = true
        // AGP 8.7 lint dedektörü bu Kotlin sürümünde çöküyor; kapatılmazsa release derlemesi durur.
        disable += "NullSafeMutableLiveData"
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
}

dependencies {
    // Compose BOM – tüm Compose bileşenlerini tutarlı sürüme kilitler
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("androidx.biometric:biometric:1.4.0-alpha02")
    implementation("com.google.android.material:material:1.12.0")
    // Coil 3.x – Kotlin/Coroutines-first, Compose entegrasyonu
    implementation("io.coil-kt.coil3:coil:3.1.0")
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-gif:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.window:window:1.4.0")
    implementation("androidx.tvprovider:tvprovider:1.1.0")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250107")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}

afterEvaluate {
    tasks.matching { it.name == "packageRelease" || it.name == "signReleaseBundle" }.configureEach {
        doFirst {
            val storePath = keystoreProps["storeFile"]
            val store = storePath?.takeIf { it.isNotBlank() }?.let { rootProject.file(it) }
            val ready = store != null && store.exists() &&
                !keystoreProps["storePassword"].isNullOrBlank() &&
                !keystoreProps["keyAlias"].isNullOrBlank() &&
                !keystoreProps["keyPassword"].isNullOrBlank()
            if (!ready) {
                throw GradleException(
                    "Release imzası eksik. keystore/keystore.properties ve JKS dosyası gerekli; debug anahtarı kullanılmaz.",
                )
            }
        }
    }
    tasks.named("assembleRelease").configure {
        doLast {
            val apkDir = rootProject.file("APK")
            apkDir.mkdirs()
            val src = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
            if (!src.exists()) return@doLast
            val dest = apkDir.resolve("MakayLauncher-$appVersionName.apk")
            src.copyTo(dest, overwrite = true)

            // R8 obfuscation haritasi - release APK'nin stack trace'leri buradan
            // cozulebilir. APK ile ayni klasorde (surumli adla) ve GitHub Release
            // asset'i olarak yayinlanir.
            val mappingSrc = layout.buildDirectory.file("outputs/mapping/release/mapping.txt").get().asFile
            val mappingOut = apkDir.resolve("MakayLauncher-$appVersionName-mapping.txt")
            if (mappingSrc.exists()) {
                mappingSrc.copyTo(mappingOut, overwrite = true)
            } else {
                mappingOut.delete()
            }

            logger.lifecycle("APK klasoru: ${dest.absolutePath}")
        }
    }
}
