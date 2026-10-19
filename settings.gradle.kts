pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            content {
                includeGroupAndSubgroups("com.github.jied83")
            }
        }
    }
}

rootProject.name = "termin-android"

include(":shared", ":androidApp")
