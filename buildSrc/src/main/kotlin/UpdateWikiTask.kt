import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class UpdateWikiTask : DefaultTask() {

    @get:OutputDirectory
    abstract val wikiDirectory: RegularFileProperty

    @get:InputFiles
    abstract val wikiFiles: ConfigurableFileCollection

    @get:Inject
    abstract val layout: ProjectLayout

    init {
        group = "build"
        description = "Update Wiki Docs"
    }

    @TaskAction
    fun executeTask() {
        val wikiDir = wikiDirectory.get().asFile
        if (!wikiDir.exists()) {
            return
        }

        wikiFiles.forEach { file ->
            val destFile = File(wikiDir, file.name)
            if (destFile.exists()) {
                var existing = destFile.readText(Charsets.UTF_8)
                val newContent = file.readText(Charsets.UTF_8)
                if (existing == newContent) {
                    return;
                }
            }
            file.copyTo(destFile, overwrite = true)
            println("Updated ${file.name} wiki")
        }
    }
}
