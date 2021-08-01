package processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

internal class DecoratorConfigProcessorTest {

    @Test
    fun `asdf`() {
        // TODO Need to include Flow as well :(
        val apiSourceFiles = getApiSourceFiles()
        val testDataFile = File(Paths.get("").toAbsolutePath().absolutePathString() + "/src/test/kotlin/processor/TestData.kt")
        val compilation = KotlinCompilation().apply {
            classpaths
            sources = (apiSourceFiles + testDataFile).map { SourceFile.fromPath(it) }
            symbolProcessorProviders = listOf(DecoratorConfigProcessorProvider())
        }

        val result = compilation.compile()

        println(result.generatedFiles.map { it.name })
        println(result.outputDirectory.listFiles().map { it.name })
        println(compilation.kspSourcesDir.listFiles().first().listFiles().map { it.name })
    }

    private fun getApiSourceFiles(): List<File> {
        val apiMainSrcPath = Paths.get("").toAbsolutePath().toFile().parentFile.absolutePath + "/api/src/main"
        return File(apiMainSrcPath).walkTopDown().filter { it.isFile }.toList()
    }

    @Test
    fun `asdfasdf`() {
        File("/Users/janmottl/.gradle/caches/jars-9").walkTopDown().filter { it.isFile }.forEach {
            println(it.name)
        }
    }

}
