plugins {
    id("java")
    id("idea")
    id("fabric-loom") version "1.15-SNAPSHOT" apply false
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "idea")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://maven.notenoughupdates.org/releases/")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://maven.azureaaron.net/releases")
        maven("https://repo1.maven.org/maven2/")
        maven("https://repo.nea.moe/releases")
        maven("https://api.modrinth.com/maven")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        exclusiveContent {
            forRepository {
                maven {
                    name = "Modrinth"
                    url = uri("https://api.modrinth.com/maven")
                }
            }
            filter {
                includeGroup("maven.modrinth")
            }
        }
    }

    java.toolchain.languageVersion = JavaLanguageVersion.of(rootProject.properties["java_version"].toString())

    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.release.set(rootProject.properties["java_version"].toString().toInt())
        }
        withType<GenerateModuleMetadata>().configureEach {
            enabled = false
        }

        jar {
            // put all built jars in the correct directory
            destinationDirectory = rootDir.resolve("build").resolve("libs_${project.name}")

            // add license file to jars
            from(rootDir.resolve("LICENSE.md"))

            // required because apparently some classes are duplicated
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }

    version = properties["mod_version"].toString()
    group = properties["mod_group"].toString()

    base {
        // format artifact names as [mod_id]-[loader]-[mc_version]-[mod_version].jar
        archivesName =
            "${rootProject.properties["mod_id"]}-${project.name}-${rootProject.properties["minecraft_version"]}"
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:26.0.1")
    }
}

tasks.jar {
    enabled = false
}