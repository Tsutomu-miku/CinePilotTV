plugins {
    `java-library`
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

val runProtocolCoreTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs CinePilot protocol core main-method tests."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("tv.cinepilot.core.protocol.ProtocolCoreTest")
}

val runTvWorkflowTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs CinePilot TV workflow main-method tests."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("tv.cinepilot.core.tv.TvWorkflowTest")
}

val runHttpTransportIntegrationTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs CinePilot HTTP transport integration tests."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("tv.cinepilot.core.protocol.HttpTransportIntegrationTest")
}

tasks.named("test") {
    dependsOn(runProtocolCoreTest, runTvWorkflowTest, runHttpTransportIntegrationTest)
}
