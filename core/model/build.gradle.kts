plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ru.savushkina.core.model"
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
    // Чистые модели. Без Firebase, без UI, без корутин.
    // Если понадобится @Parcelize — добавь `id("kotlin-parcelize")` в plugins.
}