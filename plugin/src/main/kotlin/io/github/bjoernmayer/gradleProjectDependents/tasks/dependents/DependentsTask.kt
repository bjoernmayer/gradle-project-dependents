package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents

import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.Printer
import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.StdOutPrinter
import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.YamlPrinter
import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public abstract class DependentsTask : DefaultTask() {
    @get:Input
    internal val excludedConfs: MutableSet<String> = mutableSetOf()

    @get:OutputFile
    @get:Optional
    internal val outputFile = project.objects.fileProperty()

    @get:Input
    internal var generateStdOutGraph: Boolean = true

    @get:Input
    internal var generateYamlGraph: Boolean = false

    private lateinit var printers: Set<Printer>
    private lateinit var excludedConfigurations: Set<Configuration>

    private val thisProjectName = project.projectPath
    private val rootProjectName = project.rootProject.name
    private val dependencyGraph: Map<String, ProjectDependents> =
        project.rootProject.allprojects.sortedBy { it.projectPath }.associate {
            val projectPath = it.projectPath

            projectPath to
                ProjectDependents(
                    projectPath,
                    sortedMapOf(
                        object : Comparator<Configuration> {
                            override fun compare(
                                o1: Configuration,
                                o2: Configuration,
                            ): Int = o1.name.compareTo(o2.name)
                        },
                    ),
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
                    projectDependency.dependents as MutableMap<Configuration, List<ProjectDependents>>
                    projectDependency.dependents.compute(Configuration(configuration)) { _, dependents: List<ProjectDependents>? ->
                        if (dependents == null) {
                            return@compute mutableListOf(projectDependents)
                        }

                        dependents as MutableList<ProjectDependents>

                        if (projectDependents !in dependents) {
                            dependents.add(projectDependents)
                            dependents.sortBy { it.name }
                        }

                        dependents
                    }
                }
            }
        }
    }

    @TaskAction
    public fun list() {
        excludedConfigurations = excludedConfs.map { Configuration(it) }.toSet()
        printers =
            buildSet {
                if (generateStdOutGraph) {
                    this.add(StdOutPrinter(excludedConfigurations))
                }

                if (generateYamlGraph) {
                    this.add(YamlPrinter(excludedConfigurations, outputFile.get().asFile))
                }
            }

        val projectDependents = dependencyGraph[thisProjectName] ?: return

        printers.forEach { printer -> printer.print(projectDependents) }
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
