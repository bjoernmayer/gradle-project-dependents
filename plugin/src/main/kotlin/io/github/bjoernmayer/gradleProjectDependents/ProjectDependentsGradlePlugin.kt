package io.github.bjoernmayer.gradleProjectDependents

import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.DependentsTask
import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import org.gradle.api.Plugin
import org.gradle.api.Project

public class ProjectDependentsGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.project.extensions.create("projectDependents", ProjectDependentsExtension::class.java)

        target.tasks.register("dependents", DependentsTask::class.java) { task ->
            task.group = "help"

            task.excludedConfigurations.addAll(extension.excludedConfigurations.map { Configuration(it) })

            task.generateStdOutGraph = extension.generateStdOutGraph

            if (extension.generateYamlGraph) {
                task.generateYamlGraph = true
                task.outputFile.set(target.layout.buildDirectory.file("projectDependents/graph.yaml"))
            }
        }
    }
}
