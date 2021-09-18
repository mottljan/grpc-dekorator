package io.github.mottljan.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
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
            import io.github.mottljan.api.annotation.DecoratorConfiguration
            
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
        expectedCode: KotlinCompilation.ExitCode?,
        expectedMessage: String
    ) {
        testCompilation(testSourceFileContent) { result ->
            if (expectedCode != null) {
                result.exitCode shouldBeEqualTo expectedCode
            }
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
            import io.github.mottljan.api.annotation.DecoratorConfiguration
            
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
            import io.github.mottljan.api.annotation.DecoratorConfiguration
            import io.github.mottljan.api.decoration.Decoration
            import io.github.mottljan.api.decorator.DecoratorConfig
            
            class Stub {
            
                fun $notSupportedMethodName() {}
            }
            
            @DecoratorConfiguration
            class StubConfig : DecoratorConfig<Stub> {
                
                override fun getStub() = Stub()
            }
        """.trimIndent()

        testLogging(
            testSourceFileContent = testFileContent,
            // TODO Actual result should be OK but due to issue described in class docs it fails incorrectly.
            //  Nullable param is just temporary because of this test to ignore the assertion.
            expectedCode = null,
//            expectedCode = KotlinCompilation.ExitCode.OK,
            expectedMessage = DecoratorProcessor.generateNotGeneratedFunctionWarningMsg(notSupportedMethodName)
        )
    }

    @Test
    fun `error is logged when global decorator config annotation is used for more than one class`() {
        // language=kotlin
        val testFileContent = """
            import io.github.mottljan.api.annotation.GlobalDecoratorConfiguration
            import io.github.mottljan.api.decoration.Decoration
            import io.github.mottljan.api.decorator.GlobalDecoratorConfig

            @GlobalDecoratorConfiguration
            class GlobalConfig : GlobalDecoratorConfig

            @GlobalDecoratorConfiguration
            class GlobalConfig2 : GlobalDecoratorConfig
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
            import io.github.mottljan.api.annotation.GlobalDecoratorConfiguration

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
            import io.github.mottljan.api.annotation.GlobalDecoratorConfiguration

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
    fun `error is logged when custom RPC decoration strategy method declares arguments`() {
        // language=kotlin
        val testFileContent = """
            import io.github.mottljan.api.annotation.DecoratorConfiguration
            import io.github.mottljan.api.annotation.RpcConfiguration
            import io.github.mottljan.api.decoration.Decoration
            import io.github.mottljan.api.decoration.noChangesStrategy
            import io.github.mottljan.api.decorator.DecoratorConfig

            class Stub {
            
                suspend fun rpc()
            }

            @DecoratorConfiguration
            class StubConfig : DecoratorConfig<Stub> {
                
                override fun getStub() = Stub()

                @RpcConfiguration("rpc")
                fun getCustomRpcDecorationStrategy(param: String) = noChangesStrategy()
            }
        """.trimIndent()

        testLogging(
            testSourceFileContent = testFileContent,
            expectedCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            expectedMessage = DecoratorProcessor.RPC_CONFIGURATION_PARAM_ERROR
        )
    }

    @Test
    fun `error is logged when custom RPC decoration strategy method does not return decoration strategy`() {
        // language=kotlin
        val testFileContent = """
            import io.github.mottljan.api.annotation.DecoratorConfiguration
            import io.github.mottljan.api.annotation.RpcConfiguration
            import io.github.mottljan.api.decoration.Decoration
            import io.github.mottljan.api.decorator.DecoratorConfig

            class Stub {
            
                suspend fun rpc()
            }

            @DecoratorConfiguration
            class StubConfig : DecoratorConfig<Stub> {
                
                override fun getStub() = Stub()

                @RpcConfiguration("rpc")
                fun getCustomRpcDecorationStrategy()
            }
        """.trimIndent()

        testLogging(
            testSourceFileContent = testFileContent,
            expectedCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            expectedMessage = DecoratorProcessor.RPC_CONFIGURATION_RETURN_TYPE_ERROR
        )
    }

    @Test
    fun `error is logged when custom RPC decoration strategy method tries to decorate non-existing RPC`() {
        // language=kotlin
        val testFileContent = """
            import io.github.mottljan.api.annotation.DecoratorConfiguration
            import io.github.mottljan.api.annotation.RpcConfiguration
            import io.github.mottljan.api.decoration.Decoration 
            import io.github.mottljan.api.decoration.noChangesStrategy
            import io.github.mottljan.api.decorator.DecoratorConfig

            class Stub {
            
                suspend fun rpc()
            }

            @DecoratorConfiguration
            class StubConfig : DecoratorConfig<Stub> {
                
                override fun getStub() = Stub()

                @RpcConfiguration("nonExistingRpc")
                fun getCustomRpcDecorationStrategy() = noChangesStrategy()
            }
        """.trimIndent()

        testLogging(
            testSourceFileContent = testFileContent,
            expectedCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            expectedMessage = DecoratorProcessor.generateNonExistingRpcError("nonExistingRpc")
        )
    }
}
