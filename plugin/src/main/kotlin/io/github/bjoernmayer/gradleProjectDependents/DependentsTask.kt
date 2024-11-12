package io.github.bjoernmayer.gradleProjectDependents

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

public open class DependentsTask : DefaultTask() {
    override fun getGroup(): String = "help"
    // TODO: Input for configurations that should be excluded

    private val thisProjectName = project.projectPath
    private val rootProjectName = project.rootProject.name
    private val subjectDependents: Map<String, SubjectDependents> =
        project.rootProject.allprojects.associate {
            val projectPath = it.projectPath

            projectPath to
            SubjectDependents(
                projectPath,
                mutableListOf()
            )
        }

    init {
        project.rootProject.allprojects.forEach { subproject ->
            val projectPath = subproject.projectPath

            val subProjectSubjectDependents = subjectDependents[projectPath] ?: return@forEach

            subproject.configurations.forEach { configuration ->
                configuration.dependencies.forEach forEachDependency@ { dependency ->
                    if (dependency.group?.startsWith(rootProjectName) != true) {
                        return@forEachDependency
                    }

                    val dependencyPath = dependency.group?.replace(".", ":") + ":" + dependency.name

                    val subjectDependents = subjectDependents[dependencyPath] ?: return@forEach

                    if (subProjectSubjectDependents !in subjectDependents.dependents) {
                        subjectDependents.dependents as MutableList<SubjectDependents>
                        subjectDependents.dependents.add(subProjectSubjectDependents)
                    }
                }
            }
        }
    }

    @TaskAction
    public fun list() {
        val subjectDependents = subjectDependents[thisProjectName] ?: return

        logger.lifecycle("+--- project ${subjectDependents.name}")
        subjectDependents.dependents.forEach {
            it.print(listOf(subjectDependents), 1, false)
        }
    }

    private fun SubjectDependents.print(parents: List<SubjectDependents>, level: Int = 1, last: Boolean) {
        val folderIcon = if (last) {
            "\\"
        } else {
            "+"
        }
        logger.lifecycle("|    ".repeat(level) + folderIcon+ "--- project ${this.name}")

        if (this !in parents) {
            dependents.forEachIndexed { index, subjectDependents ->
                subjectDependents.print(parents + listOf(this), level + 1, index == dependents.size - 1)
            }
        }
    }

    private companion object {
        val Project.projectPath: String
            get() = if (this == this.rootProject) {
                name
            } else {
                group.toString().replace(".", ":") + ":" + name
            }
    }
}
