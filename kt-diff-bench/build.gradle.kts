import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    kotlin("jvm")
    id("me.champeau.jmh") version "0.7.3"
}

dependencies {
    implementation(project(":kt-diff-core", configuration = "jvmRuntimeElements"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xjsr305=strict"
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

tasks.withType<JavaCompile>().configureEach {
    if (name.contains("JmhCompileGeneratedClasses")) {
        options.compilerArgs.add("-Xlint:-options")
    }
}
