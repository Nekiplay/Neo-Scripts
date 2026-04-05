plugins {
    `java-gradle-plugin`
    `java-library`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val asmVersion = "9.7"

dependencies {
    implementation("org.ow2.asm:asm:$asmVersion")
    implementation("org.ow2.asm:asm-tree:$asmVersion")
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")
    implementation("org.jetbrains:annotations:24.0.1")
}

gradlePlugin {
    plugins {
        create("annotationProcessor") {
            id = "com.nekiplay.neoscripts.annotation-processor"
            implementationClass = "com.nekiplay.neoscripts.Processor"
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}