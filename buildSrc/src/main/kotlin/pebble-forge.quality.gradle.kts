import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    id("com.diffplug.spotless")
}

val isRootProject = project == rootProject

spotless {
    format("misc") {
        if (isRootProject) {
            target("*.kts", "*.md", "docs/**/*.md", "buildSrc/**/*.kts")
            targetExclude("build/**", "buildSrc/build/**", ".gradle/**")
        } else {
            target("**/*.java", "**/*.kts", "**/*.md")
            targetExclude("build/**", ".gradle/**")
        }
        trimTrailingWhitespace()
        endWithNewline()
    }
}

pluginManager.withPlugin("java") {
    pluginManager.apply("checkstyle")

    extensions.configure<CheckstyleExtension> {
        toolVersion = "10.26.1"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        maxWarnings = 0
        maxErrors = 0
    }

    tasks.withType<Javadoc>().configureEach {
        val javadocOptions = options as StandardJavadocDocletOptions
        javadocOptions.addStringOption("Xmaxwarns", "10000")
        javadocOptions.addBooleanOption("Werror", true)
    }

    tasks.named("check") {
        dependsOn("spotlessCheck", "checkstyleMain", "checkstyleTest")
    }
}

tasks.matching { it.name == "build" }.configureEach {
    dependsOn("spotlessCheck")
}
