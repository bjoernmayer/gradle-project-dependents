package io.github.bjoernmayer.gradleProjectDependents

import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.DependentsTask
import org.gradle.api.Plugin
import org.gradle.api.Project

public class ProjectDependentsGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.project.extensions.create("projectDependents", ProjectDependentsExtension::class.java)

        target.tasks.register("dependents", DependentsTask::class.java) { task ->
            task.group = "help"
            task.description = "Displays the projects that depend on this project"

            task.excludedConfs.set(extension.excludedConfigurations)

            task.generateStdOutGraph.set(extension.generateStdOutGraph)

            task.generateYamlGraph.set(extension.generateYamlGraph)
            task.outputFile.set(
                target.layout.buildDirectory.file("projectDependents/graph.yaml"),
            )
        }
    }
}
