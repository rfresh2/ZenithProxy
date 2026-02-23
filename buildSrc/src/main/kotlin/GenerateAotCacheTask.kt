import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.work.DisableCachingByDefault
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@DisableCachingByDefault
abstract class GenerateAotCacheTask : DefaultTask() {

    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    @get:InputFile
    abstract val executableJar: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:InputDirectory
    abstract val workingDir: DirectoryProperty

    @get:Input
    abstract val jvmArgs: ListProperty<String>

    @get:Input
    abstract val startupMarker: Property<String>

    init {
        group = "run"
        description = "Start ZenithProxy to generate Java 25 AOT cache"
    }

    @TaskAction
    fun executeTask() {
        val cacheFile = outputFile.get().asFile
        val jarFile = executableJar.get().asFile
        val marker = startupMarker.get()
        cacheFile.parentFile.mkdirs()
        if (cacheFile.exists()) cacheFile.delete()

        val command = mutableListOf<String>().apply {
            add(javaLauncher.get().executablePath.asFile.absolutePath)
            addAll(jvmArgs.get())
            add("-XX:AOTCacheOutput=${cacheFile.absolutePath}")
            add("-Xlog:aot")
            add("-jar")
            add(jarFile.absolutePath)
        }

        val process = ProcessBuilder(command)
            .directory(workingDir.get().asFile)
            .redirectErrorStream(true)
            .also { it.environment()["ZENITH_DEV"] = "true" }
            .start()

        val sawStartup = AtomicBoolean(false)
        val requestedShutdown = AtomicBoolean(false)
        val outputThread = Thread({
            process.inputStream.bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    println(line)
                    if (line.contains(marker) &&
                        sawStartup.compareAndSet(false, true) &&
                        requestedShutdown.compareAndSet(false, true)
                    ) {
                        runCatching {
                            process.outputStream.writer().use { writer ->
                                writer.write("shutdown")
                                writer.flush()
                            }
                        }
                    }
                }
            }
        }, "generateAotCache-output-reader")
        outputThread.isDaemon = true
        outputThread.start()

        val startupDeadlineMillis = System.currentTimeMillis() + 120_000
        while (!sawStartup.get() && process.isAlive && System.currentTimeMillis() < startupDeadlineMillis) {
            Thread.sleep(100)
        }

        if (!sawStartup.get()) {
            process.destroyForcibly()
            outputThread.join(5_000)
            throw GradleException("ZenithProxy did not reach startup marker; AOT cache was not generated")
        }

        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroy()
        }
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly()
        }
        outputThread.join(5_000)

        if (!cacheFile.isFile) {
            throw GradleException("Expected AOT cache at ${cacheFile.absolutePath}, but it was not created")
        }
    }
}
