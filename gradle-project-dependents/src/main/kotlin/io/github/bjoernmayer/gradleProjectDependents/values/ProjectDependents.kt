package io.github.bjoernmayer.gradleProjectDependents.values

internal data class ProjectDependents(
    val name: String,
    val dependents: Map<Configuration, List<ProjectDependents>> = emptyMap(),
)
