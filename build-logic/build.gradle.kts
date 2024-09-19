plugins {
    `kotlin-dsl`
}

repositories {
    google {
        content {
            includeGroupByRegex(".*google.*")
            includeGroupByRegex(".*android.*")
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        register("BuildLogic") {
            id = "build-logic"
            implementationClass = "com.mai.build_logic.BuildLogic"
        }
    }
}

dependencies {
    implementation(libs.gradle.android)
    implementation(libs.gradle.kt)
}
