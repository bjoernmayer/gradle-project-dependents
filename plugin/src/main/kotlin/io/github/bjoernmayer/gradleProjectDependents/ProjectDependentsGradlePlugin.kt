package io.github.bjoernmayer.gradleProjectDependents

import org.gradle.api.Plugin
import org.gradle.api.Project

public class ProjectDependentsGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val dependentsTaskProvider = target.tasks.register("dependents", DependentsTask::class.java)
    }
}
