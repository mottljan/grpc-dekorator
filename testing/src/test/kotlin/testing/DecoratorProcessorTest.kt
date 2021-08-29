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
import testing.testdata.globalDecorationAProvider
import testing.testdata.globalDecorationBProvider
import testing.testdata.globalDecorationCProvider
import java.io.File
import java.nio.file.Paths
import kotlin.streams.toList

/**
 * Tests mainly behaviour of the generated decorators by executing generated decorator directly instead
 * of examining generated code
 */
@ExperimentalCoroutinesApi
class DecoratorProcessorTest : CoroutineTest() {

    private val firstDecorationProvider = TestDecoration.Provider()
    private val secondDecorationProvider = TestDecoration.Provider()

    private val expectedGeneratedTestDecoratorName = "${AppendingAllStub::class.simpleName}Decorator"

    private val stubListener = StubListener()

    private val appendingAllDecoratorWithDecorations = createAppendingAllDecorator(listOf(firstDecorationProvider, secondDecorationProvider))

    @AfterEach
    fun afterEach() {
        globalDecorationAProvider.clearTimes()
        globalDecorationBProvider.clearTimes()
        globalDecorationCProvider.clearTimes()
    }

    private fun createAppendingAllDecorator(
        stubDecorationProviders: List<Decoration.Provider<*>>,
        customRpcDecorationProviders: List<Decoration.Provider<*>> = emptyList()
    ): AppendingAllStubDecorator {
        return AppendingAllStubDecorator(
            createGlobalDecoratorConfig(),
            AppendingAllStubDecoratorConfig(
                stubDecorationProviders = stubDecorationProviders,
                customRpcDecorationProviders = customRpcDecorationProviders,
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

        val firstDecorationTime = firstDecorationProvider.lastSuspendFunDecorationNanoTime
        val secondDecorationTime = secondDecorationProvider.lastSuspendFunDecorationNanoTime
        firstDecorationTime shouldBeLessThan secondDecorationTime
    }

    @Test
    fun `decorations are applied in correct order for reversed order for suspend fun`() = testDispatcher.runBlockingTest {
        val underTest = createAppendingAllDecorator(listOf(secondDecorationProvider, firstDecorationProvider))

        underTest.rpc("")

        val firstDecorationTime = firstDecorationProvider.lastSuspendFunDecorationNanoTime
        val secondDecorationTime = secondDecorationProvider.lastSuspendFunDecorationNanoTime
        secondDecorationTime shouldBeLessThan firstDecorationTime
    }

    @Test
    fun `decorations are applied in correct order for streaming fun`() = testDispatcher.runBlockingTest {
        appendingAllDecoratorWithDecorations.streamingRpc("").collect()

        val firstDecorationTime = firstDecorationProvider.lastStreamingFunDecorationNanoTime
        val secondDecorationTime = secondDecorationProvider.lastStreamingFunDecorationNanoTime
        firstDecorationTime shouldBeLessThan secondDecorationTime
    }

    @Test
    fun `decorations are applied in correct order for reversed order for streaming fun`() = testDispatcher.runBlockingTest {
        val decorator = createAppendingAllDecorator(listOf(secondDecorationProvider, firstDecorationProvider))

        decorator.streamingRpc("").collect()

        val firstDecorationTime = firstDecorationProvider.lastStreamingFunDecorationNanoTime
        val secondDecorationTime = secondDecorationProvider.lastStreamingFunDecorationNanoTime
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
    fun `new instance of decoration is created for each RPC invocation when requested with init strategy`() = testDispatcher.runBlockingTest {
        var createdInstancesCount = 0
        val provider = TestDecoration.Provider(initStrategy = Decoration.InitStrategy.FACTORY) {
            createdInstancesCount++
            TestDecoration()
        }
        val underTest = createAppendingAllDecorator(listOf(provider))

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
        val provider = TestDecoration.Provider(initStrategy = initStrategy) {
            createdInstancesCount++
            TestDecoration()
        }
        val underTest = createAppendingAllDecorator(listOf(provider))

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
    fun `stub's decorations are appended to the global ones for appendAll strategy`() = testDispatcher.runBlockingTest {
        val underTest = createAppendingAllDecorator(listOf(firstDecorationProvider))
        val timeBeforeRpcCall = System.nanoTime()

        underTest.rpc("")

        val lastGlobalDecorationCallTime = assertThatAllGlobalDecorationsWerePreserved(timeBeforeRpcCall)
        firstDecorationProvider.lastSuspendFunDecorationNanoTime shouldBeGreaterThan lastGlobalDecorationCallTime
    }

    /**
     * Assert that all global decorations were called in the correct order and returns the call time
     * of the last one for further possible assertions
     */
    private fun assertThatAllGlobalDecorationsWerePreserved(timeBeforeRpcCall: Long): Long {
        val globalDecorationACallTime = globalDecorationAProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationACallTime shouldBeGreaterThan timeBeforeRpcCall

        val globalDecorationBCallTime = globalDecorationBProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationBCallTime shouldBeGreaterThan globalDecorationACallTime

        val globalDecorationCCallTime = globalDecorationCProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationCCallTime shouldBeGreaterThan globalDecorationBCallTime

        return globalDecorationCCallTime
    }

    @Test
    fun `stub's decorations replace all global ones for replaceAll strategy`() = testDispatcher.runBlockingTest {
        val underTest = createReplacingAllDecorator(stubDecorationProviders = listOf(firstDecorationProvider))
        val timeBeforeRpcCall = System.nanoTime()

        underTest.rpc()

        assertThatNoGlobalDecorationWasCalled(timeBeforeRpcCall)
        firstDecorationProvider.lastSuspendFunDecorationNanoTime shouldBeGreaterThan timeBeforeRpcCall
    }

    private fun createReplacingAllDecorator(
        stubDecorationProviders: List<Decoration.Provider<*>>,
        customRpcDecorationProviders: List<Decoration.Provider<*>> = emptyList(),
        anotherCustomRpcDecorationProviders: List<Decoration.Provider<*>> = emptyList(),
    ): ReplacingAllStubDecorator {
        return ReplacingAllStubDecorator(
            createGlobalDecoratorConfig(),
            ReplacingAllStubDecoratorConfig(
                stubDecorationProviders = stubDecorationProviders,
                customRpcDecorationProviders = customRpcDecorationProviders,
                anotherCustomRpcDecorationProviders = anotherCustomRpcDecorationProviders
            )
        )
    }

    private fun assertThatNoGlobalDecorationWasCalled(timeBeforeRpcCall: Long) {
        val globalDecorationACallTime = globalDecorationAProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationACallTime shouldBeLessThan timeBeforeRpcCall // global decoration was not called -> was removed

        val globalDecorationBCallTime = globalDecorationBProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationBCallTime shouldBeLessThan timeBeforeRpcCall // global decoration was not called -> was removed

        val globalDecorationCCallTime = globalDecorationCProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationCCallTime shouldBeLessThan timeBeforeRpcCall // global decoration was not called -> was removed
    }

    @Test
    fun `stub's decorations are modified in custom way for custom strategy`() = testDispatcher.runBlockingTest {
        // Arrange
        val underTest = createCustomDecorator(
            replaceStubProvider = firstDecorationProvider,
            appendStubProvider = secondDecorationProvider
        )
        val timeBeforeRpcCall = System.nanoTime()

        // Act
        underTest.rpc()

        // Assert
        // global decoration was not called -> was removed
        val globalDecorationACallTime = globalDecorationAProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationACallTime shouldBeLessThan timeBeforeRpcCall

        // global decoration was not called -> was removed
        val globalDecorationBCallTime = globalDecorationBProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationBCallTime shouldBeLessThan timeBeforeRpcCall

        // was called first -> replaced second global decoration
        val firstStubDecorationCallTime = firstDecorationProvider.lastSuspendFunDecorationNanoTime
        firstStubDecorationCallTime shouldBeGreaterThan timeBeforeRpcCall

        // was called second -> when first stub's decoration replaced second global one, positions were preserved
        val globalDecorationCCallTime = globalDecorationCProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationCCallTime shouldBeGreaterThan firstStubDecorationCallTime

        // was called on the last place, it was appended at the end
        secondDecorationProvider.lastSuspendFunDecorationNanoTime shouldBeGreaterThan globalDecorationCCallTime
    }

    private fun createCustomDecorator(
        replaceStubProvider: Decoration.Provider<*>,
        appendStubProvider: Decoration.Provider<*>,
        replaceCustomRpcProvider: Decoration.Provider<*>? = null,
        appendCustomRpcProvider: Decoration.Provider<*>? = null,
    ): CustomStubDecorator {
        return CustomStubDecorator(
            createGlobalDecoratorConfig(),
            CustomStubDecoratorConfig(
                replaceStubProvider = replaceStubProvider,
                appendStubProvider = appendStubProvider,
                replaceCustomRpcProvider = replaceCustomRpcProvider,
                appendCustomRpcProvider = appendCustomRpcProvider
            )
        )
    }

    @Test
    fun `throws exception when custom strategy tries to remove provider which does not exist`() {
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
    fun `throws exception when custom strategy tries to replace provider which does not exist`() {
        testExceptionThrowing { globalDecoratorConfig ->
            ReplaceExceptionStubDecorator(globalDecoratorConfig, ReplaceExceptionDecoratorConfig())
        }
    }

    @Test
    fun `rpc's decorations are appended to the higher level ones for appendAll strategy`() = testDispatcher.runBlockingTest {
        // Arrange
        val stubsDecorationProvider = firstDecorationProvider
        val customRpcDecorationProvider = secondDecorationProvider
        val underTest = createAppendingAllDecorator(
            stubDecorationProviders = listOf(stubsDecorationProvider),
            customRpcDecorationProviders = listOf(customRpcDecorationProvider)
        )
        val timeBeforeRpcCall = System.nanoTime()

        // Act
        underTest.customDecorationsRpc()

        // Assert
        val stubDecorationCallTime = stubsDecorationProvider.lastSuspendFunDecorationNanoTime

        val lastGlobalDecorationCallTime = assertThatAllGlobalDecorationsWerePreserved(timeBeforeRpcCall)
        stubDecorationCallTime shouldBeGreaterThan lastGlobalDecorationCallTime
        customRpcDecorationProvider.lastSuspendFunDecorationNanoTime shouldBeGreaterThan stubDecorationCallTime
    }

    @Test
    fun `rpc's decorations replace all higher level ones for replaceAll strategy`() = testDispatcher.runBlockingTest {
        val stubsDecorationProvider = firstDecorationProvider
        val customRpcDecorationProvider = secondDecorationProvider
        val underTest = createReplacingAllDecorator(
            stubDecorationProviders = listOf(stubsDecorationProvider),
            customRpcDecorationProviders = listOf(customRpcDecorationProvider)
        )
        val timeBeforeRpcCall = System.nanoTime()

        underTest.customRpc()

        assertThatNoGlobalDecorationWasCalled(timeBeforeRpcCall)
        stubsDecorationProvider.lastSuspendFunDecorationNanoTime shouldBeLessThan timeBeforeRpcCall // Stub's decoration was not called
        customRpcDecorationProvider.lastSuspendFunDecorationNanoTime shouldBeGreaterThan timeBeforeRpcCall
    }

    @Test
    fun `rpc's decorations are modified in custom way for custom strategy`() = testDispatcher.runBlockingTest {
        // Arrange
        val replaceStubProvider = TestDecoration.Provider(Decoration.Provider.Id("replaceStubProvider"))
        val appendStubProvider = TestDecoration.Provider(Decoration.Provider.Id("appendStubProvider"))
        val replaceRpcProvider = TestDecoration.Provider(Decoration.Provider.Id("replaceRpcProvider"))
        val appendRpcProvider = TestDecoration.Provider(Decoration.Provider.Id("appendRpcProvider"))
        val underTest = createCustomDecorator(
            replaceStubProvider = replaceStubProvider,
            appendStubProvider = appendStubProvider,
            replaceCustomRpcProvider = replaceRpcProvider,
            appendCustomRpcProvider = appendRpcProvider
        )
        val timeBeforeRpcCall = System.nanoTime()

        // Act
        underTest.customRpc()

        // Assert
        // globalDecorationA was not called -> was removed
        val globalDecorationACallTime = globalDecorationAProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationACallTime shouldBeLessThan timeBeforeRpcCall

        // globalDecorationB was not called -> was removed
        val globalDecorationBCallTime = globalDecorationBProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationBCallTime shouldBeLessThan timeBeforeRpcCall

        // replaceStubDecoration was not called -> was removed
        val replaceStubDecorationCallTime = replaceStubProvider.lastSuspendFunDecorationNanoTime
        replaceStubDecorationCallTime shouldBeLessThan timeBeforeRpcCall

        // appendStubDecoration was not called -> was removed
        val appendStubDecorationCallTime = appendStubProvider.lastSuspendFunDecorationNanoTime
        appendStubDecorationCallTime shouldBeLessThan timeBeforeRpcCall

        // was called first -> replaced replaceStubDecoration
        val replaceRpcDecorationCallTime = replaceRpcProvider.lastSuspendFunDecorationNanoTime
        replaceRpcDecorationCallTime shouldBeGreaterThan timeBeforeRpcCall

        // was called second -> replaced replaceStubDecoration
        val globalDecorationCCallTime = globalDecorationCProvider.getDecoration().suspendFunDecorationNanoTime
        globalDecorationCCallTime shouldBeGreaterThan replaceRpcDecorationCallTime

        // was called on the last place, it was appended at the end
        appendRpcProvider.lastSuspendFunDecorationNanoTime shouldBeGreaterThan globalDecorationCCallTime
    }

    @Test
    fun `rpc's custom decorations are applied to correct rpc when there is more custom rpcs decorations`() = testDispatcher.runBlockingTest {
        // Arrange
        val customRpcDecorationProvider = TestDecoration.Provider()
        val anotherCustomRpcDecorationProvider = TestDecoration.Provider()
        val underTest = createReplacingAllDecorator(
            stubDecorationProviders = listOf(TestDecoration.Provider()),
            customRpcDecorationProviders = listOf(customRpcDecorationProvider),
            anotherCustomRpcDecorationProviders = listOf(anotherCustomRpcDecorationProvider),
        )
        val timeBeforeRpcCall = System.nanoTime()

        // Act
        underTest.customRpc()

        // Assert
        customRpcDecorationProvider.lastSuspendFunDecorationNanoTime shouldBeGreaterThan timeBeforeRpcCall
        anotherCustomRpcDecorationProvider.lastSuspendFunDecorationNanoTime shouldBeLessThan timeBeforeRpcCall // Not called yet

        // Act - We call this after first assert to reliably verify that decorations for particular RPCs were called correctly
        underTest.anotherCustomRpc()

        // Assert
        anotherCustomRpcDecorationProvider.lastSuspendFunDecorationNanoTime shouldBeGreaterThan timeBeforeRpcCall
    }

    /**
     * Helps with testing of [Decoration] execution by recording the time of the execution which
     * can be used both for verifying that the [Decoration] was called and also verifying the order
     * of [Decoration]s.
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
            override val id: Id = ID,
            initStrategy: Decoration.InitStrategy = Decoration.InitStrategy.SINGLETON,
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

            companion object {

                val ID = Id(Provider::class.qualifiedName!!)
            }
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
