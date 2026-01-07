package io.github.bjoernmayer.gradleProjectDependents.tasks.dependents.printer.model

import io.github.bjoernmayer.gradleProjectDependents.values.Configuration
import io.github.bjoernmayer.gradleProjectDependents.values.ProjectDependents
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DependentsGraphTest {
    private val configComparator = Comparator<Configuration> { o1, o2 -> o1.name.compareTo(o2.name) }

    @Test
    fun `should convert project with no dependents`() {
        val projectDependents = ProjectDependents("myproject")

        val graph = DependentsGraph.fromProjectDependents(projectDependents, emptySet())

        assertThat(graph.name).isEqualTo("myproject")
        assertThat(graph.dependents).isEmpty()
    }

    @Test
    fun `should convert project with single dependent`() {
        val child = ProjectDependents(":app")
        val config = Configuration("implementation")
        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(child)),
            )

        val graph = DependentsGraph.fromProjectDependents(projectDependents, emptySet())

        assertThat(graph.name).isEqualTo("myproject:core")
        assertThat(graph.dependents).hasSize(1)
        assertThat(graph.dependents["implementation"]).hasSize(1)
        assertThat(graph.dependents["implementation"]?.first()?.name).isEqualTo(":app")
    }

    @Test
    fun `should convert nested dependents`() {
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

        val graph = DependentsGraph.fromProjectDependents(projectDependents, emptySet())

        assertThat(graph.name).isEqualTo("myproject:core")
        val appDependent = graph.dependents["implementation"]?.first()
        assertThat(appDependent?.name).isEqualTo(":app")
        assertThat(
            appDependent
                ?.dependents
                ?.get("implementation")
                ?.first()
                ?.name,
        ).isEqualTo(":web")
    }

    @Test
    fun `should exclude configurations`() {
        val testChild = ProjectDependents(":test-utils")
        val implChild = ProjectDependents(":app")
        val implConfig = Configuration("implementation")
        val testConfig = Configuration("testImplementation")

        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(
                    configComparator,
                    implConfig to listOf(implChild),
                    testConfig to listOf(testChild),
                ),
            )

        val graph = DependentsGraph.fromProjectDependents(projectDependents, setOf(testConfig))

        assertThat(graph.dependents).hasSize(1)
        assertThat(graph.dependents["implementation"]).hasSize(1)
        assertThat(graph.dependents["testImplementation"]).isNull()
    }

    @Test
    fun `should handle multiple configurations`() {
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

        val graph = DependentsGraph.fromProjectDependents(projectDependents, emptySet())

        assertThat(graph.dependents).hasSize(2)
        assertThat(graph.dependents["api"]?.first()?.name).isEqualTo(":app")
        assertThat(graph.dependents["implementation"]?.first()?.name).isEqualTo(":web")
    }

    @Test
    fun `should handle multiple dependents per configuration`() {
        val child1 = ProjectDependents(":app")
        val child2 = ProjectDependents(":web")
        val config = Configuration("implementation")

        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(child1, child2)),
            )

        val graph = DependentsGraph.fromProjectDependents(projectDependents, emptySet())

        assertThat(graph.dependents["implementation"]).hasSize(2)
        assertThat(graph.dependents["implementation"]?.map { it.name }).containsExactly(":app", ":web")
    }

    @Test
    fun `should prevent circular references`() {
        // Create a circular dependency: core -> app -> core
        val coreRef = ProjectDependents("myproject:core")
        val config = Configuration("implementation")
        val app =
            ProjectDependents(
                ":app",
                sortedMapOf(configComparator, config to listOf(coreRef)),
            )
        val projectDependents =
            ProjectDependents(
                "myproject:core",
                sortedMapOf(configComparator, config to listOf(app)),
            )

        val graph = DependentsGraph.fromProjectDependents(projectDependents, emptySet())

        // The graph should not infinitely recurse - the second reference to core should be empty
        assertThat(graph.name).isEqualTo("myproject:core")
        val appGraph = graph.dependents["implementation"]?.first()
        assertThat(appGraph?.name).isEqualTo(":app")
        // The circular reference back to core should have no dependents (to break the cycle)
        val coreRefGraph = appGraph?.dependents?.get("implementation")?.first()
        assertThat(coreRefGraph?.name).isEqualTo("myproject:core")
        assertThat(coreRefGraph?.dependents).isEmpty()
    }
}
