import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(name: String, default: String = ""): String =
    (findProperty(name) as String? ?: localProps.getProperty(name) ?: default).trim()

/** Pleos Connect SDK 포함 여부. 끄면 같은 이름의 Stub(src/pleosStub)이 들어가 일반 에뮬레이터에서도 실행됩니다. */
val usePleosSdk = prop("mungroad.usePleosSdk", "false").toBoolean()

android {
    namespace = "com.mungroad.car"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mungroad.car"
        minSdk = 28 // Pleos Connect SDK 요구사항: API 28 이상
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-mvp"
        buildConfigField("boolean", "USE_PLEOS_SDK", usePleosSdk.toString())
        buildConfigField("String", "MUNGROAD_API_BASE_URL", "\"${prop("MUNGROAD_API_BASE_URL")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets["main"].java.srcDir(if (usePleosSdk) "src/pleos/java" else "src/pleosStub/java")

    buildTypes {
        release {
            isMinifyEnabled = false
            // 공모전 시연용: 디버그 키로 서명(실배포 시 별도 키스토어 사용)
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // AAOS Car API(CarPropertyManager·CarUxRestrictionsManager). 휴대폰 에뮬레이터에는 없을 수 있어 required=false
    useLibrary("android.car")
    testOptions {
        unitTests.isReturnDefaultValues = true
        // 화면 스크린샷 테스트(Robolectric + Roborazzi): 리소스·폰트 포함
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.systemProperty("roborazzi.test.record", "true") }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    if (usePleosSdk) {
        implementation("ai.pleos.playground:Vehicle:${prop("pleos.vehicleVersion", "2.3.2")}")
        implementation("ai.pleos.playground:NaviHelper:${prop("pleos.naviHelperVersion", "3.0.5")}")
        implementation("ai.pleos.playground:TextToSpeech:${prop("pleos.ttsVersion", "3.3.1")}")
    }

    testImplementation("junit:junit:4.13.2")
    // 시안 비교용 화면 스크린샷(1600×900) — 앱 동작에는 포함되지 않음
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.32.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.32.2")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
