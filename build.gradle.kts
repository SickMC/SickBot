plugins {
    application
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"

    id("com.github.breadmoirai.github-release") version "2.4.1"
}

group = "net.sickmc"
version = "2.0.0"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("dev.kord:kord-core:0.8.0-M16")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("net.sickmc:sickapi:1.0.10")

    implementation("org.slf4j:slf4j-simple:2.0.3")

    val ktorVersion = "2.1.2"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}

tasks{
    compileJava{
        options.encoding = "UTF-8"
        options.release.set(18)
    }
    compileKotlin{
        kotlinOptions.jvmTarget = "18"
    }
}

application{
    mainClass.set("net.sickmc.sickbot.StartupKt")
}

githubRelease {
    token(findProperty("github.token")?.toString())

    val split = "SickMC/SickBot".split("/")
    owner(split[0])
    repo(split[1])
    tagName("v${project.version}")
    body("Add mongo integration")
    targetCommitish("master")
}