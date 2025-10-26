plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

group = "com.flo"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin stdlib
    implementation(kotlin("stdlib"))

    // Database
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
}

application {
    mainClass.set("com.flo.nem12.MainKt")
}

tasks.named<JavaExec>("run") {
    // Default arguments for development
    args =
        listOf(
            "src/test/resources/sample.nem12",
            "output.db",
        )
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    archiveBaseName.set("nem12-parser")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("standalone")

    manifest {
        attributes["Main-Class"] = "com.flo.nem12.MainKt"
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.0.1")
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
