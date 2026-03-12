package io.github.bjoernmayer.gradleProjectDependents.values

internal data class Connection(
    val configuration: Configuration,
    val dependentProjectName: String,
    val dependencyProjectName: String,
)
