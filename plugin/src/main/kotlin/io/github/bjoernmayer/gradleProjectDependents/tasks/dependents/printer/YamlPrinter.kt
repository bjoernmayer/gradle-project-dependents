package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer

import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.model.DependentsGraph
import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import org.gradle.api.logging.Logger
import java.io.File
import java.io.OutputStreamWriter

internal class YamlPrinter(
    override val excludedConfigurations: Set<Configuration>,
    private val outputFile: File,
) : Printer {
    override fun print(
        projectDependents: ProjectDependents,
        logger: Logger,
    ) {
        outputFile.parentFile?.mkdirs()

        val graph = DependentsGraph.fromProjectDependents(projectDependents, excludedConfigurations)

        outputFile.writer().use { writer ->
            graph.write(writer, 0, null)
        }

        outputFile.setLastModified(0)

        logger.lifecycle("YAML Graph written to ${outputFile.absolutePath}")
    }

    private fun DependentsGraph.write(
        writer: OutputStreamWriter,
        level: Int,
        configuration: String?,
    ) {
        val indent = "  ".repeat(level)
        val deepIndent = "$indent  "

        configuration?.also {
            writer.write("$indent\"$it\":\n")
        }

        if (level > 0) {
            writer.write("$indent- ")
        }

        writer.write("name: \"$name\"\n")

        if (dependents.isEmpty()) {
            return
        }

        if (level > 0) {
            writer.write(deepIndent)
        }
        writer.write("dependents:\n")

        dependents.entries.forEach { (configuration, dependentsList) ->
            dependentsList.forEachIndexed { index, dependent ->
                dependent.write(
                    writer,
                    level + 1,
                    configuration.takeIf { index == 0 },
                )
            }
        }
    }
}
