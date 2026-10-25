import org.gradle.api.tasks.Sync

plugins {
    base
    id("pebble-forge.quality")
}

allprojects {
    group = "io.github.allbegray.pebbleforge"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "pebble-forge.quality")
}

val docsModules = listOf(
    Triple(
        "pebble-forge-core",
        "Core",
        "Components, sandbox, and language extensions."
    ),
    Triple(
        "pebble-forge-contracts",
        "Contracts",
        "Build-time template contract checking."
    ),
    Triple(
        "pebble-forge-diagnostics",
        "Diagnostics",
        "Source-frame diagnostics and include traces."
    ),
    Triple(
        "pebble-forge-natural-html",
        "Natural HTML",
        "Browser-openable Natural HTML templates."
    ),
    Triple(
        "pebble-forge-gradle-plugin",
        "Gradle plugin",
        "Gradle tasks for contract checks and template rendering."
    ),
    Triple(
        "pebble-forge-spring",
        "Spring",
        "Spring Security template functions and sandbox helpers."
    ),
    Triple(
        "pebble-forge-spring-boot-4-starter",
        "Spring Boot starter",
        "Spring Boot 4 auto-configuration."
    )
)

val javadocAll = tasks.register<Sync>("javadocAll") {
    group = "documentation"
    description = "Builds the aggregate Javadoc site in build/docs-site."

    dependsOn(docsModules.map { ":${it.first}:javadoc" })
    into(layout.buildDirectory.dir("docs-site"))
    docsModules.forEach { (name, _, _) ->
        from(project(":$name").layout.buildDirectory.dir("docs/javadoc")) {
            into(name)
        }
    }

    doLast {
        val links = docsModules.joinToString("\n") { (name, title, description) ->
            """    <li><a href="$name/index.html">$title</a>. $description</li>"""
        }
        val html = listOf(
            "<!doctype html>",
            "<html lang=\"en\">",
            "<head>",
            "  <meta charset=\"utf-8\">",
            "  <title>Pebble Forge API</title>",
            "  <style>",
            "    body { font-family: system-ui, sans-serif; line-height: 1.5; margin: 2rem auto; max-width: 48rem; }",
            "    h1 { margin-bottom: 0.25rem; }",
            "    li { margin: 0.35rem 0; }",
            "  </style>",
            "</head>",
            "<body>",
            "  <h1>Pebble Forge API</h1>",
            "  <p>Javadoc for the Pebble Forge modules. Each module has its own index.</p>",
            "  <ul>",
            links,
            "  </ul>",
            "</body>",
            "</html>",
            ""
        ).joinToString("\n")
        layout.buildDirectory.file("docs-site/index.html").get().asFile.writeText(html)
    }
}
