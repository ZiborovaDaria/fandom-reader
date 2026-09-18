pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "fandom-reader"

include(
    ":app",
    ":core:domain",
    ":core:data",
    ":core:ui",
    ":feature:library",
    ":feature:import",
    ":feature:reader",
    ":feature:translation",
    ":source:api",
    ":source:ao3",
    ":source:ficbook",
)
