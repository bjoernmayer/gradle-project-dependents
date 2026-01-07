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

        public val generateStdOutGraph: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

        public val generateYamlGraph: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    }
