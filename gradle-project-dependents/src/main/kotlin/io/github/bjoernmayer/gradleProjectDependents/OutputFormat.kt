package io.github.bjoernmayer.gradleProjectDependents

/**
 * Output formats available for the dependency graph.
 */
public enum class OutputFormat {
    /**
     * Prints the dependency graph to standard output in a tree format.
     */
    STDOUT,

    /**
     * Outputs the dependency graph as a YAML file.
     */
    YAML,

    /**
     * Outputs the dependency graph as a JSON file using Kotlin Serialization.
     */
    JSON,

    /**
     * Outputs the dependency graph as a Mermaid flowchart in Markdown.
     */
    MERMAID,
}
