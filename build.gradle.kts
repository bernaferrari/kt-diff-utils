import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("multiplatform") version "1.9.25" apply false
    id("org.jetbrains.dokka") version "1.9.20" apply false
    id("com.diffplug.spotless") version "6.25.0"
}

allprojects {
    group = "com.bernaferrari.difflib"
    version = "4.17.0"

    repositories {
        mavenCentral()
    }
}

spotless {
    format("misc") {
        target("*.md", "**/.gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

subprojects {
    pluginManager.withPlugin("java") {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(8)
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-Xjsr305=strict"
        }
        (kotlinOptions as? KotlinJvmOptions)?.jvmTarget = "1.8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
