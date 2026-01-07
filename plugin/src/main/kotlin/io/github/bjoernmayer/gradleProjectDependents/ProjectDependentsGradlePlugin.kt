package io.github.bjoernmayer.gradleProjectDependents

import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.ConfigurationInfo
import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.DependentsTask
import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.ProjectInfo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

public class ProjectDependentsGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.project.extensions.create("projectDependents", ProjectDependentsExtension::class.java)

        target.tasks.register("dependents", DependentsTask::class.java) { task ->
            task.group = "help"
            task.description = "Displays the projects that depend on this project"

            task.excludedConfs.set(extension.excludedConfigurations)
            task.outputFormats.set(extension.outputFormats)

            task.thisProjectPath.set(target.projectPath)
            task.rootProjectName.set(target.rootProject.name)

            val rootProjectName = target.rootProject.name
            target.rootProject.allprojects.forEach { proj ->
                task.allProjects.add(proj.provider { proj.toProjectInfo(rootProjectName) })
            }

            task.yamlOutputFile.set(
                target.layout.buildDirectory
                    .file("projectDependents/graph.yaml")
                    .map { it.asFile },
            )
            task.jsonOutputFile.set(
                target.layout.buildDirectory
                    .file("projectDependents/graph.json")
                    .map { it.asFile },
            )
            task.mermaidOutputFile.set(
                target.layout.buildDirectory
                    .file("projectDependents/graph.md")
                    .map { it.asFile },
            )
        }
    }

    private fun Project.toProjectInfo(rootProjectName: String): ProjectInfo =
        ProjectInfo(
            path = projectPath,
            configurations = configurations.map { it.toConfigurationInfo(rootProjectName) },
        )

    private fun Configuration.toConfigurationInfo(rootProjectName: String): ConfigurationInfo =
        ConfigurationInfo(
            name = name,
            projectDependencies =
                dependencies
                    .filter { it.group?.startsWith(rootProjectName) == true }
                    .map { "${it.group?.replace(".", ":")}:${it.name}" },
        )

    private val Project.projectPath: String
        get() = if (this == rootProject) name else "${group.toString().replace(".", ":")}:$name"
}
