# Example Project

This is an example multi-project build demonstrating the `gradle-project-dependents` plugin.

## Project Structure

```
example/
├── core/       # Foundation module (no dependencies)
├── api/        # API module (depends on core via api)
├── service/    # Service module (depends on core and api)
└── app/        # Application (depends on service and api)
```

## Dependency Graph

```
core <── api <── service <── app
  │              │
  └──────────────┘
         │
         └───── app
```

## Usage

### See dependents of the core module:

```bash
./gradlew :core:dependents
```

Expected output:

```
+--- example-project:core
    \--- example-project:api (api)
        +--- example-project:app (implementation)
        \--- example-project:service (implementation)
            \--- example-project:app (implementation)
    \--- example-project:service (implementation)
        \--- example-project:app (implementation)
Yaml Graph written to .../core/build/projectDependents/graph.yaml
JSON Graph written to .../core/build/projectDependents/graph.json
Mermaid Graph written to .../core/build/projectDependents/graph.md
```

### See dependents of the api module:

```bash
./gradlew :api:dependents
```

### See dependents of the service module:

```bash
./gradlew :service:dependents
```

## Output Files

After running the `dependents` task, you'll find the following files in each module's build directory:

- `build/projectDependents/graph.yaml` - YAML format
- `build/projectDependents/graph.json` - JSON format
- `build/projectDependents/graph.md` - Mermaid flowchart

## Configuration

The example configures all output formats and excludes test configurations:

```kotlin
extensions.configure<ProjectDependentsExtension> {
    outputFormats.set(setOf(
        OutputFormat.STDOUT,
        OutputFormat.YAML,
        OutputFormat.JSON,
        OutputFormat.MERMAID
    ))

    excludedConfigurations.add("testImplementation")
    excludedConfigurations.add("testRuntimeOnly")
}
```
