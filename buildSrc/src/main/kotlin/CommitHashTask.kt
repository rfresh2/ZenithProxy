import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class CommitHashTask : DefaultTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val exec: ExecOperations

    init {
        group = "build"
        description = "Write commit hash / version to file"
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun executeTask() {
        val out = ByteArrayOutputStream()
        exec.exec {
            commandLine = "git rev-parse --short=8 HEAD".split(" ")
            isIgnoreExitValue = true
            standardOutput = out
            workingDir = layout.projectDirectory.asFile
        }
        kotlin.runCatching {
            val commitHash = out.toString().trim()
            if (commitHash.length > 5) {
                outputFile.get().asFile.apply {
                    parentFile.mkdirs()
                    println("Writing commit hash: $commitHash")
                    writeText(commitHash)
                }
            } else {
                throw IllegalStateException("Invalid commit hash: $commitHash")
            }
        }.exceptionOrNull()?.let {
            println("Unable to determine commit hash: ${it.message}")
        }
    }
}
