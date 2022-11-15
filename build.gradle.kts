plugins {
    application
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"

    id("com.github.breadmoirai.github-release") version "2.4.1"
}

group = "net.sickmc"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.kord:kord-core:0.8.0-M17")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("net.sickmc:sickapi:1.0.12")

    val ktorVersion = "2.1.3"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
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