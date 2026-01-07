package io.github.bjoernmayer.gradleProjectDependents

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

public abstract class ProjectDependentsExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        public val excludedConfigurations: SetProperty<String> = objects.setProperty(String::class.java)

        public val outputFormats: SetProperty<OutputFormat> =
            objects.setProperty(OutputFormat::class.java).convention(setOf(OutputFormat.STDOUT))
    }
