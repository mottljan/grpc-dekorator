package processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.Test

/**
 * Tests error and warning logging of [DecoratorProcessor]. Test of the correct behaviour (generation)
 * is tested in a separate :testing module where properly generated classes are tested as black boxes
 * instead of examining generated code using [KotlinCompilation]. Moreover, there is some issue
 * (https://github.com/google/ksp/issues/427) with testing KSP using [KotlinCompilation] anyway.
 */
class DecoratorProcessorLoggingTest {

    @Test
    fun `compilation fails when configuration annotation is applied to property`() {
        // language=kotlin
        val testFileContent = """
            import api.annotation.DecoratorConfiguration
            
            @DecoratorConfiguration
            val config: Int = 0 
        """.trimIndent()

        testLogging(
            testSourceFileContent = testFileContent,
            expectedCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            expectedMessage = DecoratorProcessor.DECORATOR_CONFIGURATION_ERROR
        )
    }

    private fun testLogging(
        testSourceFileContent: String,
        expectedCode: KotlinCompilation.ExitCode,
        expectedMessage: String
    ) {
        testCompilation(testSourceFileContent) { result ->
            result.exitCode shouldBeEqualTo expectedCode
            result.messages shouldContain expectedMessage
        }
    }

    private fun testCompilation(testSourceFileContent: String, assert: (KotlinCompilation.Result) -> Unit) {
        val testFile = SourceFile.kotlin("Test.kt", testSourceFileContent)
        val compilation = KotlinCompilation().apply {
            inheritClassPath = true
            sources = listOf(testFile)
            symbolProcessorProviders = listOf(DecoratorProcessor.Provider())
        }

        val result = compilation.compile()

        assert(result)
    }

    @Test
    fun `compilation fails when class annotated with config annotation does not implement config interface as well`() {
        // language=
        val configClassName = "StubConfig"
        // language=kotlin
        val testFileContent = """
            import api.annotation.DecoratorConfiguration
            
            @DecoratorConfiguration
            class $configClassName 
        """.trimIndent()

        testLogging(
            testSourceFileContent = testFileContent,
            expectedCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            expectedMessage = DecoratorProcessor.generateMissingInterfaceImplErrorMsg(configClassName)
        )
    }

    @Test
    fun `warning is logged when stub contains method which is not supported for decoration`() {
        // language=
        val notSupportedMethodName = "notSupportedMethodForDecoration"
        // language=kotlin
        val testFileContent = """
            import api.annotation.DecoratorConfiguration
            import api.decorator.DecoratorConfig
            
            class Stub {
            
                fun $notSupportedMethodName() {}
            }
            
            @DecoratorConfiguration
            class StubConfig : DecoratorConfig<Stub> {
                
                override fun provideStub() = Stub()
    
                override fun provideDecorations() = emptyList()
            }
        """.trimIndent()

        testLogging(
            testSourceFileContent = testFileContent,
            // TODO Actual result should be OK but due to issue described in class docs it fails incorrectly.
//            expectedCode = KotlinCompilation.ExitCode.OK,
            expectedCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            expectedMessage = DecoratorProcessor.generateNotGeneratedFunctionWarningMsg(notSupportedMethodName)
        )
    }

    @Test
    fun `error is logged when global decorator config annotation is used for more than one class`() {
        // language=kotlin
        val testFileContent = """
            import api.annotation.GlobalDecoratorConfiguration
            import api.decoration.Decoration
            import api.decorator.GlobalDecoratorConfig

            @GlobalDecoratorConfiguration
            class GlobalConfig : GlobalDecoratorConfig {
            
                override val decorationProviders: List<Decoration.Provider<*>> = emptyList()
            }

            @GlobalDecoratorConfiguration
            class GlobalConfig2 : GlobalDecoratorConfig {
            
                override val decorationProviders: List<Decoration.Provider<*>> = emptyList()
            }
        """.trimIndent()

        testLogging(
            testSourceFileContent = testFileContent,
            expectedCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            expectedMessage = DecoratorProcessor.GLOBAL_DECORATOR_CONFIGURATION_TOO_MANY_ERROR
        )
    }

    @Test
    fun `error is logged when global decorator config annotation annotates property`() {
        // language=kotlin
        val testFileContent = """
            import api.annotation.GlobalDecoratorConfiguration

            @GlobalDecoratorConfiguration
            val globalConfig = ""
        """.trimIndent()

        testLogging(
            testSourceFileContent = testFileContent,
            expectedCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            expectedMessage = DecoratorProcessor.GLOBAL_DECORATOR_CONFIGURATION_CLASS_KIND_ERROR
        )
    }

    @Test
    fun `error is logged when global decorator config annotation annotates class which does not implement required interface`() {
        // language=kotlin
        val testFileContent = """
            import api.annotation.GlobalDecoratorConfiguration

            @GlobalDecoratorConfiguration
            class GlobalConfig
        """.trimIndent()

        testLogging(
            testSourceFileContent = testFileContent,
            expectedCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            expectedMessage = DecoratorProcessor.GLOBAL_DECORATOR_CONFIGURATION_IMPL_ERROR
        )
    }

    @Test
    fun `error is logged when global decorator config class implements property without backing field`() {
        // language=kotlin
        val testFileContent = """
            import api.annotation.GlobalDecoratorConfiguration
            import api.decoration.Decoration
            import api.decorator.GlobalDecoratorConfig

            @GlobalDecoratorConfiguration
            class GlobalConfig : GlobalDecoratorConfig {
            
                override val decorationProviders: List<Decoration.Provider<*>> get() = emptyList()
            }
        """.trimIndent()

        testLogging(
            testSourceFileContent = testFileContent,
            expectedCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            expectedMessage = DecoratorProcessor.GLOBAL_DECORATOR_CONFIGURATION_PROPERTY_ERROR
        )
    }
}
