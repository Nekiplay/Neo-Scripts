plugins {
    id("maven-publish")
    id("idea")
    id("net.neoforged.moddev") version "2.0.141"
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
}

repositories {
    mavenLocal()
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
}

tasks {
    jarJar {
        archiveClassifier.set("")
    }

    jar {
        archiveClassifier.set("thin")
    }

    assemble {
        dependsOn(jarJar)
    }

    // NeoGradle compiles the game, but we don't want to add our common code to the game's code
    val notNeoTask: (Task) -> Boolean = { !it.name.startsWith("neo") && !it.name.startsWith("compileService") }

    withType<ProcessResources>().matching(notNeoTask).configureEach {
        // the properties listed here can be used in the mods.toml
        val properties =
            listOf(
                "mc_versions_neo", "neo_loader_version_range", "mod_version", "mod_id", "mod_name",
                "mod_description", "mod_authors", "mod_license"
            )

        // store a map of the properties so the configuration cache can be used
        val map = mutableMapOf<String, String>()
        properties.forEach { map[it] = rootProject.properties[it].toString() }
        inputs.property("property_map", map)

        filesMatching("META-INF/neoforge.mods.toml") {
            @Suppress("UNCHECKED_CAST")
            expand(inputs.properties["property_map"] as Map<String, String>)
        }
    }
}
