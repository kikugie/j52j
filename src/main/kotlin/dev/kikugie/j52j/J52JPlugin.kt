package dev.kikugie.j52j

import blue.endless.jankson.Jankson
import groovy.lang.MissingPropertyException
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.filter
import org.gradle.kotlin.dsl.register
import java.io.File
import java.io.FilterReader
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

open class J52JPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("j52j", J52JExtension::class.java, target)
        target.afterEvaluate {
            extension.sourcesImpl.forEach {
                val name = "j52jConvert${it.name.replaceFirstChar(Char::uppercaseChar)}Resources"
                val processResources = tasks.getByName(it.processResourcesTaskName)

                tasks.register<J52JConverter>(name) {
                    source.set(it)
                    onlyIf { !processResources.state.upToDate }
                }
                processResources.finalizedBy(name)
            }
        }
    }
}

@Suppress("LeakingThis")
abstract class J52JExtension(private val project: Project) {
    abstract val sources: ListProperty<SourceSet>
    internal val sourcesImpl: List<SourceSet>
        get() = sources.orNull ?: try {
            (project.property("sourceSets") as SourceSetContainer).toList()
        } catch (_: MissingPropertyException) {
            emptyList()
        }

    init {
        sources.convention(null)
    }

    fun sources(vararg sources: SourceSet) {
        this.sources.set(sources.toList())
    }
}

abstract class J52JConverter : DefaultTask() {
    @get:Input
    abstract val source: Property<SourceSet>

    @OptIn(ExperimentalPathApi::class)
    @TaskAction
    private fun run() {
        val root = source.get().output.resourcesDir ?: throw IllegalStateException("No output resource dir")
        // Stupid fucking ass gradle
        root.toPath().walk(PathWalkOption.INCLUDE_DIRECTORIES).filter(Files::isDirectory).forEach { path ->
            val dir = path.toFile()
            project.copy {
                filteringCharset = "UTF-8"
                from(dir)
                include("*.json5")
                exclude { startsWithKey(it.file) }
                filter<J52JProcessor>()
                rename { it.substring(0, it.lastIndex) } // Epic 5 removal
                into(dir)
            }
            project.delete {
                delete(project.fileTree(dir) {
                    include("*.json5")
                    exclude { startsWithKey(it.file) }
                })
            }
        }
    }
}

class J52JProcessor(input: Reader) : FilterReader(convertReader(input))

private val key = "no j52j"
private val jankson = Jankson.builder().build()
private fun convertReader(input: Reader): Reader {
    val text = input.use(Reader::readText)
    return jankson.load(text).toJson().reader()
}

private fun startsWithKey(file: File) = file.useLines { lines -> startsWithKey(lines.firstOrNull()) }
private fun startsWithKey(line: String?) = line == null || line.trimStart().removePrefix("//").trimStart() == key