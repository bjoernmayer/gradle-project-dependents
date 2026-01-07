plugins {
    id("io.github.bjoernmayer.gradle-project-dependents")
}

subprojects {
    group = "example-project"
    apply(plugin = "java-library")
    apply(plugin = "io.github.bjoernmayer.gradle-project-dependents")

    extensions.configure<io.github.bjoernmayer.gradleProjectDependents.ProjectDependentsExtension> {
        // Generate all output formats for demonstration
        outputFormats.set(
            setOf(
                io.github.bjoernmayer.gradleProjectDependents.OutputFormat.STDOUT,
                io.github.bjoernmayer.gradleProjectDependents.OutputFormat.YAML,
                io.github.bjoernmayer.gradleProjectDependents.OutputFormat.JSON,
                io.github.bjoernmayer.gradleProjectDependents.OutputFormat.MERMAID,
            ),
        )

        // Exclude test configurations from the graph
        excludedConfigurations.add("testImplementation")
        excludedConfigurations.add("testRuntimeOnly")
    }
}
