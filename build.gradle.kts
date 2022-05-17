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

    implementation("org.slf4j:slf4j-simple:1.7.30")
}

tasks{

    compileJava{
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    compileKotlin{
        kotlinOptions.jvmTarget = "17"
    }

    val pushToServer by registering(Exec::class){
        dependsOn(installDist)
        group = "push"
        commandLine("wsl", "rsync", "-av", "/mnt/c/Users/anton/Desktop/Ordner/Development/SickNetwork/SickBot/build/install/SickBot/", "node1:/home/sickmc/network/bot")
    }

}

application{
    mainClass.set("net.sickmc.sickbot.MainKt")
}
