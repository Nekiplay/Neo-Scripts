import org.gradle.kotlin.dsl.flatDir
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.mavenCentral

rootProject.name = "neo-scripts"

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("fabric")