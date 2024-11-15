package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer

import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.Connection
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import java.io.File
import java.io.OutputStreamWriter

internal class YamlPrinter(
    override val excludedConfigurations: Set<Configuration>,
    private val outputFile: File,
) : Printer {
    override fun print(projectDependents: ProjectDependents) {
        outputFile.writer().use { writer ->
            projectDependents.write(writer, emptySet(), 0, null)
        }

        outputFile.setLastModified(0)

        println("Yaml Graph written to ${outputFile.absolutePath}")
    }

    private fun ProjectDependents.write(
        writer: OutputStreamWriter,
        alreadyPrintedConnections: Set<Connection>,
        level: Int = 1,
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

        val dependentsToPrint =
            dependents
                .filterNot {
                    it.key in excludedConfigurations
                }

        if (dependentsToPrint.isEmpty()) {
            return
        }

        if (level > 0) {
            writer.write(deepIndent)
        }
        writer.write("dependents:\n")

        dependentsToPrint.entries
            .forEach { (configuration, dependents) ->
                dependents.forEachIndexed { index, projectDependents ->
                    val connection =
                        Connection(
                            configuration = configuration,
                            dependentProjectName = this.name,
                            dependencyProjectName = projectDependents.name,
                        )

                    if (connection in alreadyPrintedConnections) {
                        return@forEachIndexed
                    }

                    projectDependents.write(
                        writer,
                        alreadyPrintedConnections + setOf(connection),
                        level + 1,
                        configuration.name.takeIf { index == 0 },
                    )
                }
            }
    }
}
