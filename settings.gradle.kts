pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
/*dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "1.9.0")
        }
    }
}*/
rootProject.name = "Pix-EzViewer"
include(":app")
include(":ketch")
include(":BRVAH")
