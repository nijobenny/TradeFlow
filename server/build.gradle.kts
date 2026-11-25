plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "com.nijobenny.tradeflow"
version = "1.0.0"

application {
    mainClass.set("com.nijobenny.tradeflow.server.ApplicationKt")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.embedded.postgres)
    implementation(libs.jbcrypt)
    implementation(libs.logback)

    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}
