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

    private fun setupSettings(
        rootProjectName: String = "test-project",
        subprojects: List<String> = emptyList(),
    ) {
        val includeStatement =
            if (subprojects.isNotEmpty()) {
                "\ninclude(${subprojects.joinToString(", ") { "\"$it\"" }})"
            } else {
                ""
            }
        settingsFile.writeText(
            """
            rootProject.name = "$rootProjectName"$includeStatement
            """.trimIndent(),
        )
    }

    private fun setupRootBuildFile(extensionConfig: String = "") {
        val extensionBlock =
            if (extensionConfig.isNotEmpty()) {
                """
                
                extensions.configure<io.github.bjoernmayer.gradleProjectDependents.ProjectDependentsExtension> {
                    $extensionConfig
                }"""
            } else {
                ""
            }

        buildFile.writeText(
            """
            plugins {
                id("io.github.bjoernmayer.gradle-project-dependents")
            }
            
            subprojects {
                group = "test-project"
                apply(plugin = "io.github.bjoernmayer.gradle-project-dependents")
                apply(plugin = "java-library")$extensionBlock
            }
            """.trimIndent(),
        )
    }

    private fun setupSubproject(
        name: String,
        dependencies: Map<String, String> = emptyMap(),
    ) {
        File(projectDir, name).mkdirs()
        val depsBlock =
            if (dependencies.isNotEmpty()) {
                val depsString =
                    dependencies.entries.joinToString("\n    ") { (config, project) ->
                        "$config(project(\":$project\"))"
                    }
                """
                dependencies {
                    $depsString
                }
                """.trimIndent()
            } else {
                ""
            }
        File(projectDir, "$name/build.gradle.kts").writeText(depsBlock)
    }

    private fun runDependentsTask(
        project: String = "",
        vararg args: String = arrayOf("--stacktrace"),
    ) = GradleRunner
        .create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(if (project.isEmpty()) "dependents" else ":$project:dependents", *args)
        .build()

    @Test
    fun `should run dependents task on single project`() {
        settingsFile.writeText("""rootProject.name = "test-project"""")
        buildFile.writeText(
            """
            plugins {
                id("io.github.bjoernmayer.gradle-project-dependents")
            }
            """.trimIndent(),
        )

        val result = runDependentsTask()

        assertThat(result.task(":dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project")
    }

    @Test
    fun `should show dependents in multi-project build`() {
        setupSettings(subprojects = listOf("core", "app"))
        setupRootBuildFile()
        setupSubproject("core")
        setupSubproject("app", dependencies = mapOf("implementation" to "core"))

        val result = runDependentsTask("core")

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project:core")
        assertThat(result.output).contains("test-project:app")
    }

    @Test
    fun `should exclude configurations`() {
        setupSettings(subprojects = listOf("core", "app", "test-utils"))
        setupRootBuildFile(extensionConfig = """excludedConfigurations.add("testImplementation")""")
        setupSubproject("core")
        setupSubproject("app", dependencies = mapOf("implementation" to "core"))
        setupSubproject("test-utils", dependencies = mapOf("testImplementation" to "core"))

        val result = runDependentsTask("core")

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project:app")
        assertThat(result.output).doesNotContain("test-utils")
    }

    @Test
    fun `should generate yaml output when enabled`() {
        setupSettings(subprojects = listOf("core", "app"))
        setupRootBuildFile(
            extensionConfig = "outputFormats.add(io.github.bjoernmayer.gradleProjectDependents.OutputFormat.YAML)",
        )
        setupSubproject("core")
        setupSubproject("app", dependencies = mapOf("implementation" to "core"))

        val result = runDependentsTask("core")

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val yamlFile = File(projectDir, "core/build/projectDependents/graph.yaml")
        assertThat(yamlFile).exists()
        val yamlContent = yamlFile.readText()
        assertThat(yamlContent).contains("name: \"test-project:core\"")
        assertThat(yamlContent).contains("test-project:app")
    }

    @Test
    fun `should disable stdout output when configured`() {
        setupSettings(subprojects = listOf("core", "app"))
        setupRootBuildFile(
            extensionConfig = "outputFormats.set(setOf(io.github.bjoernmayer.gradleProjectDependents.OutputFormat.YAML))",
        )
        setupSubproject("core")
        setupSubproject("app", dependencies = mapOf("implementation" to "core"))

        val result = runDependentsTask("core")

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("YAML Graph written to")
    }

    @Test
    fun `should handle transitive dependents`() {
        setupSettings(subprojects = listOf("core", "service", "app"))
        setupRootBuildFile()
        setupSubproject("core")
        setupSubproject("service", dependencies = mapOf("implementation" to "core"))
        setupSubproject("app", dependencies = mapOf("implementation" to "service"))

        val result = runDependentsTask("core")

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project:core")
        assertThat(result.output).contains("test-project:service")
        assertThat(result.output).contains("test-project:app")
    }

    @Test
    fun `should list task in help group`() {
        settingsFile.writeText("""rootProject.name = "test-project"""")
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
        setupSettings(subprojects = listOf("core", "api-client"))
        setupRootBuildFile()
        setupSubproject("core")
        setupSubproject("api-client", dependencies = mapOf("api" to "core"))

        val result = runDependentsTask("core")

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project:api-client")
        assertThat(result.output).contains("api")
    }

    @Test
    fun `should limit depth when depth option is set`() {
        setupSettings(subprojects = listOf("core", "service", "app"))
        setupRootBuildFile(extensionConfig = "depth.set(1)")
        setupSubproject("core")
        setupSubproject("service", dependencies = mapOf("implementation" to "core"))
        setupSubproject("app", dependencies = mapOf("implementation" to "service"))

        val result = runDependentsTask("core")

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project:core")
        assertThat(result.output).contains("test-project:service")
        // app should NOT appear because it's at depth 2 (service -> app)
        assertThat(result.output).doesNotContain("test-project:app")
    }

    @Test
    fun `should show full tree when depth is not set`() {
        setupSettings(subprojects = listOf("core", "service", "app"))
        setupRootBuildFile()
        setupSubproject("core")
        setupSubproject("service", dependencies = mapOf("implementation" to "core"))
        setupSubproject("app", dependencies = mapOf("implementation" to "service"))

        val result = runDependentsTask("core")

        assertThat(result.task(":core:dependents")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("test-project:core")
        assertThat(result.output).contains("test-project:service")
        assertThat(result.output).contains("test-project:app")
    }
}
