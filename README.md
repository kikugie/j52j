# J52J
*AKA "Json5 to Json"*

Simple Gradle plugin that converts `.json5` files into `.json` when gradle processes resources.

> [!WARNING]
> You can't use `fabric.mod.json5` with this plugin.
> Fabric Loom reads `fabric.mod.json` directly from the source code and doesn't work with the `.json5` format.

## Setup
Add the plugin using
```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven("https://maven.kikugie.dev/releases")
    }
}
// build.gradle.kts
plugins {
    id("dev.kikugie.j52j") version "1.0.2"
}
```

```groovy
// settings.gradle
pluginManagement {
    repositories {
        maven { url = "https://maven.kikugie.dev/releases"}
    }
}
// build.gradle
plugins {
    id "dev.kikugie.j52j" version "1.0.2"
}
```

## Configuring the plugin
By default, it will process all source sets in the project, marked as `resources`. 
However, you can specify which source sets it should process:
```kotlin
// build.gradle[.kts]
j52j {
    sources(sourceSets["main"])
}
```

If you would like a specific file to be kept as `.json5`, add `// no j52j` at the top:
```json5
// no j52j
{
  json: 5
}
```
