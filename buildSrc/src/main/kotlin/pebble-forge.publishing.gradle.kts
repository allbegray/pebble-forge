import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    `maven-publish`
    signing
}

val repositoryUrl = "https://github.com/allbegray/pebble-forge"

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set(project.name)
                description.set(project.provider { project.description ?: "Pebble Forge module" })
                url.set(repositoryUrl)
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("allbegray")
                        name.set("allbegray")
                        url.set("https://github.com/allbegray")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/allbegray/pebble-forge.git")
                    developerConnection.set("scm:git:ssh://github.com/allbegray/pebble-forge.git")
                    url.set(repositoryUrl)
                }
            }
        }
    }
    repositories {
        maven {
            name = "staging"
            url = uri(rootProject.layout.buildDirectory.dir("staging-repo").get().asFile)
        }
    }
}

signing {
    val signingKey = providers.gradleProperty("signingKey").orNull
    val signingPassword = providers.gradleProperty("signingPassword").orNull
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}
