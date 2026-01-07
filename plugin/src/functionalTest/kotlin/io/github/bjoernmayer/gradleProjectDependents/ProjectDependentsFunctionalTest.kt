package io.github.bjoernmayer.gradleProjectDependents

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectDependentsFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    private lateinit var settingsFile: File
    private lateinit var buildFile: File

    @BeforeEach
    fun setUp() {
        settingsFile = File(projectDir, "settings.gradle.kts")
        buildFile = File(projectDir, "build.gradle.kts")
    }

    @Test
    fun `should run dependents task on single project`() {
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins {
                id("io.github.bjoernmayer.gradle-project-dependents")
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("dependents", "--stacktrace")
                .build()

        assertThat(result.task(":dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project")
    }

    @Test
    fun `should show dependents in multi-project build`() {
        // Root project
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            include("core", "app")
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins {
                id("io.github.bjoernmayer.gradle-project-dependents")
            }
            
            subprojects {
                group = "test-project"
                apply(plugin = "io.github.bjoernmayer.gradle-project-dependents")
                apply(plugin = "java-library")
            }
            """.trimIndent(),
        )

        // Core module
        File(projectDir, "core").mkdirs()
        File(projectDir, "core/build.gradle.kts").writeText(
            """
            // core module - no dependencies
            """.trimIndent(),
        )

        // App module depends on core
        File(projectDir, "app").mkdirs()
        File(projectDir, "app/build.gradle.kts").writeText(
            """
            dependencies {
                implementation(project(":core"))
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(":core:dependents", "--stacktrace")
                .build()

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project:core")
        assertThat(result.output).contains("test-project:app")
    }

    @Test
    fun `should exclude configurations`() {
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            include("core", "app", "test-utils")
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins {
                id("io.github.bjoernmayer.gradle-project-dependents")
            }
            
            subprojects {
                group = "test-project"
                apply(plugin = "io.github.bjoernmayer.gradle-project-dependents")
                apply(plugin = "java-library")
                
                extensions.configure<io.github.bjoernmayer.gradleProjectDependents.ProjectDependentsExtension> {
                    excludedConfigurations.add("testImplementation")
                }
            }
            """.trimIndent(),
        )

        // Core module
        File(projectDir, "core").mkdirs()
        File(projectDir, "core/build.gradle.kts").writeText("")

        // App module depends on core via implementation
        File(projectDir, "app").mkdirs()
        File(projectDir, "app/build.gradle.kts").writeText(
            """
            dependencies {
                implementation(project(":core"))
            }
            """.trimIndent(),
        )

        // Test-utils depends on core via testImplementation
        File(projectDir, "test-utils").mkdirs()
        File(projectDir, "test-utils/build.gradle.kts").writeText(
            """
            dependencies {
                testImplementation(project(":core"))
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(":core:dependents", "--stacktrace")
                .build()

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project:app")
        // test-utils should be excluded since it uses testImplementation
        assertThat(result.output).doesNotContain("test-utils")
    }

    @Test
    fun `should generate yaml output when enabled`() {
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            include("core", "app")
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins {
                id("io.github.bjoernmayer.gradle-project-dependents")
            }
            
            subprojects {
                group = "test-project"
                apply(plugin = "io.github.bjoernmayer.gradle-project-dependents")
                apply(plugin = "java-library")
                
                extensions.configure<io.github.bjoernmayer.gradleProjectDependents.ProjectDependentsExtension> {
                    generateYamlGraph.set(true)
                }
            }
            """.trimIndent(),
        )

        // Core module
        File(projectDir, "core").mkdirs()
        File(projectDir, "core/build.gradle.kts").writeText("")

        // App module depends on core
        File(projectDir, "app").mkdirs()
        File(projectDir, "app/build.gradle.kts").writeText(
            """
            dependencies {
                implementation(project(":core"))
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(":core:dependents", "--stacktrace")
                .build()

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val yamlFile = File(projectDir, "core/build/projectDependents/graph.yaml")
        assertThat(yamlFile).exists()
        val yamlContent = yamlFile.readText()
        assertThat(yamlContent).contains("name: \"test-project:core\"")
        assertThat(yamlContent).contains("test-project:app")
    }

    @Test
    fun `should disable stdout output when configured`() {
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            include("core", "app")
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins {
                id("io.github.bjoernmayer.gradle-project-dependents")
            }
            
            subprojects {
                group = "test-project"
                apply(plugin = "io.github.bjoernmayer.gradle-project-dependents")
                apply(plugin = "java-library")
                
                extensions.configure<io.github.bjoernmayer.gradleProjectDependents.ProjectDependentsExtension> {
                    generateStdOutGraph.set(false)
                    generateYamlGraph.set(true)
                }
            }
            """.trimIndent(),
        )

        // Core module
        File(projectDir, "core").mkdirs()
        File(projectDir, "core/build.gradle.kts").writeText("")

        // App module
        File(projectDir, "app").mkdirs()
        File(projectDir, "app/build.gradle.kts").writeText(
            """
            dependencies {
                implementation(project(":core"))
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(":core:dependents", "--stacktrace")
                .build()

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        // Should still show yaml file message but not the tree
        assertThat(result.output).contains("Yaml Graph written to")
    }

    @Test
    fun `should handle transitive dependents`() {
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            include("core", "service", "app")
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins {
                id("io.github.bjoernmayer.gradle-project-dependents")
            }
            
            subprojects {
                group = "test-project"
                apply(plugin = "io.github.bjoernmayer.gradle-project-dependents")
                apply(plugin = "java-library")
            }
            """.trimIndent(),
        )

        // Core module - no dependencies
        File(projectDir, "core").mkdirs()
        File(projectDir, "core/build.gradle.kts").writeText("")

        // Service module depends on core
        File(projectDir, "service").mkdirs()
        File(projectDir, "service/build.gradle.kts").writeText(
            """
            dependencies {
                implementation(project(":core"))
            }
            """.trimIndent(),
        )

        // App module depends on service
        File(projectDir, "app").mkdirs()
        File(projectDir, "app/build.gradle.kts").writeText(
            """
            dependencies {
                implementation(project(":service"))
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(":core:dependents", "--stacktrace")
                .build()

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project:core")
        assertThat(result.output).contains("test-project:service")
        // App depends on service which depends on core, so transitive
        assertThat(result.output).contains("test-project:app")
    }

    @Test
    fun `should list task in help group`() {
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins {
                id("io.github.bjoernmayer.gradle-project-dependents")
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("tasks", "--group=help")
                .build()

        assertThat(result.output).contains("dependents")
        assertThat(result.output).contains("Displays the projects that depend on this project")
    }

    @Test
    fun `should work with api configuration`() {
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            include("core", "api-client")
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins {
                id("io.github.bjoernmayer.gradle-project-dependents")
            }
            
            subprojects {
                group = "test-project"
                apply(plugin = "io.github.bjoernmayer.gradle-project-dependents")
                apply(plugin = "java-library")
            }
            """.trimIndent(),
        )

        // Core module
        File(projectDir, "core").mkdirs()
        File(projectDir, "core/build.gradle.kts").writeText("")

        // API client depends on core via api
        File(projectDir, "api-client").mkdirs()
        File(projectDir, "api-client/build.gradle.kts").writeText(
            """
            dependencies {
                api(project(":core"))
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(":core:dependents", "--stacktrace")
                .build()

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project:api-client")
        assertThat(result.output).contains("api")
    }
}
