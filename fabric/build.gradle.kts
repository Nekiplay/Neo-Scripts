val lwjgl_version: String by project

plugins {
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
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
        dirs("../libs")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

val shadowModImpl by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:${rootProject.properties["minecraft_version"]}")

    val luaj = "local:luaj-jse:3.0.2"
    implementation(luaj)
    include(luaj)

    implementation("net.fabricmc:fabric-loader:${rootProject.properties["fabric_loader_version"]}")
    implementation("net.fabricmc.fabric-api:fabric-api:${rootProject.properties["fabric_api_version"]}")
    implementation("net.fabricmc:fabric-language-kotlin:${rootProject.properties["fabric_kotlin_version"]}")

    // HM-API (https://github.com/AzureAaron/hm-api/releases)
    val hmAPi = "local:hm-api:1.0.3+26.1"
    implementation(hmAPi)
    include(hmAPi)

    // NEU RepoParser (https://repo.nea.moe/#/releases/moe/nea/neurepoparser)
    include(implementation("moe.nea:neurepoparser:${rootProject.properties["repoparser_version"]}")!!)

    // Networth Calculator (https://maven.azureaaron.net/#/releases/net/azureaaron/networth-calculator)
    include(implementation("net.azureaaron:networth-calculator:${rootProject.properties["networth_calculator_version"]}")!!)

    // JGit used pull data from the NEU item repo
    include(implementation("org.eclipse.jgit:org.eclipse.jgit:${rootProject.properties["jgit_version"]}")!!)

    // Legacy Item DFU (https://maven.azureaaron.net/releases/net/azureaaron/legacy-item-dfu)
    include(implementation("net.azureaaron:legacy-item-dfu:${rootProject.properties["legacy_item_dfu_version"]}")!!)

    compileOnly(files("../libs/firmament.jar"))

    include(implementation("io.github.classgraph:classgraph:4.8.184")!!)

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

    include(implementation("ai.djl:api:0.36.0")!!)
    include(implementation("ai.djl:basicdataset:0.36.0")!!)

    include(implementation("ai.djl.pytorch:pytorch-engine:0.36.0")!!)
    include(implementation("ai.djl.pytorch:pytorch-native-cpu:2.7.1")!!)

    // Apache Commons Compress for archive handling
    val commonsCompressVersion = "1.27.1"
    include(implementation("org.apache.commons:commons-compress:$commonsCompressVersion")!!)

    // HWID System
    include(implementation("com.github.oshi:oshi-core:6.9.0")!!)

    implementation(files("../libs/xaerominimap-fabric-26.1.2-25.3.14.jar"))
}

tasks {
    shadowJar {
        from(sourceSets.main.get().output)
        configurations = listOf(shadowModImpl)
        archiveClassifier.set("shadow")
        mergeServiceFiles()
    }

    jar {
        dependsOn(shadowJar)
    }

    processResources {
        // the properties listed here can be used in the fabric.mod.json
        val properties =
            listOf(
                "mc_versions_fabric", "mod_version", "mod_id", "mod_name",
                "mod_description", "mod_authors", "mod_license"
            )

        val map = mutableMapOf<String, String>()
        properties.forEach { map[it] = rootProject.properties[it].toString() }
        inputs.property("property_map", map)

        filesMatching("fabric.mod.json") {
            @Suppress("UNCHECKED_CAST")
            expand(inputs.properties["property_map"] as Map<String, String>)
        }
    }

    named("compileTestJava").configure {
        enabled = false
    }

    named("test").configure {
        enabled = false
    }
}

loom {
    clientOnlyMinecraftJar()
    accessWidenerPath = file("src/main/resources/neoscripts.classtweaker")
    mixin.useLegacyMixinAp.set(false)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "25"
    targetCompatibility = "25"
}
