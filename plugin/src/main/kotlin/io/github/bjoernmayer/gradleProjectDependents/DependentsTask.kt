package io.github.bjoernmayer.gradleProjectDependents

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

public abstract class DependentsTask : DefaultTask() {
    @Input
    public val excludedConfigurations: MutableSet<String> = mutableSetOf()

    private val thisProjectName = project.projectPath
    private val rootProjectName = project.rootProject.name
    private val dependencyGraph: Map<String, ProjectDependents> =
        project.rootProject.allprojects.associate {
            val projectPath = it.projectPath

            projectPath to
                ProjectDependents(
                    projectPath,
                    mutableMapOf(),
                )
        }

    init {
        project.rootProject.allprojects.forEach { project ->
            val projectDependents = dependencyGraph[project.projectPath] ?: return@forEach

            project.configurations.forEach forEachConfiguration@{ configuration ->
                configuration.dependencies.forEach forEachDependency@{ dependency ->
                    if (dependency.group?.startsWith(rootProjectName) != true) {
                        return@forEachDependency
                    }

                    val projectDependencyPath = dependency.group?.replace(".", ":") + ":" + dependency.name

                    val projectDependency = dependencyGraph[projectDependencyPath] ?: return@forEachConfiguration

                    // Add this project to the dependents of the dependency
                    projectDependency.dependents as MutableMap<String, List<ProjectDependents>>
                    projectDependency.dependents.compute(configuration.name) { _, dependents: List<ProjectDependents>? ->
                        if (dependents == null) {
                            return@compute mutableListOf(projectDependents)
                        }

                        dependents as MutableList<ProjectDependents>

                        if (projectDependents !in dependents) {
                            dependents.add(projectDependents)
                        }

                        dependents
                    }
                }
            }
        }
    }

    @TaskAction
    public fun list() {
        val projectDependents = dependencyGraph[thisProjectName] ?: return

        projectDependents.print(emptyList(), 0, false, null)
    }

    private fun ProjectDependents.print(
        parents: List<ProjectDependents>,
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
        logger.lifecycle("|    ".repeat(level) + folderIcon + "--- ${this.name} ${configuration?.let { "($it)" } ?: ""}")

        if (this !in parents) {
            dependents
                .filterNot {
                    it.key in excludedConfigurations
                }.entries
                .forEachIndexed { index, (configuration, dependents) ->
                    dependents.forEach { projectDependents ->
                        projectDependents.print(parents + listOf(projectDependents), level + 1, index == dependents.size - 1, configuration)
                    }
                }
        }
    }

    private companion object {
        val Project.projectPath: String
            get() =
                if (this == this.rootProject) {
                    name
                } else {
                    group.toString().replace(".", ":") + ":" + name
                }
    }
}
