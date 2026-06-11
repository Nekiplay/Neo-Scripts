plugins {
    id("maven-publish")
    id("idea")
    id("net.neoforged.moddev") version "2.0.141"
    id("org.jetbrains.kotlin.jvm") version "2.4.0"
}

repositories {
    flatDir {
        dirs("../libs")
    }
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)
kotlin.jvmToolchain(21)

neoForge {
    version = rootProject.properties["neoforge_version"].toString()

    runs {
        val vmArgs = arrayOf("-XX:+UseZGC", "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+AllowEnhancedClassRedefinition", "-Xms500M", "-Xmx2G")
        create("Client") {
            client()
            gameDirectory = rootProject.file("run/client/${rootProject.properties["minecraft_version"]}")
            jvmArguments.addAll(*vmArgs)
        }
        create("Server") {
            server()
            gameDirectory = rootProject.file("run/server/${rootProject.properties["minecraft_version"]}")
            jvmArguments.addAll(*vmArgs)
        }
    }

    mods {
        create(rootProject.properties["mod_id"].toString()) {
            sourceSet(sourceSets.main.get())
        }
    }

    accessTransformers {
        file("src/main/resources/META-INF/accesstransformer.cfg")
    }
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:5.3.0")

    val luaj = "local:luaj-jse:3.0.2"
    implementation(luaj)
    jarJar(luaj)

    jarJar(implementation("io.github.classgraph:classgraph:4.8.184")!!)

    // ImGUI
    val imguiVersion = property("imgui_version") as String
    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-windows:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-linux:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-macos:$imguiVersion")
    jarJar("io.github.spair:imgui-java-binding:$imguiVersion")
    jarJar("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    jarJar("io.github.spair:imgui-java-natives-windows:$imguiVersion")
    jarJar("io.github.spair:imgui-java-natives-linux:$imguiVersion")
    jarJar("io.github.spair:imgui-java-natives-macos:$imguiVersion")

    // Catboost
    val catboostDep = "ai.catboost:catboost-prediction:${property("catboost_version")}"
    jarJar(implementation(catboostDep)!!)

    val catboostTransitive = "ai.catboost:catboost-common:${property("catboost_version")}"
    jarJar(implementation(catboostTransitive)!!)

    jarJar(implementation("ai.djl:api:0.36.0")!!)
    jarJar(implementation("ai.djl:basicdataset:0.36.0")!!)

    jarJar(implementation("ai.djl.pytorch:pytorch-engine:0.36.0")!!)
    jarJar(implementation("ai.djl.pytorch:pytorch-native-cpu:2.7.1")!!)

    // Apache Commons Compress for archive handling
    val commonsCompressVersion = "1.27.1"
    jarJar(implementation("org.apache.commons:commons-compress:$commonsCompressVersion")!!)

    // HWID System
    jarJar(implementation("com.github.oshi:oshi-core:6.9.0")!!)

    implementation(files("../libs/xaerominimap-neoforge-26.1.2-25.3.14.jar"))
}

tasks {
    withType<AbstractArchiveTask>().matching { it.name == "bundleJarJar" }.configureEach {
        archiveClassifier.set("")
    }

    jar {
        archiveClassifier.set("")
    }

    withType<ProcessResources>().configureEach {
        val properties = mapOf(
            "mod_id" to rootProject.properties["mod_id"].toString(),
            "mod_version" to rootProject.properties["mod_version"].toString(),
            "mc_versions_neo" to rootProject.properties["mc_versions_neo"].toString(),
            "neo_loader_version_range" to rootProject.properties["neo_loader_version_range"].toString(),
            "mod_name" to rootProject.properties["mod_name"].toString(),
            "mod_description" to rootProject.properties["mod_description"].toString(),
            "mod_authors" to rootProject.properties["mod_authors"].toString(),
            "mod_license" to rootProject.properties["mod_license"].toString()
        )
        inputs.properties(properties)

        filesMatching("META-INF/neoforge.mods.toml") {
            expand(properties)
        }
    }
}
tasks.build {
    dependsOn(tasks.jarJar)
}
