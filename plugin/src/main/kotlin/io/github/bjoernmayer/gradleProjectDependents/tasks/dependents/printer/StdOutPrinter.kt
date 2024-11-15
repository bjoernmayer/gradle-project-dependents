package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer

import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.Connection
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents

internal class StdOutPrinter(
    override val excludedConfigurations: Set<Configuration>,
) : Printer {
    override fun print(projectDependents: ProjectDependents) {
        projectDependents.print(emptySet(), 0, false, null)
    }

    private fun ProjectDependents.print(
        alreadyPrintedConnections: Set<Connection>,
        level: Int = 1,
        last: Boolean,
        configuration: String?,
    ) {
        val folderIcon =
            if (last) {
                "\\"
            } else {
                "+"
            }
        println("|    ".repeat(level) + folderIcon + "--- ${this.name} ${configuration?.let { "($it)" } ?: ""}")

        dependents
            .filterNot {
                it.key in excludedConfigurations
            }.entries
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

                    projectDependents.print(
                        alreadyPrintedConnections + setOf(connection),
                        level + 1,
                        index == dependents.size - 1,
                        configuration.name,
                    )
                }
            }
    }
}
