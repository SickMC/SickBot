plugins {
    kotlin("jvm") version "1.6.10"
}

group = "me.anton"
version = "1.0"

repositories {
    mavenCentral()

    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("dev.kord:kord-core:0.8.0-M12")
    implementation("org.litote.kmongo:kmongo-coroutine:4.5.0")
    implementation("dev.kord.x:emoji:0.5.0")
    implementation("org.slf4j:slf4j-simple:1.7.30")
}