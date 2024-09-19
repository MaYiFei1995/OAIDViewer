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

include(
    ":app", ":library:impl",
    ":library:lib_1023",
    ":library:lib_1025",
    ":library:lib_1026",
    ":library:lib_1027",
    ":library:lib_1029",
    ":library:lib_1030",
    ":library:lib_1100",
    ":library:lib_1200",
    ":library:lib_1201",
    ":library:lib_2000",
    ":library:lib_2100",
    ":library:lib_2200",
    ":library:lib_2300",
    ":library:lib_2400",
    ":library:lib_2500",
    ":library:lib_2501"
)

rootProject.name = "OAIDViewer"
