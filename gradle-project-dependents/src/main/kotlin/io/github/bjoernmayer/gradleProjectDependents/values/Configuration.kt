package io.github.bjoernmayer.gradleProjectDependents.values

import org.gradle.api.artifacts.Configuration

@JvmInline
internal value class Configuration(
    val name: String,
) {
    constructor(configuration: Configuration) : this(configuration.name)
}
