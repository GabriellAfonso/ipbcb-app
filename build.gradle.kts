// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Use o alias que definimos no TOML para o KSP
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false

}

// Redirect build output to a container-local path on Linux to avoid cross-platform cache conflicts.
// Windows (Android Studio) keeps the default build/ dir inside the project (shared volume).
// Linux (container) writes to /home/node/.gradle-builds/ which is NOT on the shared volume.
val isLinux = System.getProperty("os.name").contains("Linux", ignoreCase = true)
if (isLinux) {
    allprojects {
        layout.buildDirectory.set(file("/home/node/.gradle-builds/${rootProject.name}/${project.name}"))
    }
}