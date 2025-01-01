plugins {
    java
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.24"
}

group = "dev.kikugie"
version = "1.0.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("blue.endless:jankson:1.2.3")
}

kotlin {
    jvmToolchain(16)
}

publishing {
    repositories {
        maven {
            name = "kikugieMaven"
            url = uri("https://maven.kikugie.dev/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create("basic", BasicAuthentication::class)
            }
        }
    }

    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = "j52j"
            version = project.version.toString()
            artifact(tasks.getByName("jar"))
        }
    }
}

gradlePlugin {
    website = "https://github.com/kikugie/j52j"
    vcsUrl = "https://github.com/kikugie/j52j"

    plugins {
        create("stonecutter") {
            id = "dev.kikugie.j52j"
            implementationClass = "dev.kikugie.j52j.J52JPlugin"
            displayName = "j52j"
            description = "Json5 to Json resource processing plugin"
        }
    }
}
