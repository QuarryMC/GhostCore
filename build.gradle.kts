import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "me.kooper"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.triumphteam.dev/snapshots/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Provided dependencies
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper.packetevents:spigot:2.0.2")

    // Regular dependencies
    implementation("it.unimi.dsi:fastutil:8.5.12")
    implementation("dev.triumphteam:triumph-gui:3.1.1")
    implementation("org.decimal4j:decimal4j:1.0.3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.kooper.GhostCoreKt" // Adjust for your main class
    }
}

tasks.create("buildAndCopy", Copy::class) {
    group = "server"
    dependsOn("shadowJar")
    from(tasks.getByName("shadowJar").outputs)
    into("C:\\Users\\riley\\AppData\\Roaming\\.feather\\player-server\\servers\\cb8c898a-5ed9-47ed-996d-b7f378555200\\plugins")
}