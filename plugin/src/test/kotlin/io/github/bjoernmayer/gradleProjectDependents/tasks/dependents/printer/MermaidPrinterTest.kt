package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer

import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.logging.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MermaidPrinterTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var logger: Logger
    private val configComparator = Comparator<Configuration> { o1, o2 -> o1.name.compareTo(o2.name) }

    @BeforeEach
    fun setUp() {
        logger = mockk(relaxed = true)
    }

    @Test
    fun `should write mermaid file for project with no dependents`() {
        val outputFile = File(tempDir, "graph.md")
        val printer = MermaidPrinter(emptySet(), null, outputFile)
        val projectDependents = ProjectDependents("myproject")

        printer.print(projectDependents, logger)

        assertThat(outputFile).exists()
        val content = outputFile.readText()
        assertThat(content).contains("```mermaid")
        assertThat(content).contains("flowchart BT")
        assertThat(content).contains("```")
    }

    @Test
    fun `should write mermaid file with dependents`() {
        val outputFile = File(tempDir, "graph.md")
        val printer = MermaidPrinter(emptySet(), null, outputFile)
        val child = ProjectDependents(":app")
        val config = Configuration("implementation")
        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(child)),
            )

        printer.print(projectDependents, logger)

        assertThat(outputFile).exists()
        val content = outputFile.readText()
        assertThat(content).contains("```mermaid")
        assertThat(content).contains("flowchart BT")
        assertThat(content).contains(":app")
        assertThat(content).contains("myproject:core")
        assertThat(content).contains("implementation")
        assertThat(content).contains("-->")
    }

    @Test
    fun `should write mermaid file with nested dependents`() {
        val outputFile = File(tempDir, "graph.md")
        val printer = MermaidPrinter(emptySet(), null, outputFile)
        val grandChild = ProjectDependents(":web")
        val config = Configuration("implementation")
        val child =
            ProjectDependents(
                ":app",
                sortedMapOf(configComparator, config to listOf(grandChild)),
            )
        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(child)),
            )

        printer.print(projectDependents, logger)

        assertThat(outputFile).exists()
        val content = outputFile.readText()
        assertThat(content).contains("myproject:core")
        assertThat(content).contains(":app")
        assertThat(content).contains(":web")
    }

    @Test
    fun `should exclude configurations`() {
        val outputFile = File(tempDir, "graph.md")
        val excludedConfig = Configuration("testImplementation")
        val printer = MermaidPrinter(setOf(excludedConfig), null, outputFile)

        val testChild = ProjectDependents(":test-utils")
        val implChild = ProjectDependents(":app")
        val implConfig = Configuration("implementation")

        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(
                    configComparator,
                    implConfig to listOf(implChild),
                    excludedConfig to listOf(testChild),
                ),
            )

        printer.print(projectDependents, logger)

        val content = outputFile.readText()
        assertThat(content).contains(":app")
        assertThat(content).contains("implementation")
        assertThat(content).doesNotContain(":test-utils")
        assertThat(content).doesNotContain("testImplementation")
    }

    @Test
    fun `should write multiple dependents for same configuration`() {
        val outputFile = File(tempDir, "graph.md")
        val printer = MermaidPrinter(emptySet(), null, outputFile)
        val child1 = ProjectDependents(":app")
        val child2 = ProjectDependents(":web")
        val config = Configuration("implementation")

        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(child1, child2)),
            )

        printer.print(projectDependents, logger)

        val content = outputFile.readText()
        assertThat(content).contains(":app")
        assertThat(content).contains(":web")
    }

    @Test
    fun `should reset file modification time`() {
        val outputFile = File(tempDir, "graph.md")
        val printer = MermaidPrinter(emptySet(), null, outputFile)
        val child = ProjectDependents(":app")
        val config = Configuration("implementation")
        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(child)),
            )

        printer.print(projectDependents, logger)

        assertThat(outputFile.lastModified()).isEqualTo(0L)
    }

    @Test
    fun `should handle multiple configurations`() {
        val outputFile = File(tempDir, "graph.md")
        val printer = MermaidPrinter(emptySet(), null, outputFile)
        val child1 = ProjectDependents(":app")
        val child2 = ProjectDependents(":web")
        val apiConfig = Configuration("api")
        val implConfig = Configuration("implementation")

        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(
                    configComparator,
                    apiConfig to listOf(child1),
                    implConfig to listOf(child2),
                ),
            )

        printer.print(projectDependents, logger)

        val content = outputFile.readText()
        assertThat(content).contains("api")
        assertThat(content).contains("implementation")
        assertThat(content).contains(":app")
        assertThat(content).contains(":web")
    }

    @Test
    fun `should sanitize node ids`() {
        val outputFile = File(tempDir, "graph.md")
        val printer = MermaidPrinter(emptySet(), null, outputFile)
        val child = ProjectDependents(":app-module")
        val config = Configuration("implementation")
        val projectDependents =
            ProjectDependents(
                "my.project:core",
                sortedMapOf(configComparator, config to listOf(child)),
            )

        printer.print(projectDependents, logger)

        val content = outputFile.readText()
        // IDs should have colons, dashes, and dots replaced with underscores
        assertThat(content).contains("_app_module")
        assertThat(content).contains("my_project_core")
    }

    @Test
    fun `should create bottom-to-top flowchart`() {
        val outputFile = File(tempDir, "graph.md")
        val printer = MermaidPrinter(emptySet(), null, outputFile)
        val child = ProjectDependents(":app")
        val config = Configuration("implementation")
        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(child)),
            )

        printer.print(projectDependents, logger)

        val content = outputFile.readText()
        // BT = Bottom to Top, showing dependents pointing to their dependencies
        assertThat(content).contains("flowchart BT")
    }
}
