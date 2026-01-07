package io.github.bjoernmayer.gradleProjectDependents

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
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

        /**
         * Maximum depth of the dependency tree to display.
         * When not set (null), the full tree is displayed.
         * A value of 1 shows only direct dependents, 2 shows dependents and their dependents, etc.
         */
        public val depth: Property<Int> = objects.property(Int::class.java)
    }
