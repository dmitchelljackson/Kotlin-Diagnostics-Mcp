plugins {
    kotlin("jvm") version "2.2.0"
    application
    id("com.gradleup.shadow") version "8.3.5"
}

group = "kotlindiagnosticsmcp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure the application plugin
application {
    mainClass.set("kotlindiagnosticsmcp.MainKt")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.3.1")

    // Kotlin logging with simple SLF4J implementation
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.17")
}

tasks.test {
    useJUnitPlatform()
}

// Custom task to run the diagnostics test
tasks.register<JavaExec>("testDiagnostics") {
    group = "verification"
    description = "Run the Kotlin LSP diagnostics test"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("kotlindiagnosticsmcp.lsp.TestDiagnostics")

    // Configure SLF4J Simple Logger via system properties
    systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO")
    systemProperty("org.slf4j.simpleLogger.log.kotlindiagnosticsmcp.lsp.TestDiagnostics", "DEBUG")
    systemProperty("org.slf4j.simpleLogger.log.kotlindiagnosticsmcp.lsp.KotlinLanguageServerClient", "DEBUG")
    systemProperty("org.slf4j.simpleLogger.showDateTime", "true")
    systemProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss.SSS")
    systemProperty("org.slf4j.simpleLogger.showLogName", "true")
    systemProperty("org.slf4j.simpleLogger.showThreadName", "true")
    systemProperty("org.slf4j.simpleLogger.logFile", "logs/kotlin-diagnostics-mcp.log")
}

kotlin {
    jvmToolchain(21)
}

// Configure JAR naming
tasks.jar {
    archiveBaseName.set("kotlin-diagnostics")
    archiveVersion.set("")  // Remove version from filename
}

tasks.shadowJar {
    archiveBaseName.set("kotlin-diagnostics")
    archiveClassifier.set("")  // Remove the "-all" suffix from shadow JAR
    archiveVersion.set("")  // Remove version from filename
}