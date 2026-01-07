plugins {
    idea
    `java-gradle-plugin`
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"

    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("com.gradle.plugin-publish") version "2.0.0"
}

kotlin {
    explicitApi()

    jvmToolchain(21)
}

ktlint {
    version.set("1.8.0")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")

    testImplementation("io.mockk:mockk:1.14.7")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation(gradleTestKit())
}

group = "io.github.bjoernmayer"

version = "0.3.3"

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
