plugins {
    alias(libs.plugins.kotlinjvmbase)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.example.famekodriver"
version = "1.0.0"

application {
    mainClass.set("com.example.famekodriver.backend.ApplicationKt")
}

dependencies {
    implementation(project(":shared-models"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.thymeleaf)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.firebase.admin)
    implementation(libs.h3)
    implementation(libs.jedis)
    implementation(libs.jakarta.mail)
    implementation(libs.google.api.client)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.jbcrypt)
}

kotlin {
    jvmToolchain(17)
}
