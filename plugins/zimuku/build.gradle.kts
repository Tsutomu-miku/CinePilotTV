plugins {
    `java-library`
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

dependencies {
    implementation(project(":plugin-spi"))
    // ZIP/RAR/7z 解压
    implementation("org.apache.commons:commons-compress:1.26.2")

    testImplementation("junit:junit:4.13.2")
}
