plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.2.1"
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
    implementation("tools.jackson.core:jackson-databind:3.2.4")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.2.4")

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website.set("https://github.com/allbegray/pebble-forge")
    vcsUrl.set("https://github.com/allbegray/pebble-forge")
    plugins {
        create("pebbleForgeContracts") {
            id = "io.github.allbegray.pebbleforge.contracts"
            implementationClass = "io.github.allbegray.pebbleforge.gradle.PebbleForgeContractsPlugin"
            displayName = "Pebble Forge contracts"
            description = "Checks Pebble templates against a model schema and component contracts."
            tags.set(listOf("pebble", "templates", "template-engine", "contracts", "static-analysis"))
        }
        create("pebbleForgeDiagnostics") {
            id = "io.github.allbegray.pebbleforge.diagnostics"
            implementationClass = "io.github.allbegray.pebbleforge.gradle.PebbleForgeDiagnosticsPlugin"
            displayName = "Pebble Forge diagnostics"
            description = "Renders Pebble templates and reports source-mapped diagnostics."
            tags.set(listOf("pebble", "templates", "template-engine", "diagnostics", "rendering"))
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
