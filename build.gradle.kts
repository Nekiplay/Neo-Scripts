import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("fabric-loom") version "1.13-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
    id("com.nekiplay.hypixelcry.annotation-processor")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    flatDir {
        dirs("libs")
    }
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.notenoughupdates.org/releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-public/") }
    maven { url = uri("https://maven.azureaaron.net/releases") }
    maven { url = uri("https://repo1.maven.org/maven2/") }
    maven { url = uri("https://repo.nea.moe/releases") }
    maven { url = uri("https://maven.azureaaron.net/releases") }
    maven {
        url = uri("https://maven.azureaaron.net/releases")

        content {
            includeGroup("net.azureaaron")
        }
    }

    maven {
        url = uri("https://maven.azureaaron.net/snapshots")

        content {
            includeGroup("net.azureaaron")
        }
    }

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

base {
    archivesName.set(project.property("archives_base_name") as String)
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

val shadowModImpl by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    implementation("org.luaj:luaj-jse:3.0.1")
    include("org.luaj:luaj-jse:3.0.1")
    shadowModImpl("org.luaj:luaj-jse:3.0.1")

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")

    // HM-API (https://github.com/AzureAaron/hm-api/releases)
    include(modImplementation(files("libs/hm-api-1.0.1+1.21.2.jar"))!!)
    //include(modImplementation("net.azureaaron:hm-api:${property("hm_api_version")}")!!)  // HM API (Hypixel Mod API Library)

    // Occlusion Culling
    include(implementation("com.logisticscraft:occlusionculling:${property("occlusionculling_version")}")!!)

    // NEU RepoParser (https://repo.nea.moe/#/releases/moe/nea/neurepoparser)
    include(implementation(files("libs/neurepoparser-1.8.0.jar"))!!)

    // Networth Calculator (https://maven.azureaaron.net/#/releases/net/azureaaron/networth-calculator)
    include(implementation(files("libs/networth-calculator-1.0.5.jar"))!!)

    // JGit used pull data from the NEU item repo
    include(implementation("org.eclipse.jgit:org.eclipse.jgit:${property("jgit_version")}")!!)

    // Legacy Item DFU (https://maven.azureaaron.net/releases/net/azureaaron/legacy-item-dfu)
    include(implementation(files("libs/legacy-item-dfu-1.0.3+1.21.5.jar"))!!)

    // ImGUI
    val imguiVersion = property("imgui_version") as String

    // Основные зависимости ImGUI
    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")

    // Нативные библиотеки для всех платформ
    implementation("io.github.spair:imgui-java-natives-windows:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-linux:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-macos:$imguiVersion")

    // Включите все в финальный JAR
    include("io.github.spair:imgui-java-binding:$imguiVersion")
    include("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    include("io.github.spair:imgui-java-natives-windows:$imguiVersion")
    include("io.github.spair:imgui-java-natives-linux:$imguiVersion")
    include("io.github.spair:imgui-java-natives-macos:$imguiVersion")
}

tasks {
    shadowJar {
        from(sourceSets.main.get().output)
        configurations = listOf(shadowModImpl)
        archiveClassifier.set("shadow")
        mergeServiceFiles()
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
    }
}

loom {
    clientOnlyMinecraftJar()
    accessWidenerPath.set(file("src/main/resources/hypixelcry.accesswidener"))
    mixin.useLegacyMixinAp.set(false)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "21"
    targetCompatibility = "21"
}