import com.mai.build_logic.setupLibraryModule

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("build-logic")
}

setupLibraryModule("1.0.27") {
    namespace = "com.mai.oaidviewer.library"
}