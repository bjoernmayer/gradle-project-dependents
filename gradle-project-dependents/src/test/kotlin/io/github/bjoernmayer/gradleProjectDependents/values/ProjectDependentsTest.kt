package io.github.bjoernmayer.gradleProjectDependents.values

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectDependentsTest {
    @Test
    fun `should create ProjectDependents with name only`() {
        val projectDependents = ProjectDependents(":core")

        assertThat(projectDependents.name).isEqualTo(":core")
        assertThat(projectDependents.dependents).isEmpty()
    }

    @Test
    fun `should create ProjectDependents with dependents`() {
        val childDependents = ProjectDependents(":app")
        val config = Configuration("implementation")
        val dependentsMap = mapOf(config to listOf(childDependents))

        val projectDependents = ProjectDependents(":core", dependentsMap)

        assertThat(projectDependents.name).isEqualTo(":core")
        assertThat(projectDependents.dependents).hasSize(1)
        assertThat(projectDependents.dependents[config]).containsExactly(childDependents)
    }

    @Test
    fun `should have data class equality`() {
        val dependents1 = ProjectDependents(":core")
        val dependents2 = ProjectDependents(":core")

        assertThat(dependents1).isEqualTo(dependents2)
        assertThat(dependents1.hashCode()).isEqualTo(dependents2.hashCode())
    }

    @Test
    fun `should not be equal with different names`() {
        val dependents1 = ProjectDependents(":core")
        val dependents2 = ProjectDependents(":utils")

        assertThat(dependents1).isNotEqualTo(dependents2)
    }

    @Test
    fun `should support multiple configurations`() {
        val child1 = ProjectDependents(":app")
        val child2 = ProjectDependents(":web")
        val apiConfig = Configuration("api")
        val implConfig = Configuration("implementation")

        val dependentsMap =
            mapOf(
                apiConfig to listOf(child1),
                implConfig to listOf(child2),
            )

        val projectDependents = ProjectDependents(":core", dependentsMap)

        assertThat(projectDependents.dependents).hasSize(2)
        assertThat(projectDependents.dependents[apiConfig]).containsExactly(child1)
        assertThat(projectDependents.dependents[implConfig]).containsExactly(child2)
    }

    @Test
    fun `should support multiple dependents per configuration`() {
        val child1 = ProjectDependents(":app")
        val child2 = ProjectDependents(":web")
        val config = Configuration("implementation")

        val dependentsMap = mapOf(config to listOf(child1, child2))

        val projectDependents = ProjectDependents(":core", dependentsMap)

        assertThat(projectDependents.dependents[config]).containsExactly(child1, child2)
    }
}
