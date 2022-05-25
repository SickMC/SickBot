plugins {
    application
    kotlin("jvm") version "1.6.21"
}

group = "net.sickmc"
version = "1.0"

repositories {
    mavenCentral()

    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("dev.kord:kord-core:0.8.0-M14")
    implementation("dev.kord.x:emoji:0.5.0")

    implementation("org.litote.kmongo:kmongo-coroutine:4.5.1")
    implementation("io.github.crackthecodeabhi:kreds:0.7")

    implementation("org.slf4j:slf4j-simple:1.7.36")
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}

tasks{

    compileJava{
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    compileKotlin{
        kotlinOptions.jvmTarget = "17"
    }

}

application{
    mainClass.set("net.sickmc.sickbot.MainKt")
}
