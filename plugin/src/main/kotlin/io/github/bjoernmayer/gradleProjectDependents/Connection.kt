package io.github.bjoernmayer.gradleProjectDependents

public data class Connection(
    val configuration: String,
    val dependentProjectName: String,
    val dependencyProjectName: String,
)
