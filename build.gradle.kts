import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.25" apply false
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
    apply(plugin = "org.jetbrains.kotlin.jvm")

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
            jvmTarget = "1.8"
            freeCompilerArgs = freeCompilerArgs + "-Xjsr305=strict"
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        add("testImplementation", "org.junit.jupiter:junit-jupiter:5.11.4")
        add("testImplementation", "org.assertj:assertj-core:3.27.3")
    }
}
