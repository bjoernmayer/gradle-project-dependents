package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents

import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.Printer
import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.StdOutPrinter
import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.YamlPrinter
import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Dependency graph can change without file modifications")
public abstract class DependentsTask : DefaultTask() {
    @get:Input
    internal abstract val excludedConfs: SetProperty<String>

    // Not annotated as @OutputFile since this task is @UntrackedTask
    @get:Internal
    internal val outputFile = project.objects.fileProperty()

    @get:Input
    internal abstract val generateStdOutGraph: Property<Boolean>

    @get:Input
    internal abstract val generateYamlGraph: Property<Boolean>

    @TaskAction
    public fun list() {
        val excludedConfigurations = excludedConfs.get().map { Configuration(it) }.toSet()
        val printers = buildPrinters(excludedConfigurations)

        val dependencyGraph = buildDependencyGraph()
        val projectDependents = dependencyGraph[thisProjectPath] ?: return

        printers.forEach { printer -> printer.print(projectDependents) }
    }

    private fun buildPrinters(excludedConfigurations: Set<Configuration>): Set<Printer> =
        buildSet {
            if (generateStdOutGraph.get()) {
                add(StdOutPrinter(excludedConfigurations))
            }

            if (generateYamlGraph.get()) {
                add(YamlPrinter(excludedConfigurations, outputFile.get().asFile))
            }
        }

    private fun buildDependencyGraph(): Map<String, ProjectDependents> {
        val rootProjectName = project.rootProject.name
        val dependencyGraph: Map<String, ProjectDependents> =
            project.rootProject.allprojects.sortedBy { it.projectPath }.associate {
                val projectPath = it.projectPath

                projectPath to
                    ProjectDependents(
                        projectPath,
                        sortedMapOf(
                            Comparator { o1, o2 -> o1.name.compareTo(o2.name) },
                        ),
                    )
            }

        project.rootProject.allprojects.forEach { proj ->
            val projectDependents = dependencyGraph[proj.projectPath] ?: return@forEach

            proj.configurations.forEach forEachConfiguration@{ configuration ->
                configuration.dependencies.forEach forEachDependency@{ dependency ->
                    if (dependency.group?.startsWith(rootProjectName) != true) {
                        return@forEachDependency
                    }

                    val projectDependencyPath = dependency.group?.replace(".", ":") + ":" + dependency.name

                    val projectDependency = dependencyGraph[projectDependencyPath] ?: return@forEachConfiguration

                    // Add this project to the dependents of the dependency
                    val dependentsMap = projectDependency.dependents as MutableMap<Configuration, MutableList<ProjectDependents>>
                    dependentsMap.compute(Configuration(configuration)) { _, dependents ->
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

    private val thisProjectPath: String
        get() = project.projectPath

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
