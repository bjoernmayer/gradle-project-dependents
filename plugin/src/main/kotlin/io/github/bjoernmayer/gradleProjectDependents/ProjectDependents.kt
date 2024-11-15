package io.github.bjoernmayer.gradleProjectDependents

public data class ProjectDependents(
    val name: String,
    val dependents: Map<String, List<ProjectDependents>> = emptyMap(),
)
