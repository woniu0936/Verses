// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
    alias(libs.plugins.binary.compatibility.validator)
}

apiValidation {
    ignoredProjects.add("verses-sample")
}

tasks.register("setupGitHooks", Exec::class) {
    description = "Configures git hooks from .githooks directory"
    group = "help"
    commandLine("git", "config", "core.hooksPath", ".githooks")
}

// Automatically setup hooks on build if they are not configured
tasks.named("prepareKotlinBuildScriptModel") {
    dependsOn("setupGitHooks")
}