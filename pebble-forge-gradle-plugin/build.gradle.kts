plugins {
    `java-gradle-plugin`
}

description = "Gradle plugin for Pebble Forge template contract checks."

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(project(":pebble-forge-contracts"))
    implementation(project(":pebble-forge-core"))
    implementation(project(":pebble-forge-diagnostics"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.22.3")

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    plugins {
        create("pebbleForgeContracts") {
            id = "io.github.allbegray.pebbleforge.contracts"
            implementationClass = "io.github.allbegray.pebbleforge.gradle.PebbleForgeContractsPlugin"
        }
        create("pebbleForgeDiagnostics") {
            id = "io.github.allbegray.pebbleforge.diagnostics"
            implementationClass = "io.github.allbegray.pebbleforge.gradle.PebbleForgeDiagnosticsPlugin"
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
