# J52J
*AKA "Json5 to Json"*

Simple Gradle plugin that converts `.json5` files into `.json` when Gradle processes resources.

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
    id("dev.kikugie.j52j") version "2.0"
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
    id "dev.kikugie.j52j" version "2.0"
}
```

## Configuring the plugin
### Global configuration
```kotlin
// build.gradle[.kts]
j52j {
    /* Overrides sources processed by the plugin.
    By default, it dynamically adds all registered sources,
    so this is not required unless you want some sources to not be processed.*/
    sources(sourceSets["main"])
    
    params {
        /* Enables indentation in the processed JSON files.
        Due to limitations of Gson, the indent can only be two spaces.*/
        prettyPrinting = true // default: false
    }
}
```
## Per-file configuration
File properties are configured in a header comment:
```json5
// this is a header
{
  json: 5
}
```
There are the following parameters that can be put in the header:
- `// no j52j` - Skips processing for the file, keeping it in JSON5 format.
- `// to mcmeta` (or other) - Specifies a different file extension to be used after conversion. The extension must not start with a dot.