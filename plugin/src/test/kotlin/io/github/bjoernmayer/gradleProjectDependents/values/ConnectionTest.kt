package io.github.bjoernmayer.gradleProjectDependents.values

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConnectionTest {
    @Test
    fun `should create Connection with all properties`() {
        val configuration = Configuration("implementation")
        val connection =
            Connection(
                configuration = configuration,
                dependentProjectName = ":app",
                dependencyProjectName = ":core",
            )

        assertThat(connection.configuration).isEqualTo(configuration)
        assertThat(connection.dependentProjectName).isEqualTo(":app")
        assertThat(connection.dependencyProjectName).isEqualTo(":core")
    }

    @Test
    fun `should have data class equality`() {
        val config = Configuration("api")
        val connection1 = Connection(config, ":app", ":core")
        val connection2 = Connection(config, ":app", ":core")

        assertThat(connection1).isEqualTo(connection2)
        assertThat(connection1.hashCode()).isEqualTo(connection2.hashCode())
    }

    @Test
    fun `should not be equal with different dependent project`() {
        val config = Configuration("api")
        val connection1 = Connection(config, ":app", ":core")
        val connection2 = Connection(config, ":web", ":core")

        assertThat(connection1).isNotEqualTo(connection2)
    }

    @Test
    fun `should not be equal with different dependency project`() {
        val config = Configuration("api")
        val connection1 = Connection(config, ":app", ":core")
        val connection2 = Connection(config, ":app", ":utils")

        assertThat(connection1).isNotEqualTo(connection2)
    }

    @Test
    fun `should not be equal with different configuration`() {
        val connection1 = Connection(Configuration("api"), ":app", ":core")
        val connection2 = Connection(Configuration("implementation"), ":app", ":core")

        assertThat(connection1).isNotEqualTo(connection2)
    }
}
