plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api(project(":kt-diff-core"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
