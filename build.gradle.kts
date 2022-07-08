plugins {
    application
    kotlin("jvm") version "1.7.0"
}

group = "net.sickmc"
version = "1.0"

repositories {
    mavenCentral()

    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("dev.kord:kord-core:0.8.x-SNAPSHOT")
    implementation("dev.kord.x:emoji:0.5.0")

    implementation("org.litote.kmongo:kmongo-coroutine:4.6.1")

    implementation("org.slf4j:slf4j-simple:2.0.0-alpha7")

    val ktorVersion = "2.0.3"
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
        options.release.set(17)
    }
    compileKotlin{
        kotlinOptions.jvmTarget = "17"
    }

}

application{
    mainClass.set("net.sickmc.sickbot.StartupKt")
}
