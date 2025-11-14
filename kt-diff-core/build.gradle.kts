import org.gradle.api.file.DuplicatesStrategy

plugins {
    `java-library`
    id("org.jetbrains.dokka")
    id("me.champeau.jmh") version "0.7.3"
}

dependencies {
    // no additional implementation dependencies for main source set
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.jar {
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
