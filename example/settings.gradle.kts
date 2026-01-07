rootProject.name = "example-project"

pluginManagement {
    includeBuild("../plugin")
}

include("core", "service", "api", "app")
