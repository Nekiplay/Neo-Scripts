val lwjgl_version: String by project

plugins {
    java
    `maven-publish`
    id("fabric-loom") version "1.15-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
    id("com.nekiplay.hypixelcry.annotation-processor")
    id("com.gradleup.shadow") version "9.3.0"
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.lwjgl") {
            useVersion(lwjgl_version)
        }
    }
}

repositories {
    flatDir {
        dirs("libs")
    }
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.notenoughupdates.org/releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-public/") }
    maven { url = uri("https://maven.azureaaron.net/releases") }
    maven { url = uri("https://repo1.maven.org/maven2/") }
    maven { url = uri("https://repo.nea.moe/releases") }
    maven { url = uri("https://api.modrinth.com/maven") }
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

    val luajava_version = "4.1.0"
    val jnigen_version = "2.5.1"

    // 1. Luajava Core
    implementation("party.iroiro.luajava:luajava:$luajava_version")
    include("party.iroiro.luajava:luajava:$luajava_version")

    // 2. Lua 5.5 Implementation
    implementation("party.iroiro.luajava:luajit:$luajava_version")
    include("party.iroiro.luajava:luajit:$luajava_version")

    // 3. The Natives
    val natives = "party.iroiro.luajava:lua55-platform:$luajava_version:natives-desktop"
    implementation(natives)
    include(natives)

    // 4. The Native Loader
    implementation("com.badlogicgames.gdx:gdx-jnigen-loader:$jnigen_version")
    include("com.badlogicgames.gdx:gdx-jnigen-loader:$jnigen_version")

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")

    // HM-API (https://github.com/AzureAaron/hm-api/releases)
    include(modImplementation("net.azureaaron:hm-api:${property("hm_api_version")}")!!)  // HM API (Hypixel Mod API Library)

    // Occlusion Culling
    include(implementation("com.logisticscraft:occlusionculling:${property("occlusionculling_version")}")!!)

    // NEU RepoParser (https://repo.nea.moe/#/releases/moe/nea/neurepoparser)
    include(implementation("moe.nea:neurepoparser:${property("repoparser_version")}")!!)

    // Networth Calculator (https://maven.azureaaron.net/#/releases/net/azureaaron/networth-calculator)
    include(implementation("net.azureaaron:networth-calculator:${property("networth_calculator_version")}")!!)

    // JGit used pull data from the NEU item repo
    include(implementation("org.eclipse.jgit:org.eclipse.jgit:${property("jgit_version")}")!!)

    // Legacy Item DFU (https://maven.azureaaron.net/releases/net/azureaaron/legacy-item-dfu)
    include(implementation("net.azureaaron:legacy-item-dfu:${property("legacy_item_dfu_version")}")!!)

    // ImGUI
    val imguiVersion = property("imgui_version") as String
    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-windows:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-linux:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-macos:$imguiVersion")
    include("io.github.spair:imgui-java-binding:$imguiVersion")
    include("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    include("io.github.spair:imgui-java-natives-windows:$imguiVersion")
    include("io.github.spair:imgui-java-natives-linux:$imguiVersion")
    include("io.github.spair:imgui-java-natives-macos:$imguiVersion")

    // Catboost
    val catboostDep = "ai.catboost:catboost-prediction:${property("catboost_version")}"
    include(implementation(catboostDep)!!)

    val catboostTransitive = "ai.catboost:catboost-common:${property("catboost_version")}"
    include(implementation(catboostTransitive)!!)

    modRuntimeOnly(files("libs/firmament.jar"))

    include(implementation("ai.djl:api:0.15.0")!!)
    include(implementation("ai.djl:basicdataset:0.15.0")!!)

    include(implementation("ai.djl.pytorch:pytorch-engine:0.15.0")!!)
    include(implementation("ai.djl.pytorch:pytorch-native-auto:1.9.1")!!)
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
