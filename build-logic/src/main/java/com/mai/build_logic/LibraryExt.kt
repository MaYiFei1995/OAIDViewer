package com.mai.build_logic

import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

fun Project.setupLibraryModule(version: String = "", block: LibraryExtension.() -> Unit = {}) {
    setupBaseModule<LibraryExtension> {
        namespace = "com.mai.oaidviewer.library"
        buildTypes {
            release {
                isMinifyEnabled = false
            }
        }

        block()
    }
    dependencies.add("compileOnly", project.the<VersionCatalogsExtension>().named("libs").findLibrary("androidx-core-ktx").get())
    if (version.isNotEmpty()) {
        dependencies.add("compileOnly", project(":library:impl"))
        dependencies.add("compileOnly", files("$rootDir/Doc/$version/oaid_sdk_$version.aar"))
    }
}

val versionCodes = listOf(
    1023,
    1025,
    1026,
    1027,
    1029,
    1030,
    1100,
    1200,
    1201,
    2000,
    2100,
    2200,
    2300,
    2400,
    2500,
    2501,
    2700
)

fun Project.setupAppModule(block: BaseAppModuleExtension.() -> Unit = {}) {
    setupBaseModule<BaseAppModuleExtension> {
        namespace = "com.mai.oaidviewer"
        defaultConfig {
            applicationId = "com.example.oaidtest2"
            flavorDimensions += "demo"
        }
        buildFeatures {
            compose = true
        }
        buildTypes {
            debug { disableMinify() }
            release { disableMinify() }
        }
        versionCodes.forEach { version ->
            productFlavors.register("lib_$version") {
                dimension = "demo"
                versionCode = version
                versionName = parseVersionName(version)
            }
        }
        block()
    }
    dependencies.apply {
        add("implementation", project(":library:impl"))
        versionCodes.forEach { versionCode ->
            val flavorName = "lib_$versionCode"
            val versionName = parseVersionName(versionCode)
            add(flavorName + "Implementation", project(":library:$flavorName"))
            add(
                flavorName + "Implementation",
                files("$rootDir/Doc/$versionName/oaid_sdk_$versionName.aar")
            )
        }
    }
}

private fun parseVersionName(version: Int) = when {
    version < 1000 -> "0.${version / 100}.${version % 100}"
    version < 2000 -> "1.${version % 1000 / 100}.${version % 100}"
    else -> "${version / 1000}.${version % 1000 / 100}.${version % 100}"
}

private fun ApplicationBuildType.disableMinify() {
    isMinifyEnabled = false
}

private inline fun <reified T : BaseExtension> Project.setupBaseModule(crossinline block: T.() -> Unit = {}) {
    extensions.configure<BaseExtension>("android") {
        compileSdkVersion(35)
        defaultConfig {
            minSdk = 21
            targetSdk = 35
        }
        (this as T).block()
    }
    kotlinExtension.jvmToolchain(17)
}