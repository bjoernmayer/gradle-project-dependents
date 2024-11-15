package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer

import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents

internal interface Printer {
    val excludedConfigurations: Set<Configuration>

    fun print(projectDependents: ProjectDependents)
}
