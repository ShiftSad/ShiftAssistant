plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "dev.shiftsad"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        name = "placeholderapi-repo"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation(platform("com.aallam.openai:openai-client-bom:4.0.1"))
    implementation(platform("com.aallam.ktoken:ktoken-bom:0.4.0"))
    implementation("com.aallam.openai:openai-client")
    implementation("com.aallam.ktoken:ktoken")
    runtimeOnly("io.ktor:ktor-client-okhttp")

    implementation(platform("com.fasterxml.jackson:jackson-bom:2.19.2"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.apache.lucene:lucene-core:10.2.2")
    implementation("org.apache.lucene:lucene-analyzers-common:10.2.2")
    implementation("org.apache.lucene:lucene-knn:10.2.2")

    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks {
    runServer {
        minecraftVersion("1.21.8")
    }
}

val targetJavaVersion = 17
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}
