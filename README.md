# Gradle Project Dependents Plugin

[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?label=Plugin%20Portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fio%2Fgithub%2Fbjoernmayer%2Fgradle-project-dependents%2Fio.github.bjoernmayer.gradle-project-dependents.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/io.github.bjoernmayer.gradle-project-dependents)

Gradle Plugin to list project dependents of a project in multi-projects.

## Usage
Using the https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block:

```kts
plugins {
    id("io.github.bjoernmayer.gradle-project-dependents") version "<version>"
}
```

Using legacy plugin application:

```kts
buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("io.github.bjoernmayer:plugin:<version>")
    }
}

apply(plugin = "io.github.bjoernmayer.gradle-project-dependents")
```

## Tasks

### `dependents`
Displays a tree of dependent projects on the project, where the task was executed in.

#### Example:
Imagine a multi-project with Project A, Project B, Project C

Project B `build.gradle.kts`:
```
dependencies {
    implementation(project(":projectA"))
}
```

Project C `build.gradle.kts`:
```
dependencies {
    implementation(project(":projectA"))
    implementation(project(":projectC"))
}
```

Executing the task:
```bash
./gradlew :projectA:dependents
```

Expected output:
```bash
+--- project root-project-name:projectA
|    +--- project root-project-name:projectB (implementation)
|    |    \--- project root-project-name:projectC (implementation)
|    \--- project root-project-name:projectC (implementation)
```

## Configuration
### `excludedConfigurations`
Configurations can be excluded from the printed graph by adding them to `excludedConfigurations`:

```kts
// build.gradle.kts

projectDependents {
    excludedConfigurations.add("testImplementation")
}
```

## Developing
Checkout this repository next to some project, where you want to use it.
Then in the `settings.gradle.kts` of said project, add this:

```kts
// settings.gradle.kts

pluginManagement {
    includeBuild("../gradle-project-dependents")
}
```

Then, you can just apply the plugin like usual:
```kts
// build.gradle.kts

id("io.github.bjoernmayer.gradle-project-dependents")
```
