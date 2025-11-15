plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api(project(":kt-diff-core", configuration = "jvmRuntimeElements"))
    implementation(project(":kt-diff-jvm-io", configuration = "jvmRuntimeElements"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.4.0.202509020913-r")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.test {
    useJUnitPlatform()
}
