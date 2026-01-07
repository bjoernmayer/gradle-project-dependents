package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer

import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.model.DependentsGraph
import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import org.gradle.api.logging.Logger

internal class StdOutPrinter(
    override val excludedConfigurations: Set<Configuration>,
    override val maxDepth: Int?,
) : Printer {
    override fun print(
        projectDependents: ProjectDependents,
        logger: Logger,
    ) {
        val graph = DependentsGraph.fromProjectDependents(projectDependents, excludedConfigurations, maxDepth)
        print(graph.format(0, false, null))
    }

    private fun DependentsGraph.format(
        level: Int,
        last: Boolean,
        configuration: String?,
    ): String {
        val folderIcon = if (last) "\\" else "+"
        return buildString {
            appendLine("|    ".repeat(level) + folderIcon + "--- $name ${configuration?.let { "($it)" } ?: ""}")
            dependents.entries.forEach { (configuration, dependentsList) ->
                dependentsList.forEachIndexed { index, dependent ->
                    append(
                        dependent.format(
                            level + 1,
                            index == dependentsList.size - 1,
                            configuration,
                        ),
                    )
                }
            }
        }
    }
}
