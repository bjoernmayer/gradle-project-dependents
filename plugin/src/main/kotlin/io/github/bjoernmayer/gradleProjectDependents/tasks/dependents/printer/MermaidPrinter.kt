package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer

import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.model.DependentsGraph
import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import org.gradle.api.logging.Logger
import java.io.File

internal class MermaidPrinter(
    override val excludedConfigurations: Set<Configuration>,
    private val outputFile: File,
) : Printer {
    override fun print(
        projectDependents: ProjectDependents,
        logger: Logger,
    ) {
        outputFile.parentFile?.mkdirs()

        val graph = DependentsGraph.fromProjectDependents(projectDependents, excludedConfigurations)
        val connections = graph.collectConnections()

        val content =
            buildString {
                appendLine("```mermaid")
                appendLine("flowchart BT")
                connections.forEach { appendLine("    $it") }
                appendLine("```")
            }

        outputFile.writeText(content)
        outputFile.setLastModified(0)

        logger.lifecycle("Mermaid Graph written to ${outputFile.absolutePath}")
    }

    private fun DependentsGraph.collectConnections(): Set<String> =
        dependents
            .flatMap { (configuration, dependentsList) ->
                dependentsList.flatMap { dependent ->
                    val fromId = sanitizeId(dependent.name)
                    val toId = sanitizeId(name)
                    val connection = "$fromId[\"${dependent.name}\"] -->|$configuration| $toId[\"$name\"]"

                    setOf(connection) + dependent.collectConnections()
                }
            }.toSet()

    private fun sanitizeId(name: String): String = name.replace(":", "_").replace("-", "_").replace(".", "_")
}
