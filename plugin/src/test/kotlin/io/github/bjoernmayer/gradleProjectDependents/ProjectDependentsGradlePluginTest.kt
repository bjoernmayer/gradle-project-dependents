package io.github.bjoernmayer.gradleProjectDependents

import io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.DependentsTask
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProjectDependentsGradlePluginTest {
    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    fun `should register the dependents task`() {
        project.plugins.apply("io.github.bjoernmayer.gradle-project-dependents")

        val task = project.tasks.findByName("dependents")

        assertThat(task).isNotNull
        assertThat(task).isInstanceOf(DependentsTask::class.java)
    }

    @Test
    fun `should register extension`() {
        project.plugins.apply("io.github.bjoernmayer.gradle-project-dependents")

        val extension = project.extensions.findByName("projectDependents")

        assertThat(extension).isNotNull
        assertThat(extension).isInstanceOf(ProjectDependentsExtension::class.java)
    }

    @Test
    fun `should set task group to help`() {
        project.plugins.apply("io.github.bjoernmayer.gradle-project-dependents")

        val task = project.tasks.findByName("dependents")

        assertThat(task?.group).isEqualTo("help")
    }

    @Test
    fun `should set task description`() {
        project.plugins.apply("io.github.bjoernmayer.gradle-project-dependents")

        val task = project.tasks.findByName("dependents")

        assertThat(task?.description).isEqualTo("Displays the projects that depend on this project")
    }

    @Test
    fun `should configure extension defaults`() {
        project.plugins.apply("io.github.bjoernmayer.gradle-project-dependents")

        val extension = project.extensions.getByType(ProjectDependentsExtension::class.java)

        assertThat(extension.generateStdOutGraph.get()).isTrue()
        assertThat(extension.generateYamlGraph.get()).isFalse()
        assertThat(extension.excludedConfigurations.get()).isEmpty()
    }

    @Test
    fun `should allow configuring extension`() {
        project.plugins.apply("io.github.bjoernmayer.gradle-project-dependents")

        val extension = project.extensions.getByType(ProjectDependentsExtension::class.java)
        extension.generateStdOutGraph.set(false)
        extension.generateYamlGraph.set(true)
        extension.excludedConfigurations.add("testImplementation")

        assertThat(extension.generateStdOutGraph.get()).isFalse()
        assertThat(extension.generateYamlGraph.get()).isTrue()
        assertThat(extension.excludedConfigurations.get()).containsExactly("testImplementation")
    }

    @Test
    fun `task should receive extension configuration`() {
        project.plugins.apply("io.github.bjoernmayer.gradle-project-dependents")

        val extension = project.extensions.getByType(ProjectDependentsExtension::class.java)
        extension.excludedConfigurations.add("testRuntimeOnly")

        // Force task realization to check wiring
        val task = project.tasks.getByName("dependents") as DependentsTask

        assertThat(task.excludedConfs.get()).contains("testRuntimeOnly")
    }
}
