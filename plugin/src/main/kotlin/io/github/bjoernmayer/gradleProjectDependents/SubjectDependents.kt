package io.github.bjoernmayer.gradleProjectDependents

public data class SubjectDependents(
    val name: String,
    val dependents: List<SubjectDependents> = emptyList(),
)
