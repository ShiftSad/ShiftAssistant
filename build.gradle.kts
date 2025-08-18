plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
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
    maven("https://repo.codemc.org/repository/maven-public/") {
        name = "codemc-repo"
    }
}

dependencies {
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation(platform("com.aallam.openai:openai-client-bom:4.0.1"))
    implementation(platform("com.aallam.ktoken:ktoken-bom:0.4.0"))
    implementation("com.aallam.openai:openai-client")
    implementation("com.aallam.ktoken:ktoken")
    runtimeOnly("io.ktor:ktor-client-okhttp")

    // Force compatible Jackson versions
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.19.2"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-annotations")

    implementation("org.apache.lucene:lucene-core:10.2.2")
    implementation("org.apache.lucene:lucene-analysis-common:10.2.2")
    implementation("org.apache.lucene:lucene-backward-codecs:10.2.2")

    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:10.1.2")
}

tasks {
    runServer {
        minecraftVersion("1.21.8")
        jvmArgs("--add-modules", "jdk.incubator.vector")
    }

    build {
        dependsOn("shadowJar")
    }

    shadowJar {
        relocate("com.fasterxml.jackson", "dev.shiftsad.libs.jackson")
        relocate("dev.jorel.commandapi", "dev.shiftsad.libs.commandapi")
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}
