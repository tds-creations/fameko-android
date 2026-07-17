plugins {
    alias(libs.plugins.kotlinjvmbase)
}

dependencies {
    api(libs.gson)
}

kotlin {
    jvmToolchain(17)
}
