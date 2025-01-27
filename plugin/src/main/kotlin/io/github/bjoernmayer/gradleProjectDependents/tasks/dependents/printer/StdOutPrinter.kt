package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer

import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.Connection
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents

internal class StdOutPrinter(
    override val excludedConfigurations: Set<Configuration>,
) : Printer {
    override fun print(projectDependents: ProjectDependents) {
        print(projectDependents.format(emptySet(), 0, false, null))
    }

    private fun ProjectDependents.format(
        alreadyPrintedConnections: Set<Connection>,
        level: Int = 1,
        last: Boolean,
        configuration: String?,
    ): String {
        val folderIcon =
            if (last) {
                "\\"
            } else {
                "+"
            }
        return buildString {
            appendLine("|    ".repeat(level) + folderIcon + "--- ${this@format.name} ${configuration?.let { "($it)" } ?: ""}")
            dependents
                .filterNot {
                    it.key in excludedConfigurations
                }.entries
                .forEach { (configuration, dependents) ->
                    dependents.forEachIndexed { index, projectDependents ->
                        val connection =
                            Connection(
                                configuration = configuration,
                                dependentProjectName = this@format.name,
                                dependencyProjectName = projectDependents.name,
                            )

                        if (connection in alreadyPrintedConnections) {
                            return@forEachIndexed
                        }

                        append(
                            projectDependents.format(
                                alreadyPrintedConnections + setOf(connection),
                                level + 1,
                                index == dependents.size - 1,
                                configuration.name,
                            ),
                        )
                    }
                }
        }
    }
}
