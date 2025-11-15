plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api(project(":kt-diff-core"))
    implementation(project(":kt-diff-jvm-io"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.4.0.202509020913-r")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
