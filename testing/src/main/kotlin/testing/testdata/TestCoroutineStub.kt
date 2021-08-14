package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decorator.DecoratorConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

@Suppress("UnusedPrivateMember")
class TestCoroutineStub(private val testCoroutineStubListener: TestCoroutineStubListener) {

    suspend fun rpc(request: String): String {
        val returnVal = "return value"
        testCoroutineStubListener.onRpcCalled(request, returnVal)
        return returnVal
    }

    suspend fun rpcWithMultipleArgs(
        firstArg: String,
        secondArg: Int,
        thirdArg: Boolean,
        fourthArg: Unit
    ): Boolean {
        println("rpcWithMultipleArgs called")
        return false
    }

    suspend fun rpcWithoutArgs(): Int {
        println("rpcWithoutArgs called")
        return 0
    }

    suspend fun rpcReturningUnit(request: String) {
        println("rpcReturningUnit called")
    }

    fun notSupportedFunForDecoration() {
        println("notSupportedFunForDecoration called")
    }

    fun streamingRpc(request: String): Flow<String> {
        val results = mutableListOf<String>()
        return flowOf("first")
            .onEach { results += it }
            .onCompletion { testCoroutineStubListener.onStreamingRpcCalled(request, results) }
    }

    fun streamingRpcWithoutArgs(): Flow<Int> {
        println("streamingRpcWithoutArgs called")
        return flowOf(0, 1, 2)
    }

    fun streamingRpcWithMultipleArgs(
        firstArg: String,
        secondArg: Int,
        thirdArg: Boolean,
        fourthArg: Unit
    ): Flow<Unit> {
        println("streamingRpcWithMultipleArgs called")
        return flowOf(Unit)
    }
}

@DecoratorConfiguration
internal class TestCoroutineStubDecoratorConfig(
    private val decorationProviders: List<Decoration.Provider<*>>,
    private val testCoroutineStubListener: TestCoroutineStubListener
) : DecoratorConfig<TestCoroutineStub> {

    override fun getStub(): TestCoroutineStub {
        return TestCoroutineStub(testCoroutineStubListener)
    }

    override fun getDecorationProviders(): List<Decoration.Provider<*>> {
        return decorationProviders
    }
}

interface TestCoroutineStubListener {

    fun onRpcCalled(request: String, result: String)

    fun onStreamingRpcCalled(request: String, result: List<String>)
}
