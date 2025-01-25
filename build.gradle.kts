plugins {
    java
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.0"
}

group = "dev.kikugie"
version = "2.0"

repositories {
    mavenCentral()
    maven("https://maven.quiltmc.org/repository/release") {
        name = "QuiltMC"
    }
}

dependencies {
    implementation("org.quiltmc.parsers:gson:0.2.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
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
