plugins {
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(project(":pebble-forge-core"))
    implementation(project(":pebble-forge-diagnostics"))
    runtimeOnly("ch.qos.logback:logback-classic:1.5.33")
}

application {
    mainClass = "io.github.allbegray.pebbleforge.demo.Demo"
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}
