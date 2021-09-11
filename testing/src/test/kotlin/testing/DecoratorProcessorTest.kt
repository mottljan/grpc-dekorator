package testing

import api.decoration.Decoration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import stub.decorator.wtf.AppendingAllStubDecorator
import stub.decorator.wtf.CustomStubDecorator
import stub.decorator.wtf.RemoveExceptionStubDecorator
import stub.decorator.wtf.ReplaceExceptionStubDecorator
import stub.decorator.wtf.ReplacingAllStubDecorator
import testing.extension.CoroutineTest
import testing.testdata.AppendingAllStub
import testing.testdata.AppendingAllStubDecoratorConfig
import testing.testdata.CustomStubDecoratorConfig
import testing.testdata.InternalCoroutineStub
import testing.testdata.RemoveExceptionDecoratorConfig
import testing.testdata.ReplaceExceptionDecoratorConfig
import testing.testdata.ReplacingAllStubDecoratorConfig
import testing.testdata.TestCoroutineStubListener
import testing.testdata.TestGlobalDecoratorConfig
import testing.testdata.globalDecorationA
import testing.testdata.globalDecorationB
import testing.testdata.globalDecorationC
import java.io.File
import java.nio.file.Paths
import kotlin.streams.toList

/**
 * Tests mainly behaviour of the generated decorators by executing generated decorator directly instead
 * of examining generated code
 */
@ExperimentalCoroutinesApi
class DecoratorProcessorTest : CoroutineTest() {

    private val firstDecoration = TestDecoration()
    private val secondDecoration = TestDecoration()

    private val expectedGeneratedTestDecoratorName = "${AppendingAllStub::class.simpleName}Decorator"

    private val stubListener = StubListener()

    private val appendingAllDecoratorWithDecorations = createAppendingAllDecorator(listOf(firstDecoration, secondDecoration))

    @AfterEach
    fun afterEach() {
        globalDecorationA.clearTimes()
        globalDecorationB.clearTimes()
        globalDecorationC.clearTimes()
    }

    private fun createAppendingAllDecorator(
        stubDecorations: List<Decoration>,
        customRpcDecorations: List<Decoration> = emptyList()
    ): AppendingAllStubDecorator {
        return AppendingAllStubDecorator(
            createGlobalDecoratorConfig(),
            AppendingAllStubDecoratorConfig(
                stubDecorations = stubDecorations,
                customRpcDecorations = customRpcDecorations,
                testCoroutineStubListener = stubListener
            )
        )
    }

    private fun createGlobalDecoratorConfig(onProcessException: (Exception) -> Unit = {}): TestGlobalDecoratorConfig {
        return TestGlobalDecoratorConfig(onProcessException)
    }

    @Test
    fun `name of the generated decorator is correct`() {
        val generatedDecoratorFile = findGeneratedTestDecorator()

        generatedDecoratorFile.exists() shouldBe true
    }

    private fun findGeneratedTestDecorator(): File {
        return findGeneratedDecorator(expectedGeneratedTestDecoratorName)
    }

    private fun findGeneratedDecorator(decoratorName: String): File {
        val generatedKspFilesPath = "${Paths.get("").toAbsolutePath()}/build/generated/ksp"
        val expectedFileName = "$decoratorName.kt"
        val allGeneratedFiles = File(generatedKspFilesPath).walkTopDown()
        val generatedDecoratorFile = allGeneratedFiles.find { it.name == expectedFileName }
        if (generatedDecoratorFile == null) {
            val allFilesString = allGeneratedFiles.filter { it.isFile }
                .map { it.name }
                .joinToString("\n")
            throw IllegalStateException("$expectedFileName was not generated!\n\nGenerated files:\n$allFilesString")
        }
        return generatedDecoratorFile
    }

    @Test
    fun `generated decorator has public visibility modifier when stub has that too`() {
        val generatedDecoratorFile = findGeneratedTestDecorator()

        generatedDecoratorFile.bufferedReader().lines().toList().forEach { line ->
            if (line.contains("class")) {
                val hasPublicModifier = line.startsWith("class") || line.startsWith("public")
                hasPublicModifier shouldBe true
                return
            }
        }
    }

    @Test
    fun `generated decorator has internal visibility modifier when stub has that too`() {
        val generatedDecoratorFile = findGeneratedDecorator("${InternalCoroutineStub::class.simpleName}Decorator")

        generatedDecoratorFile.bufferedReader().lines().toList().forEach { line ->
            if (line.contains("class")) {
                val hasPublicModifier = line.startsWith("internal")
                hasPublicModifier shouldBe true
                return
            }
        }
    }

    @Test
    fun `decorations are applied in correct order for suspend fun`() = testDispatcher.runBlockingTest {
        appendingAllDecoratorWithDecorations.rpc("")

        val firstDecorationTime = firstDecoration.suspendFunDecorationNanoTime
        val secondDecorationTime = secondDecoration.suspendFunDecorationNanoTime
        firstDecorationTime shouldBeLessThan secondDecorationTime
    }

    @Test
    fun `decorations are applied in correct order for reversed order for suspend fun`() = testDispatcher.runBlockingTest {
        val underTest = createAppendingAllDecorator(listOf(secondDecoration, firstDecoration))

        underTest.rpc("")

        val firstDecorationTime = firstDecoration.suspendFunDecorationNanoTime
        val secondDecorationTime = secondDecoration.suspendFunDecorationNanoTime
        secondDecorationTime shouldBeLessThan firstDecorationTime
    }

    @Test
    fun `decorations are applied in correct order for streaming fun`() = testDispatcher.runBlockingTest {
        appendingAllDecoratorWithDecorations.streamingRpc("").collect()

        val firstDecorationTime = firstDecoration.streamingFunDecorationNanoTime
        val secondDecorationTime = secondDecoration.streamingFunDecorationNanoTime
        firstDecorationTime shouldBeLessThan secondDecorationTime
    }

    @Test
    fun `decorations are applied in correct order for reversed order for streaming fun`() = testDispatcher.runBlockingTest {
        val decorator = createAppendingAllDecorator(listOf(secondDecoration, firstDecoration))

        decorator.streamingRpc("").collect()

        val firstDecorationTime = firstDecoration.streamingFunDecorationNanoTime
        val secondDecorationTime = secondDecoration.streamingFunDecorationNanoTime
        secondDecorationTime shouldBeLessThan firstDecorationTime
    }

    @Test
    fun `correct suspend RPC called with correct params and return value when decorations are applied`() = testDispatcher.runBlockingTest {
        testCorrectRpcCall(appendingAllDecoratorWithDecorations)
    }

    private suspend fun testCorrectRpcCall(appendingAllStubDecorator: AppendingAllStubDecorator) {
        val expectedRequest = "expectedRequest"

        val actualResult = appendingAllStubDecorator.rpc(expectedRequest)

        stubListener.rpcRequestResult.request shouldBeEqualTo expectedRequest
        actualResult shouldBeEqualTo stubListener.rpcRequestResult.result
    }

    @Test
    fun `correct streaming RPC called with correct params and return value when decorations are applied`() = testDispatcher.runBlockingTest {
        testCorrectStreamingRpcCall(appendingAllDecoratorWithDecorations)
    }

    private suspend fun testCorrectStreamingRpcCall(appendingAllStubDecorator: AppendingAllStubDecorator) {
        val expectedRequest = "expectedRequest"

        val actualResults = mutableListOf<String>()
        appendingAllStubDecorator.streamingRpc(expectedRequest).toCollection(actualResults)

        stubListener.streamingRpcRequestResult.request shouldBeEqualTo expectedRequest
        actualResults shouldBeEqualTo stubListener.streamingRpcRequestResult.result
    }

    @Test
    fun `correct suspend RPC called with correct params and return value when no decorations are applied`() = testDispatcher.runBlockingTest {
        testCorrectRpcCall(createTestDecoratorWithNoDecorations())
    }

    private fun createTestDecoratorWithNoDecorations(): AppendingAllStubDecorator {
        return createAppendingAllDecorator(emptyList())
    }

    @Test
    fun `correct streaming RPC called with correct params and return value when no decorations are applied`() = testDispatcher.runBlockingTest {
        testCorrectStreamingRpcCall(createTestDecoratorWithNoDecorations())
    }

    @Test
    fun `stub's decorations are appended to the global ones for appendAll strategy`() = testDispatcher.runBlockingTest {
        val underTest = createAppendingAllDecorator(listOf(firstDecoration))
        val timeBeforeRpcCall = System.nanoTime()

        underTest.rpc("")

        val lastGlobalDecorationCallTime = assertThatAllGlobalDecorationsWerePreserved(timeBeforeRpcCall)
        firstDecoration.suspendFunDecorationNanoTime shouldBeGreaterThan lastGlobalDecorationCallTime
    }

    /**
     * Assert that all global decorations were called in the correct order and returns the call time
     * of the last one for further possible assertions
     */
    private fun assertThatAllGlobalDecorationsWerePreserved(timeBeforeRpcCall: Long): Long {
        val globalDecorationACallTime = globalDecorationA.suspendFunDecorationNanoTime
        globalDecorationACallTime shouldBeGreaterThan timeBeforeRpcCall

        val globalDecorationBCallTime = globalDecorationB.suspendFunDecorationNanoTime
        globalDecorationBCallTime shouldBeGreaterThan globalDecorationACallTime

        val globalDecorationCCallTime = globalDecorationC.suspendFunDecorationNanoTime
        globalDecorationCCallTime shouldBeGreaterThan globalDecorationBCallTime

        return globalDecorationCCallTime
    }

    @Test
    fun `stub's decorations replace all global ones for replaceAll strategy`() = testDispatcher.runBlockingTest {
        val underTest = createReplacingAllDecorator(stubDecorations = listOf(firstDecoration))
        val timeBeforeRpcCall = System.nanoTime()

        underTest.rpc()

        assertThatNoGlobalDecorationWasCalled(timeBeforeRpcCall)
        firstDecoration.suspendFunDecorationNanoTime shouldBeGreaterThan timeBeforeRpcCall
    }

    private fun createReplacingAllDecorator(
        stubDecorations: List<Decoration>,
        customRpcDecorations: List<Decoration> = emptyList(),
        anotherCustomRpcDecorations: List<Decoration> = emptyList(),
    ): ReplacingAllStubDecorator {
        return ReplacingAllStubDecorator(
            createGlobalDecoratorConfig(),
            ReplacingAllStubDecoratorConfig(
                stubDecorations = stubDecorations,
                customRpcDecorations = customRpcDecorations,
                anotherCustomRpcDecorations = anotherCustomRpcDecorations
            )
        )
    }

    private fun assertThatNoGlobalDecorationWasCalled(timeBeforeRpcCall: Long) {
        val globalDecorationACallTime = globalDecorationA.suspendFunDecorationNanoTime
        globalDecorationACallTime shouldBeLessThan timeBeforeRpcCall // global decoration was not called -> was removed

        val globalDecorationBCallTime = globalDecorationB.suspendFunDecorationNanoTime
        globalDecorationBCallTime shouldBeLessThan timeBeforeRpcCall // global decoration was not called -> was removed

        val globalDecorationCCallTime = globalDecorationC.suspendFunDecorationNanoTime
        globalDecorationCCallTime shouldBeLessThan timeBeforeRpcCall // global decoration was not called -> was removed
    }

    @Test
    fun `stub's decorations are modified in custom way for custom strategy`() = testDispatcher.runBlockingTest {
        // Arrange
        val underTest = createCustomDecorator(
            replaceStubDecoration = firstDecoration,
            appendStubDecoration = secondDecoration
        )
        val timeBeforeRpcCall = System.nanoTime()

        // Act
        underTest.rpc()

        // Assert
        // global decoration was not called -> was removed
        val globalDecorationACallTime = globalDecorationA.suspendFunDecorationNanoTime
        globalDecorationACallTime shouldBeLessThan timeBeforeRpcCall

        // global decoration was not called -> was removed
        val globalDecorationBCallTime = globalDecorationB.suspendFunDecorationNanoTime
        globalDecorationBCallTime shouldBeLessThan timeBeforeRpcCall

        // was called first -> replaced second global decoration
        val firstStubDecorationCallTime = firstDecoration.suspendFunDecorationNanoTime
        firstStubDecorationCallTime shouldBeGreaterThan timeBeforeRpcCall

        // was called second -> when first stub's decoration replaced second global one, positions were preserved
        val globalDecorationCCallTime = globalDecorationC.suspendFunDecorationNanoTime
        globalDecorationCCallTime shouldBeGreaterThan firstStubDecorationCallTime

        // was called on the last place, it was appended at the end
        secondDecoration.suspendFunDecorationNanoTime shouldBeGreaterThan globalDecorationCCallTime
    }

    private fun createCustomDecorator(
        replaceStubDecoration: Decoration,
        appendStubDecoration: Decoration,
        replaceCustomRpcDecoration: Decoration? = null,
        appendCustomRpcDecoration: Decoration? = null,
    ): CustomStubDecorator {
        return CustomStubDecorator(
            createGlobalDecoratorConfig(),
            CustomStubDecoratorConfig(
                replaceStubDecoration = replaceStubDecoration,
                appendStubDecoration = appendStubDecoration,
                replaceCustomRpcDecoration = replaceCustomRpcDecoration,
                appendCustomRpcDecoration = appendCustomRpcDecoration
            )
        )
    }

    @Test
    fun `throws exception when custom strategy tries to remove decoration which does not exist`() {
        testExceptionThrowing { globalDecoratorConfig ->
            RemoveExceptionStubDecorator(globalDecoratorConfig, RemoveExceptionDecoratorConfig())
        }
    }

    private fun testExceptionThrowing(act: (TestGlobalDecoratorConfig) -> Unit) {
        var actualException: Exception? = null
        val globalDecoratorConfig = createGlobalDecoratorConfig { actualException = it }

        act(globalDecoratorConfig)

        actualException shouldNotBe null
    }

    @Test
    fun `throws exception when custom strategy tries to replace decoration which does not exist`() {
        testExceptionThrowing { globalDecoratorConfig ->
            ReplaceExceptionStubDecorator(globalDecoratorConfig, ReplaceExceptionDecoratorConfig())
        }
    }

    @Test
    fun `rpc's decorations are appended to the higher level ones for appendAll strategy`() = testDispatcher.runBlockingTest {
        // Arrange
        val stubsDecoration = firstDecoration
        val customRpcDecoration = secondDecoration
        val underTest = createAppendingAllDecorator(
            stubDecorations = listOf(stubsDecoration),
            customRpcDecorations = listOf(customRpcDecoration)
        )
        val timeBeforeRpcCall = System.nanoTime()

        // Act
        underTest.customDecorationsRpc(underTest.customDecorationsRpcDecorations)

        // Assert
        val stubDecorationCallTime = stubsDecoration.suspendFunDecorationNanoTime

        val lastGlobalDecorationCallTime = assertThatAllGlobalDecorationsWerePreserved(timeBeforeRpcCall)
        stubDecorationCallTime shouldBeGreaterThan lastGlobalDecorationCallTime
        customRpcDecoration.suspendFunDecorationNanoTime shouldBeGreaterThan stubDecorationCallTime
    }

    @Test
    fun `rpc's decorations replace all higher level ones for replaceAll strategy`() = testDispatcher.runBlockingTest {
        val stubsDecoration = firstDecoration
        val customRpcDecoration = secondDecoration
        val underTest = createReplacingAllDecorator(
            stubDecorations = listOf(stubsDecoration),
            customRpcDecorations = listOf(customRpcDecoration)
        )
        val timeBeforeRpcCall = System.nanoTime()

        underTest.customRpc(underTest.customRpcDecorations)

        assertThatNoGlobalDecorationWasCalled(timeBeforeRpcCall)
        stubsDecoration.suspendFunDecorationNanoTime shouldBeLessThan timeBeforeRpcCall // Stub's decoration was not called
        customRpcDecoration.suspendFunDecorationNanoTime shouldBeGreaterThan timeBeforeRpcCall
    }

    @Test
    fun `rpc's decorations are modified in custom way for custom strategy`() = testDispatcher.runBlockingTest {
        // Arrange
        val replaceStubDecoration = TestDecoration(Decoration.Id("replaceStubDecoration"))
        val appendStubDecoration = TestDecoration(Decoration.Id("appendStubDecoration"))
        val replaceRpcDecoration = TestDecoration(Decoration.Id("replaceRpcDecoration"))
        val appendRpcDecoration = TestDecoration(Decoration.Id("appendRpcDecoration"))
        val underTest = createCustomDecorator(
            replaceStubDecoration = replaceStubDecoration,
            appendStubDecoration = appendStubDecoration,
            replaceCustomRpcDecoration = replaceRpcDecoration,
            appendCustomRpcDecoration = appendRpcDecoration
        )
        val timeBeforeRpcCall = System.nanoTime()

        // Act
        underTest.customRpc(underTest.customRpcDecorations)

        // Assert
        // globalDecorationA was not called -> was removed
        val globalDecorationACallTime = globalDecorationA.suspendFunDecorationNanoTime
        globalDecorationACallTime shouldBeLessThan timeBeforeRpcCall

        // globalDecorationB was not called -> was removed
        val globalDecorationBCallTime = globalDecorationB.suspendFunDecorationNanoTime
        globalDecorationBCallTime shouldBeLessThan timeBeforeRpcCall

        // replaceStubDecoration was not called -> was removed
        val replaceStubDecorationCallTime = replaceStubDecoration.suspendFunDecorationNanoTime
        replaceStubDecorationCallTime shouldBeLessThan timeBeforeRpcCall

        // appendStubDecoration was not called -> was removed
        val appendStubDecorationCallTime = appendStubDecoration.suspendFunDecorationNanoTime
        appendStubDecorationCallTime shouldBeLessThan timeBeforeRpcCall

        // was called first -> replaced replaceStubDecoration
        val replaceRpcDecorationCallTime = replaceRpcDecoration.suspendFunDecorationNanoTime
        replaceRpcDecorationCallTime shouldBeGreaterThan timeBeforeRpcCall

        // was called second -> replaced replaceStubDecoration
        val globalDecorationCCallTime = globalDecorationC.suspendFunDecorationNanoTime
        globalDecorationCCallTime shouldBeGreaterThan replaceRpcDecorationCallTime

        // was called on the last place, it was appended at the end
        appendRpcDecoration.suspendFunDecorationNanoTime shouldBeGreaterThan globalDecorationCCallTime
    }

    @Test
    fun `rpc's custom decorations are applied to correct rpc when there is more custom rpcs decorations`() = testDispatcher.runBlockingTest {
        // Arrange
        val customRpcDecoration = TestDecoration()
        val anotherCustomRpcDecoration = TestDecoration()
        val underTest = createReplacingAllDecorator(
            stubDecorations = listOf(TestDecoration()),
            customRpcDecorations = listOf(customRpcDecoration),
            anotherCustomRpcDecorations = listOf(anotherCustomRpcDecoration),
        )
        val timeBeforeRpcCall = System.nanoTime()

        // Act
        underTest.customRpc(underTest.customRpcDecorations)

        // Assert
        customRpcDecoration.suspendFunDecorationNanoTime shouldBeGreaterThan timeBeforeRpcCall
        anotherCustomRpcDecoration.suspendFunDecorationNanoTime shouldBeLessThan timeBeforeRpcCall // Not called yet

        // Act - We call this after first assert to reliably verify that decorations for particular RPCs were called correctly
        underTest.anotherCustomRpc(underTest.anotherCustomRpcDecorations)

        // Assert
        anotherCustomRpcDecoration.suspendFunDecorationNanoTime shouldBeGreaterThan timeBeforeRpcCall
    }

    /**
     * Helps with testing of [Decoration] execution by recording the time of the execution which
     * can be used both for verifying that the [Decoration] was called and also verifying the order
     * of [Decoration]s.
     */
    private class TestDecoration(override val id: Decoration.Id = ID) : Decoration {

        var suspendFunDecorationNanoTime = 0L
            private set

        var streamingFunDecorationNanoTime = 0L
            private set

        override suspend fun <Response> decorate(rpc: suspend () -> Response): Response {
            suspendFunDecorationNanoTime = System.nanoTime()
            return rpc()
        }

        override fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response> {
            streamingFunDecorationNanoTime = System.nanoTime()
            return rpc()
        }

        companion object {

            val ID = Decoration.Id(TestDecoration::class.qualifiedName!!)
        }
    }

    /**
     * Helps with testing of invocations of RPCs
     */
    private class StubListener : TestCoroutineStubListener {

        lateinit var rpcRequestResult: RequestResult<String>
            private set

        lateinit var streamingRpcRequestResult: RequestResult<List<String>>
            private set

        override fun onRpcCalled(request: String, result: String) {
            rpcRequestResult = RequestResult(request = request, result = result)
        }

        override fun onStreamingRpcCalled(request: String, result: List<String>) {
            streamingRpcRequestResult = RequestResult(request = request, result = result)
        }

        data class RequestResult<Result>(val request: String, val result: Result)
    }
}
