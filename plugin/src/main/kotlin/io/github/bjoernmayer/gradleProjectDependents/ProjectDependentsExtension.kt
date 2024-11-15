package io.github.bjoernmayer.gradleProjectDependents

import org.gradle.api.tasks.Input

public open class ProjectDependentsExtension {
    @get:Input
    public val excludedConfigurations: MutableSet<String> = mutableSetOf()

    @get:Input
    public var generateStdOutGraph: Boolean = true

    @get:Input
    public var generateYamlGraph: Boolean = false
}
