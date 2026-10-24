plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    api(project(":pebble-forge-core"))

    compileOnly("org.springframework:spring-web:7.0.7")
    compileOnly("org.springframework.security:spring-security-core:7.0.8")
    compileOnly("org.springframework.security:spring-security-web:7.0.8")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")

    testImplementation("org.springframework:spring-web:7.0.7")
    testImplementation("org.springframework:spring-test:7.0.7")
    testImplementation("org.springframework.security:spring-security-core:7.0.8")
    testImplementation("org.springframework.security:spring-security-web:7.0.8")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.1.0")
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
