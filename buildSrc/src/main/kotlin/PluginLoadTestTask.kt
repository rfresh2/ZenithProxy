import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import java.io.BufferedOutputStream
import javax.inject.Inject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

abstract class PluginLoadTestTask : DefaultTask() {

    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    @get:InputFiles
    abstract val classpath: ConfigurableFileCollection

    @get:Input
    abstract val mainClass: Property<String>

    @get:Input
    abstract val jvmArgs: ListProperty<String>

    @get:InputDirectory
    abstract val workingDir: RegularFileProperty

    @get:Inject
    abstract val layout: ProjectLayout

    init {
        group = "verification"
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun exec() {
        // Build the command to launch the application similarly to JavaExec
        val javaPath = javaLauncher.get().executablePath.asFile.absolutePath
        val cp = checkNotNull(classpath) { "Classpath must be configured" }.asPath
        val main = checkNotNull(mainClass.orNull) { "mainClass must be configured" }

        val jvm = (jvmArgs.get() ?: emptyList()).map { it.toString() }

        val command = mutableListOf<String>().apply {
            add(javaPath)
            addAll(jvm)
            add("-cp")
            add(cp)
            add(main)
        }

        val pb = ProcessBuilder(command)
            .directory(workingDir.asFile.get() ?: layout.projectDirectory.asFile)
            .redirectErrorStream(false) // Capture stdout only, as requested

        logger.lifecycle("[pluginLoadTest] Starting application: ${command.joinToString(" ")}")

        val process = try {
            pb.start()
        } catch (e: Exception) {
            throw GradleException("Failed to start process for PluginLoadTest", e)
        }

        val startedMarker = "ZenithProxy started!"
        val failureMarker = "Plugin Load Failure"
        val successMarker = "Plugin Loaded"

        val capturedLines = mutableListOf<String>()
        val started = AtomicBoolean(false)

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val readerThread = Thread {
            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line!!
                    capturedLines.add(l)
                    logger.lifecycle(l)
                    if (!started.get() && l.contains(startedMarker)) {
                        started.set(true)
                    }
                }
            } catch (_: Exception) {
                // ignore reader exceptions when process is destroyed
            } finally {
                try { reader.close() } catch (_: Exception) {}
            }
        }
        readerThread.isDaemon = true
        readerThread.start()

        // Wait for the started marker with a timeout
        val timeout = Duration.ofMinutes(1)
        val waitUntil = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout.toMillis())
        while (!started.get() && System.nanoTime() < waitUntil) {
            try { Thread.sleep(100) } catch (_: InterruptedException) { break }
        }

        val sawStarted = started.get()

        // Stop the process regardless
        try {
            process.destroy()
            if (process.isAlive) process.destroyForcibly()
        } catch (_: Exception) { }

        try { readerThread.join(5000) } catch (_: InterruptedException) { }

        if (!sawStarted) {
            throw GradleException("Timed out waiting for '$startedMarker' in application output")
        }

        // Analyze output prior to the start marker
        val startIndex = capturedLines.indexOfFirst { it.contains(startedMarker) }
        val linesBeforeStart = if (startIndex >= 0) capturedLines.subList(0, startIndex) else capturedLines

        val failures = linesBeforeStart.count { it.contains(failureMarker) }
        val successes = linesBeforeStart.count { it.contains(successMarker) }

        logger.lifecycle("[pluginLoadTest] Detected $successes successful plugin load(s) and $failures failure(s) before start")

        if (failures > 0) {
            throw GradleException("PluginLoadTest failed: detected $failures plugin load failure(s). See output above for details.")
        }

        logger.lifecycle("[pluginLoadTest] No plugin load failures detected. Task successful.")

    }

}
