package io.github.bjoernmayer.gradleProjectDependents

import org.gradle.api.Plugin
import org.gradle.api.Project

public class ProjectDependentsGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.project.extensions.create("projectDependents", ProjectDependentsExtension::class.java)

        target.tasks.register("dependents", DependentsTask::class.java) {
            it.group = "help"
            it.excludedConfigurations.addAll(extension.excludedConfigurations)
        }
    }
}
