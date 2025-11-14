plugins {
    `java-library`
}

dependencies {
    api(project(":kt-diff-core"))
    implementation(project(":kt-diff-jvm-io"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.4.0.202509020913-r")
}
