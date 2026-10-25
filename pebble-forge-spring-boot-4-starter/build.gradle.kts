plugins {
    id("pebble-forge.publishing")
}

description = "Spring Boot 4 auto-configuration for Pebble Forge."

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    api(project(":pebble-forge-spring"))
    api(project(":pebble-forge-diagnostics"))
    api("io.pebbletemplates:pebble-spring-boot-starter:4.1.2")
    api("org.springframework.boot:spring-boot-autoconfigure:4.0.6")

    compileOnly("org.springframework.boot:spring-boot-webmvc:4.0.6")
    compileOnly("org.springframework:spring-context:7.0.7")
    compileOnly("org.springframework:spring-web:7.0.7")
    compileOnly("org.springframework:spring-webmvc:7.0.7")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
    compileOnly("org.springframework.security:spring-security-core:7.0.8")
    compileOnly("org.springframework.security:spring-security-web:7.0.8")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:4.0.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.6")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc:4.0.6")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test:4.0.6")
    testImplementation("org.springframework.security:spring-security-core:7.0.8")
    testImplementation("org.springframework.security:spring-security-web:7.0.8")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
