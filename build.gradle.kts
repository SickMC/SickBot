plugins {
    kotlin("jvm") version "1.6.21"
}

group = "me.anton"
version = "1.0"

repositories {
    mavenCentral()

    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("dev.kord:kord-core:0.8.0-M12")
    implementation("org.litote.kmongo:kmongo-coroutine:4.5.1")
    implementation("dev.kord.x:emoji:0.5.0")
    implementation("io.github.crackthecodeabhi:kreds:0.7")
}