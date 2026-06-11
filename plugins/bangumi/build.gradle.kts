plugins {
    `java-library`
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

dependencies {
    implementation(project(":plugin-spi"))
}
