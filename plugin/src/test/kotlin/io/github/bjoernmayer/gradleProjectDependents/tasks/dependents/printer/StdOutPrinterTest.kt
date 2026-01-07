package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer

import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.logging.Logger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class StdOutPrinterTest {
    private val originalOut = System.out
    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var logger: Logger
    private val configComparator = Comparator<Configuration> { o1, o2 -> o1.name.compareTo(o2.name) }

    @BeforeEach
    fun setUp() {
        outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        logger = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
    }

    @Test
    fun `should print project with no dependents`() {
        val printer = StdOutPrinter(emptySet())
        val projectDependents = ProjectDependents("myproject")

        printer.print(projectDependents, logger)

        val output = outputStream.toString()
        assertThat(output).contains("myproject")
    }

    @Test
    fun `should print project with single dependent`() {
        val printer = StdOutPrinter(emptySet())
        val child = ProjectDependents(":app")
        val config = Configuration("implementation")
        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(child)),
            )

        printer.print(projectDependents, logger)

        val output = outputStream.toString()
        assertThat(output).contains("myproject:core")
        assertThat(output).contains(":app")
        assertThat(output).contains("implementation")
    }

    @Test
    fun `should print nested dependents`() {
        val printer = StdOutPrinter(emptySet())
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

        val output = outputStream.toString()
        assertThat(output).contains("myproject:core")
        assertThat(output).contains(":app")
        assertThat(output).contains(":web")
    }

    @Test
    fun `should exclude configurations`() {
        val excludedConfig = Configuration("testImplementation")
        val printer = StdOutPrinter(setOf(excludedConfig))

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

        val output = outputStream.toString()
        assertThat(output).contains(":app")
        assertThat(output).contains("implementation")
        assertThat(output).doesNotContain(":test-utils")
        assertThat(output).doesNotContain("testImplementation")
    }

    @Test
    fun `should print multiple dependents for same configuration`() {
        val printer = StdOutPrinter(emptySet())
        val child1 = ProjectDependents(":app")
        val child2 = ProjectDependents(":web")
        val config = Configuration("implementation")

        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(child1, child2)),
            )

        printer.print(projectDependents, logger)

        val output = outputStream.toString()
        assertThat(output).contains(":app")
        assertThat(output).contains(":web")
    }

    @Test
    fun `should handle circular dependencies by tracking connections`() {
        val printer = StdOutPrinter(emptySet())
        val config = Configuration("implementation")

        // Create circular reference: core -> app -> core (should not loop forever)
        val core = ProjectDependents("myproject:core", emptyMap())
        val app =
            ProjectDependents(
                ":app",
                sortedMapOf(configComparator, config to listOf(core)),
            )

        // Manually set up the circular reference
        val coreWithApp =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(app)),
            )

        // This should complete without infinite loop
        printer.print(coreWithApp, logger)

        val output = outputStream.toString()
        assertThat(output).contains("myproject:core")
        assertThat(output).contains(":app")
    }
}
