import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    id("me.champeau.jmh") version "0.7.3"
}

kotlin {
    jvm {
        withJava()
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = freeCompilerArgs + "-Xjsr305=strict"
            }
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:5.11.4")
                implementation("org.assertj:assertj-core:3.27.3")
            }
        }
    }
}

tasks.named<Jar>("jvmJar") {
    manifest {
        attributes["Automatic-Module-Name"] = "com.bernaferrari.difflib.core"
    }
}

jmh {
    includes.set(listOf("DiffBenchmark"))
    duplicateClassesStrategy.set(DuplicatesStrategy.WARN)
    warmupIterations.set(1)
    iterations.set(1)
    fork.set(1)
    warmup.set("1s")
    timeOnIteration.set("1s")
}
