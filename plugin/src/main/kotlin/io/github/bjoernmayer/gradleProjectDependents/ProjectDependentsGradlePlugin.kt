package io.github.bjoernmayer.gradleProjectDependents

import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.DependentsTask
import org.gradle.api.Plugin
import org.gradle.api.Project

public class ProjectDependentsGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.project.extensions.create("projectDependents", ProjectDependentsExtension::class.java)

        target.tasks.register("dependents", DependentsTask::class.java) { task ->
            task.group = "help"

            task.excludedConfs.addAll(extension.excludedConfigurations.map { it })

            task.generateStdOutGraph = extension.generateStdOutGraph

            if (extension.generateYamlGraph) {
                task.generateYamlGraph = true
                task.outputFile.set(target.layout.buildDirectory.file("projectDependents/graph.yaml"))
            }

            task.outputs.upToDateWhen { false }
        }
    }
}
