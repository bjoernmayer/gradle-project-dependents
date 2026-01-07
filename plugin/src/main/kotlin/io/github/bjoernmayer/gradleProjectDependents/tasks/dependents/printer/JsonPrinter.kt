package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer

import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.model.DependentsGraph
import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import kotlinx.serialization.json.Json
import org.gradle.api.logging.Logger
import java.io.File

internal class JsonPrinter(
    override val excludedConfigurations: Set<Configuration>,
    private val outputFile: File,
) : Printer {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    override fun print(
        projectDependents: ProjectDependents,
        logger: Logger,
    ) {
        outputFile.parentFile?.mkdirs()

        val graph = DependentsGraph.fromProjectDependents(projectDependents, excludedConfigurations)
        val jsonString = json.encodeToString(DependentsGraph.serializer(), graph)

        outputFile.writeText(jsonString)
        outputFile.setLastModified(0)

        logger.lifecycle("JSON Graph written to ${outputFile.absolutePath}")
    }
}
