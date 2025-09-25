pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("myLibs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "WomenSafetyApp2" // Or your actual project name
include(":app")