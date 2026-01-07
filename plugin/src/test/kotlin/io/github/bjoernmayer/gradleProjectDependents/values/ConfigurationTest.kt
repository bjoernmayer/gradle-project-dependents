package io.github.bjoernmayer.gradleProjectDependents.values

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.artifacts.Configuration
import org.junit.jupiter.api.Test

class ConfigurationTest {
    @Test
    fun `should create Configuration from string`() {
        val configuration =
            io.github.bjoernmayer.gradleProjectDependents.values
                .Configuration("implementation")

        assertThat(configuration.name).isEqualTo("implementation")
    }

    @Test
    fun `should create Configuration from Gradle Configuration`() {
        val gradleConfig = mockk<Configuration>()
        every { gradleConfig.name } returns "testImplementation"

        val configuration =
            io.github.bjoernmayer.gradleProjectDependents.values
                .Configuration(gradleConfig)

        assertThat(configuration.name).isEqualTo("testImplementation")
    }

    @Test
    fun `should have value equality`() {
        val config1 =
            io.github.bjoernmayer.gradleProjectDependents.values
                .Configuration("api")
        val config2 =
            io.github.bjoernmayer.gradleProjectDependents.values
                .Configuration("api")

        assertThat(config1).isEqualTo(config2)
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode())
    }

    @Test
    fun `should not be equal with different names`() {
        val config1 =
            io.github.bjoernmayer.gradleProjectDependents.values
                .Configuration("api")
        val config2 =
            io.github.bjoernmayer.gradleProjectDependents.values
                .Configuration("implementation")

        assertThat(config1).isNotEqualTo(config2)
    }
}
