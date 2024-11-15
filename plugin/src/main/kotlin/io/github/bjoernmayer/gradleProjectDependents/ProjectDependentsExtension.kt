package io.github.bjoernmayer.gradleProjectDependents

import org.gradle.api.tasks.Input

public open class ProjectDependentsExtension {
    @Input
    public val excludedConfigurations: MutableSet<String> = mutableSetOf()
}
