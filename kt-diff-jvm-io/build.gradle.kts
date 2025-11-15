plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
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
        val jvmMain by getting {
            dependencies {
                api(project(":kt-diff-core", configuration = "jvmRuntimeElements"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
                runtimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
                implementation("org.assertj:assertj-core:3.27.3")
            }
        }
    }
}
