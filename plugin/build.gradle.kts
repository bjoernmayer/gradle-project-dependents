plugins {
    idea
    `java-gradle-plugin`
    kotlin("jvm") version "2.0.21"

    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("com.gradle.plugin-publish") version "1.3.0"
}

kotlin {
    explicitApi()

    jvmToolchain(21)
}

ktlint {
    version.set("1.4.1")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("io.mockk:mockk:1.13.13")
}

group = "io.github.bjoernmayer"

version = "0.3.0"

gradlePlugin {
    website = "https://github.com/bjoernmayer/gradle-project-dependents"
    vcsUrl = "https://github.com/bjoernmayer/gradle-project-dependents"

    val artifactregistryGradlePlugin by plugins.creating {
        id = "io.github.bjoernmayer.gradle-project-dependents"
        implementationClass = "io.github.bjoernmayer.gradleProjectDependents.ProjectDependentsGradlePlugin"

        displayName = "Project Dependents"
        description = "List project dependents on a project in multi-project"
        tags = listOf("dependents", "dependencies", "multi-project")
    }
}

idea {
    // Going through decompiled class files is no fun (no code navigation),
    // hence, we instruct Gradle to download the actual sources
    module {
        isDownloadJavadoc = false
        isDownloadSources = true
    }
}

tasks.test {
    useJUnitPlatform()
}
