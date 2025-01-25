plugins {
    java
    id("dev.kikugie.j52j")
}

sourceSets {
    val extra by creating {
        compileClasspath += sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
    }
}