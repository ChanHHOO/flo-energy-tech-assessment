plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.flo"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin stdlib
    implementation(kotlin("stdlib"))

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
    args = listOf(
        "src/test/resources/sample.nem12",
        "output.sql"
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
