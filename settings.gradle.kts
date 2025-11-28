pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    includeBuild("build-logic")
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":app")
rootDir.resolve("library").listFiles().filter { it.isDirectory }.forEach { dir ->
    include(":library:${dir.name}")
}

rootProject.name = "OAIDViewer"
