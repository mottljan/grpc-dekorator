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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import stub.decorator.wtf.TestCoroutineStubDecorator
import testing.extension.CoroutineTest
import testing.testdata.InternalCoroutineStub
import testing.testdata.TestCoroutineStub
import testing.testdata.TestCoroutineStubDecoratorConfig
import testing.testdata.TestCoroutineStubListener
import testing.testdata.TestGlobalDecoratorConfig
import testing.testdata.globalDecorationProvider
import java.io.File
import java.nio.file.Paths
import kotlin.streams.toList

/**
 * Tests mainly behaviour of the generated decorator by executing generated decorator directly
 */
@ExperimentalCoroutinesApi
class DecoratorProcessorTest : CoroutineTest() {

    private val firstDecorationProvider = TestDecoration.Provider(Decoration.InitStrategy.SINGLETON)
    private val secondDecorationProvider = TestDecoration.Provider(Decoration.InitStrategy.SINGLETON)

    private val expectedGeneratedTestDecoratorName = "${TestCoroutineStub::class.simpleName}Decorator"

    private val stubListener = StubListener()

    private val testDecoratorWithDecorations = createTestDecorator(listOf(firstDecorationProvider, secondDecorationProvider))

    @AfterEach
    fun afterEach() {
        globalDecorationProvider.clearTimes()
    }

    private fun createTestDecorator(decorationProviders: List<Decoration.Provider<*>>): TestCoroutineStubDecorator {
        return TestCoroutineStubDecorator(
            TestGlobalDecoratorConfig(),
            TestCoroutineStubDecoratorConfig(
                decorationProviders = decorationProviders,
                testCoroutineStubListener = stubListener
            )
        )
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
        testDecoratorWithDecorations.rpc("")

        val firstDecorationTime = firstDecorationProvider.lastSuspendFunDecorationNanoTime
        val secondDecorationTime = secondDecorationProvider.lastSuspendFunDecorationNanoTime
        firstDecorationTime shouldBeLessThan secondDecorationTime
    }

    @Test
    fun `decorations are applied in correct order for reversed order for suspend fun`() = testDispatcher.runBlockingTest {
        val underTest = createTestDecorator(listOf(secondDecorationProvider, firstDecorationProvider))

        underTest.rpc("")

        val firstDecorationTime = firstDecorationProvider.lastSuspendFunDecorationNanoTime
        val secondDecorationTime = secondDecorationProvider.lastSuspendFunDecorationNanoTime
        secondDecorationTime shouldBeLessThan firstDecorationTime
    }

    @Test
    fun `decorations are applied in correct order for streaming fun`() = testDispatcher.runBlockingTest {
        testDecoratorWithDecorations.streamingRpc("").collect()

        val firstDecorationTime = firstDecorationProvider.lastStreamingFunDecorationNanoTime
        val secondDecorationTime = secondDecorationProvider.lastStreamingFunDecorationNanoTime
        firstDecorationTime shouldBeLessThan secondDecorationTime
    }

    @Test
    fun `decorations are applied in correct order for reversed order for streaming fun`() = testDispatcher.runBlockingTest {
        val decorator = createTestDecorator(listOf(secondDecorationProvider, firstDecorationProvider))

        decorator.streamingRpc("").collect()

        val firstDecorationTime = firstDecorationProvider.lastStreamingFunDecorationNanoTime
        val secondDecorationTime = secondDecorationProvider.lastStreamingFunDecorationNanoTime
        secondDecorationTime shouldBeLessThan firstDecorationTime
    }

    @Test
    fun `correct suspend RPC called with correct params and return value when decorations are applied`() = testDispatcher.runBlockingTest {
        testCorrectRpcCall(testDecoratorWithDecorations)
    }

    private suspend fun testCorrectRpcCall(testCoroutineStubDecorator: TestCoroutineStubDecorator) {
        val expectedRequest = "expectedRequest"

        val actualResult = testCoroutineStubDecorator.rpc(expectedRequest)

        stubListener.rpcRequestResult.request shouldBeEqualTo expectedRequest
        actualResult shouldBeEqualTo stubListener.rpcRequestResult.result
    }

    @Test
    fun `correct streaming RPC called with correct params and return value when decorations are applied`() = testDispatcher.runBlockingTest {
        testCorrectStreamingRpcCall(testDecoratorWithDecorations)
    }

    private suspend fun testCorrectStreamingRpcCall(testCoroutineStubDecorator: TestCoroutineStubDecorator) {
        val expectedRequest = "expectedRequest"

        val actualResults = mutableListOf<String>()
        testCoroutineStubDecorator.streamingRpc(expectedRequest).toCollection(actualResults)

        stubListener.streamingRpcRequestResult.request shouldBeEqualTo expectedRequest
        actualResults shouldBeEqualTo stubListener.streamingRpcRequestResult.result
    }

    @Test
    fun `correct suspend RPC called with correct params and return value when no decorations are applied`() = testDispatcher.runBlockingTest {
        testCorrectRpcCall(createTestDecoratorWithNoDecorations())
    }

    private fun createTestDecoratorWithNoDecorations(): TestCoroutineStubDecorator {
        return createTestDecorator(emptyList())
    }

    @Test
    fun `correct streaming RPC called with correct params and return value when no decorations are applied`() = testDispatcher.runBlockingTest {
        testCorrectStreamingRpcCall(createTestDecoratorWithNoDecorations())
    }

    @Test
    fun `new instance of decoration is created for each RPC invocation when requested with init strategy`() = testDispatcher.runBlockingTest {
        var createdInstancesCount = 0
        val provider = TestDecoration.Provider(Decoration.InitStrategy.FACTORY) {
            createdInstancesCount++
            TestDecoration()
        }
        val underTest = createTestDecorator(listOf(provider))

        underTest.rpc("")
        underTest.rpc("")

        createdInstancesCount shouldBeEqualTo 2
    }

    @Test
    fun `single instance of decoration is created right away for all RPC invocations when requested with init strategy`() {
        testDispatcher.runBlockingTest {
            testOnlyOneDecorationInstanceCreation(Decoration.InitStrategy.SINGLETON)
        }
    }

    private suspend fun testOnlyOneDecorationInstanceCreation(initStrategy: Decoration.InitStrategy) {
        var createdInstancesCount = 0
        val provider = TestDecoration.Provider(initStrategy) {
            createdInstancesCount++
            TestDecoration()
        }
        val underTest = createTestDecorator(listOf(provider))

        createdInstancesCount shouldBeEqualTo if (initStrategy == Decoration.InitStrategy.SINGLETON) 1 else 0

        underTest.rpc("")
        underTest.rpc("")

        createdInstancesCount shouldBeEqualTo 1
    }

    @Test
    fun `single instance of decoration is created lazily when requested with init strategy and first RPC is called`() {
        testDispatcher.runBlockingTest {
            testOnlyOneDecorationInstanceCreation(Decoration.InitStrategy.LAZY)
        }
    }

    @Test
    fun `global decoration is included automatically as first stub's decoration for suspend fun`() = testDispatcher.runBlockingTest {
        val underTest = createTestDecorator(listOf(firstDecorationProvider))
        val timeBeforeRpcCall = System.nanoTime()

        underTest.rpc("")

        val globalDecorationCallTime = globalDecorationProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationCallTime shouldBeGreaterThan timeBeforeRpcCall
        firstDecorationProvider.lastSuspendFunDecorationNanoTime shouldBeGreaterThan globalDecorationCallTime
    }

    /**
     * Helps with testing of [Decoration] execution
     */
    private class TestDecoration : Decoration {

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

        class Provider(
            initStrategy: Decoration.InitStrategy,
            factory: () -> TestDecoration = ::TestDecoration
        ) : Decoration.Provider<TestDecoration>(initStrategy, factory) {

            /**
             * Returns time of the last suspend fun decoration. If [Decoration.InitStrategy.FACTORY]
             * is set, multiple invocations return different results.
             */
            val lastSuspendFunDecorationNanoTime get() = getDecoration().suspendFunDecorationNanoTime

            /**
             * Returns time of the last streaming fun decoration. If [Decoration.InitStrategy.FACTORY]
             * is set, multiple invocations return different results.
             */
            val lastStreamingFunDecorationNanoTime get() = getDecoration().streamingFunDecorationNanoTime
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
