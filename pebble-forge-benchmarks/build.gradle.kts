plugins {
    application
}

description = "JMH benchmarks for Pebble and Pebble Forge rendering."

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(project(":pebble-forge-core"))
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "org.openjdk.jmh.Main"
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register<JavaExec>("benchmark") {
    group = "verification"
    description = "Runs the short JMH benchmark suite. This task is manual and is not part of check."

    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "org.openjdk.jmh.Main"

    val jmhArgs = providers.gradleProperty("jmhArgs").orNull
    args(
        jmhArgs?.split(Regex("\\s+"))?.filter(String::isNotBlank)
            ?: listOf("-f", "1", "-wi", "2", "-i", "3", "-w", "1s", "-r", "1s")
    )
}
