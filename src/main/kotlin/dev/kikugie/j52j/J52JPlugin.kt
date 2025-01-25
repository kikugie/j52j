package dev.kikugie.j52j

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.Problems
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.quiltmc.parsers.json.JsonFormat
import org.quiltmc.parsers.json.JsonReader
import org.quiltmc.parsers.json.gson.GsonReader
import java.io.File
import java.io.Reader
import javax.inject.Inject

private val Project.sourceSets: SourceSetContainer? get() = project.findProperty("sourceSets") as? SourceSetContainer
private val SourceSet.taskName: String get() = "j52jConvert${name.replaceFirstChar(Char::uppercaseChar)}Resources"
private fun SourceSet.createj52jTask(project: Project, extension: J52JExtension): TaskProvider<J52JConverter> {
    val dependency = project.tasks.getByName(processResourcesTaskName)
    return project.tasks.register<J52JConverter>(taskName) {
        parameters.set(extension.parameters)
        source(this@createj52jTask)
        dependsOn(dependency)
    }.also {
        dependency.finalizedBy(it)
    }
}

internal fun transform(reader: Reader, args: J52JConverterProperties): String {
    val json = JsonReader.create(reader, JsonFormat.JSON5)
    val gson = JsonParser.parseReader(GsonReader(json))
    return GsonBuilder()
        .apply { if (args.prettyPrinting) setPrettyPrinting() }
        .create().toJson(gson)
}

open class J52JPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create<J52JExtension>("j52j", target)
        configureTaskManagement(target, extension)
    }

    private fun configureTaskManagement(project: Project, extension: J52JExtension) = project.sourceSets?.apply {
        all { if (extension.overriden.isEmpty() || this in extension.overriden) createj52jTask(project, extension) }
        whenObjectRemoved { project.tasks.findByName(taskName)?.let { project.tasks.remove(it) } }
    }
}

abstract class J52JExtension(private val project: Project) {
    internal var overriden: Set<SourceSet> = emptySet()
        private set
    internal var parameters: J52JConverterProperties = J52JConverterProperties()
        private set

    /**Configures parameters for the resulting JSON formatting.*/
    fun params(block: Action<J52JConverterProperties>) {
        parameters = J52JConverterProperties().apply(block::execute)
    }

    /**Overrides, which source sets will be processed.*/
    fun sources(vararg sources: SourceSet) = sources(sources.toList())

    /**Overrides, which source sets will be processed.*/
    fun sources(sources: Iterable<SourceSet>) {
        overriden = sources.toSet()
        val new = overriden
        val old = project.sourceSets?.toSet() ?: emptySet()

        (old - new).mapNotNull { project.tasks.findByName(it.taskName) }.let { project.tasks.removeAll(it) }
        (new - old).forEach { it.createj52jTask(project, this) }
    }
}

@Suppress("UnstableApiUsage")
abstract class J52JConverter : SourceTask() {
    private companion object {
        val PROBLEM_GROUP = object : ProblemGroup {
            override fun getName(): String = "j52j-processing"
            override fun getDisplayName(): String = "J52J Conversions"
            override fun getParent(): ProblemGroup? = null
        }
    }

    @get:Input
    abstract val parameters: Property<J52JConverterProperties>

    @get:Internal
    abstract val files: Property<FileTree>

    @get:Inject
    abstract val problems: Problems

    /**Configures parameters for the resulting JSON formatting.*/
    fun params(block: Action<J52JConverterProperties>) {
        parameters.set(J52JConverterProperties().apply(block::execute))
    }

    /**Appends files for the given [source] to be processed.*/
    fun source(source: SourceSet) {
        // Setup source files for caching
        source.resources.srcDirs.forEach {
            source(it)
        }

        // Setup processed files for transforming
        project.fileTree(source.output.resourcesDir!!).matching {
            include("**/*.json5")
        }.let {
            val tree = files.orNull
            if (tree != null) files.set(tree + it)
            else files.set(it)
        }
    }


    @TaskAction
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    fun run() {
        var exception: Throwable? = null
        for (file in files.get()) processFile(file).onSuccess {
            if (it == null) logger.debug("[J52J] Skipped {}", file)
            else logger.debug("[J52J] Converted {} to {}", file, it)
        }.recoverCatching {
            reportFileProblem(file, it)
        }.onFailure {
            if (exception == null) exception = RuntimeException("[J52J] One or more files failed to be processed")
            exception!!.addSuppressed(it)
        }
        if (exception != null) throw problems.reporter.throwing {
            id("j52j-processing-composite", "J52J Task Error")
            contextualLabel("See messages above for more information")
            withException(exception!!)
        }
    }

    private fun reportFileProblem(file: File, error: Throwable): Nothing = throw problems.reporter.throwing {
        id("j52j-processing-error", "J52J File Conversion Error", PROBLEM_GROUP)
        contextualLabel("An exception occured while '$file' was processed")
        withException(error)
        logger.error("[J52J] Error while processing '$file'", error)
    }

    private fun processFile(file: File) = kotlin.runCatching {
        val properties = J52JFileProperties.read(file)
        if (properties.skip) return@runCatching null
        val filename = "${file.nameWithoutExtension}.${properties.target}"
        val content = transform(file.reader(), parameters.get())
        File(file.parentFile, filename).also {
            it.writeText(content)
            file.delete()
        }
    }
}

class J52JConverterProperties {
    var prettyPrinting: Boolean = false
}

data class J52JFileProperties(val skip: Boolean, val target: String) {
    companion object {
        val DEFAULT = J52JFileProperties(false, "json")
        val PATTERN = Regex("to (\\S+)")
        fun read(file: File): J52JFileProperties = file.useLines { lines ->
            val first = lines.find { it.trimStart().startsWith("//") } ?: return@useLines DEFAULT
            val skip = "no j52j" in first
            val target = PATTERN.find(first)?.groupValues?.getOrNull(1) ?: "json"
            J52JFileProperties(skip, target)
        }
    }
}
