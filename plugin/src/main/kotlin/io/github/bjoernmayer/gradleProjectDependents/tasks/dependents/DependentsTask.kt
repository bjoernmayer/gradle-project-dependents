package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents

import io.github.bjoernmayer.gradleProjectDependents.OutputFormat
import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.JsonPrinter
import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.MermaidPrinter
import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.Printer
import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.StdOutPrinter
import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.YamlPrinter
import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.Serializable

/**
 * Serializable data class to hold project dependency information captured at configuration time.
 */
public data class ProjectInfo(
    @get:Input
    val path: String,
    @get:Nested
    val configurations: List<ConfigurationInfo>,
) : Serializable {
    public companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Serializable data class to hold configuration dependency information.
 */
public data class ConfigurationInfo(
    @get:Input
    val name: String,
    @get:Input
    val projectDependencies: List<String>,
) : Serializable {
    public companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@UntrackedTask(because = "Dependency graph can change without file modifications")
public abstract class DependentsTask : DefaultTask() {
    @get:Input
    internal abstract val excludedConfs: SetProperty<String>

    @get:Input
    internal abstract val outputFormats: SetProperty<OutputFormat>

    @get:Input
    internal abstract val thisProjectPath: Property<String>

    @get:Input
    internal abstract val rootProjectName: Property<String>

    @get:Nested
    internal abstract val allProjects: SetProperty<ProjectInfo>

    // Not annotated as @OutputFile since this task is @UntrackedTask
    @get:Internal
    internal abstract val yamlOutputFile: Property<java.io.File>

    @get:Internal
    internal abstract val jsonOutputFile: Property<java.io.File>

    @get:Internal
    internal abstract val mermaidOutputFile: Property<java.io.File>

    @TaskAction
    public fun list() {
        val excludedConfigurations = excludedConfs.get().map { Configuration(it) }.toSet()
        val printers = buildPrinters(excludedConfigurations)

        val dependencyGraph = buildDependencyGraph()
        val projectDependents = dependencyGraph[thisProjectPath.get()] ?: return

        printers.forEach { printer -> printer.print(projectDependents, logger) }
    }

    private fun buildPrinters(excludedConfigurations: Set<Configuration>): List<Printer> =
        outputFormats
            .get()
            .map { format ->
                when (format) {
                    OutputFormat.STDOUT -> StdOutPrinter(excludedConfigurations)
                    OutputFormat.YAML -> YamlPrinter(excludedConfigurations, yamlOutputFile.get())
                    OutputFormat.JSON -> JsonPrinter(excludedConfigurations, jsonOutputFile.get())
                    OutputFormat.MERMAID -> MermaidPrinter(excludedConfigurations, mermaidOutputFile.get())
                }
            }.sortedBy { it !is StdOutPrinter } // STDOUT first

    private fun buildDependencyGraph(): Map<String, ProjectDependents> {
        val rootName = rootProjectName.get()
        val projects = allProjects.get()

        val dependencyGraph: Map<String, ProjectDependents> =
            projects.sortedBy { it.path }.associate { projectInfo ->
                projectInfo.path to
                    ProjectDependents(
                        projectInfo.path,
                        sortedMapOf(
                            Comparator { o1, o2 -> o1.name.compareTo(o2.name) },
                        ),
                    )
            }

        projects.forEach { projectInfo ->
            val projectDependents = dependencyGraph[projectInfo.path] ?: return@forEach

            projectInfo.configurations.forEach forEachConfiguration@{ configInfo ->
                configInfo.projectDependencies.forEach forEachDependency@{ dependencyPath ->
                    if (!dependencyPath.startsWith(rootName)) {
                        return@forEachDependency
                    }

                    val projectDependency = dependencyGraph[dependencyPath] ?: return@forEachConfiguration

                    // Add this project to the dependents of the dependency
                    @Suppress("UNCHECKED_CAST")
                    val dependentsMap = projectDependency.dependents as MutableMap<Configuration, MutableList<ProjectDependents>>
                    dependentsMap.compute(Configuration(configInfo.name)) { _, dependents ->
                        val list = dependents ?: mutableListOf()
                        if (projectDependents !in list) {
                            list.add(projectDependents)
                            list.sortBy { it.name }
                        }
                        list
                    }
                }
            }
        }

        return dependencyGraph
    }
}
