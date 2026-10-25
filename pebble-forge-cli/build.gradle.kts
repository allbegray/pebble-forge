plugins {
    application
}

description = "Standalone CLI for rendering Pebble Forge templates with diagnostics."

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(project(":pebble-forge-core"))
    implementation(project(":pebble-forge-diagnostics"))
    implementation("tools.jackson.core:jackson-databind:3.2.4")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.2.4")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.18")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "io.github.allbegray.pebbleforge.cli.PebbleForgeCli"
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
