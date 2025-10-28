import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

@DisableCachingByDefault
abstract class JavaPathTask : DefaultTask() {

    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    @get:Inject
    abstract val layout: ProjectLayout

    init {
        group = "run"
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun executeTask() {
        val execPath = javaLauncher.get().executablePath
        // create a file symlinked to the java executable for use in scripts
        layout.buildDirectory.asFile.get().mkdirs()
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            val f: File = File(layout.buildDirectory.asFile.get().toString() + "/java_toolchain.bat")
            if (f.exists()) {
                f.delete()
            }
            f.writeText("@" + execPath.asFile.toString() + " %*")
        } else if (Os.isFamily(Os.FAMILY_UNIX)) {
            val f: File = File(layout.buildDirectory.asFile.get().toString() + "/java_toolchain")
            if (f.exists()) {
                f.delete()
            }
            Files.createSymbolicLink(f.toPath(), execPath.asFile.toPath())
        }
    }
}
