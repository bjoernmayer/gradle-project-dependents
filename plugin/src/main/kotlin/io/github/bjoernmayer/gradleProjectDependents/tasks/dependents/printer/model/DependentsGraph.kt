package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.model

import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import kotlinx.serialization.Serializable

@Serializable
internal data class DependentsGraph(
    val name: String,
    val dependents: Map<String, List<DependentsGraph>> = emptyMap(),
) {
    companion object {
        fun fromProjectDependents(
            projectDependents: ProjectDependents,
            excludedConfigurations: Set<Configuration>,
        ): DependentsGraph = projectDependents.toGraph(excludedConfigurations, emptySet())

        private fun ProjectDependents.toGraph(
            excludedConfigurations: Set<Configuration>,
            visitedPaths: Set<Pair<String, String>>,
        ): DependentsGraph {
            val filteredDependents =
                dependents
                    .filterNot { it.key in excludedConfigurations }
                    .mapKeys { it.key.name }
                    .mapValues { (_, dependentsList) ->
                        dependentsList.mapNotNull { dependent ->
                            val path = this.name to dependent.name

                            if (path in visitedPaths) {
                                return@mapNotNull null
                            }

                            dependent.toGraph(excludedConfigurations, visitedPaths + path)
                        }
                    }.filterValues { it.isNotEmpty() }

            return DependentsGraph(
                name = name,
                dependents = filteredDependents,
            )
        }
    }
}
