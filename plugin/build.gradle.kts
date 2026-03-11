plugins {
    idea
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.ktlint)
    alias(libs.plugins.plugin.publish)
}

kotlin {
    explicitApi()

    jvmToolchain(21)
}

ktlint {
    version.set(
        libs.versions.ktlint
            .asProvider()
            .get(),
    )
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(libs.kotlinx.serialization.json)

    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter)

    testImplementation(libs.mockk)
    testImplementation(libs.assertj.core)
    testImplementation(gradleTestKit())
}

group = "io.github.bjoernmayer"

version = "1.0.0"

gradlePlugin {
    website = "https://github.com/bjoernmayer/gradle-project-dependents"
    vcsUrl = "https://github.com/bjoernmayer/gradle-project-dependents"

    @Suppress("unused")
    val projectDependentsPlugin by plugins.creating {
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

// Configure functional test source set
val functionalTestSourceSet: SourceSet =
    sourceSets.create("functionalTest")

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.check {
    dependsOn(functionalTest)
}
