pluginManagement {
    includeBuild("..")

    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.quiltmc.org/repository/release") {
            name = "QuiltMC"
        }
    }
}