package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer

import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class YamlPrinterTest {
    @TempDir
    lateinit var tempDir: File

    private val configComparator = Comparator<Configuration> { o1, o2 -> o1.name.compareTo(o2.name) }

    @Test
    fun `should write yaml file for project with no dependents`() {
        val outputFile = File(tempDir, "graph.yaml")
        val printer = YamlPrinter(emptySet(), outputFile)
        val projectDependents = ProjectDependents("myproject")

        printer.print(projectDependents)

        assertThat(outputFile).exists()
        val content = outputFile.readText()
        assertThat(content).contains("name: \"myproject\"")
    }

    @Test
    fun `should write yaml file with dependents`() {
        val outputFile = File(tempDir, "graph.yaml")
        val printer = YamlPrinter(emptySet(), outputFile)
        val child = ProjectDependents(":app")
        val config = Configuration("implementation")
        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(child)),
            )

        printer.print(projectDependents)

        assertThat(outputFile).exists()
        val content = outputFile.readText()
        assertThat(content).contains("name: \"myproject:core\"")
        assertThat(content).contains("dependents:")
        assertThat(content).contains("\"implementation\":")
        assertThat(content).contains("name: \":app\"")
    }

    @Test
    fun `should write yaml file with nested dependents`() {
        val outputFile = File(tempDir, "graph.yaml")
        val printer = YamlPrinter(emptySet(), outputFile)
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

        printer.print(projectDependents)

        assertThat(outputFile).exists()
        val content = outputFile.readText()
        assertThat(content).contains("name: \"myproject:core\"")
        assertThat(content).contains("name: \":app\"")
        assertThat(content).contains("name: \":web\"")
    }

    @Test
    fun `should exclude configurations`() {
        val outputFile = File(tempDir, "graph.yaml")
        val excludedConfig = Configuration("testImplementation")
        val printer = YamlPrinter(setOf(excludedConfig), outputFile)

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

        printer.print(projectDependents)

        val content = outputFile.readText()
        assertThat(content).contains("name: \":app\"")
        assertThat(content).contains("\"implementation\":")
        assertThat(content).doesNotContain(":test-utils")
        assertThat(content).doesNotContain("testImplementation")
    }

    @Test
    fun `should write multiple dependents for same configuration`() {
        val outputFile = File(tempDir, "graph.yaml")
        val printer = YamlPrinter(emptySet(), outputFile)
        val child1 = ProjectDependents(":app")
        val child2 = ProjectDependents(":web")
        val config = Configuration("implementation")

        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(child1, child2)),
            )

        printer.print(projectDependents)

        val content = outputFile.readText()
        assertThat(content).contains("name: \":app\"")
        assertThat(content).contains("name: \":web\"")
    }

    @Test
    fun `should reset file modification time`() {
        val outputFile = File(tempDir, "graph.yaml")
        val printer = YamlPrinter(emptySet(), outputFile)
        val projectDependents = ProjectDependents("myproject")

        printer.print(projectDependents)

        // Last modified should be set to 0 (epoch)
        assertThat(outputFile.lastModified()).isEqualTo(0L)
    }

    @Test
    fun `should handle multiple configurations`() {
        val outputFile = File(tempDir, "graph.yaml")
        val printer = YamlPrinter(emptySet(), outputFile)
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

        printer.print(projectDependents)

        val content = outputFile.readText()
        assertThat(content).contains("\"api\":")
        assertThat(content).contains("\"implementation\":")
        assertThat(content).contains("name: \":app\"")
        assertThat(content).contains("name: \":web\"")
    }

    @Test
    fun `should print path message to stdout`() {
        val outputFile = File(tempDir, "graph.yaml")
        val printer = YamlPrinter(emptySet(), outputFile)
        val projectDependents = ProjectDependents("myproject")

        // Capture stdout
        val originalOut = System.out
        val outputStream = java.io.ByteArrayOutputStream()
        System.setOut(java.io.PrintStream(outputStream))

        try {
            printer.print(projectDependents)
            val stdoutContent = outputStream.toString()
            assertThat(stdoutContent).contains("Yaml Graph written to")
            assertThat(stdoutContent).contains(outputFile.absolutePath)
        } finally {
            System.setOut(originalOut)
        }
    }
}
