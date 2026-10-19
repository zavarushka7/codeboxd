plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ru.savushkina.core.domain"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))

    // Domain-интерфейсы используют kotlinx.coroutines.Flow и StateFlow.
    // Без явного подключения компилятор их не видит.
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}