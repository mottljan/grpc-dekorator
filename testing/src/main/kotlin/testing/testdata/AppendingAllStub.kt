package testing.testdata

import api.annotation.DecoratorConfiguration
import api.annotation.RpcConfiguration
import api.decoration.Decoration
import api.decoration.appendAllStrategy
import api.decorator.DecoratorConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

@Suppress("UnusedPrivateMember")
class AppendingAllStub(private val testCoroutineStubListener: TestCoroutineStubListener) {

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

    suspend fun customDecorationsRpc() {}
}

@DecoratorConfiguration
class AppendingAllStubDecoratorConfig(
    private val stubDecorationProviders: List<Decoration.Provider<*>>,
    private val customRpcDecorationProviders: List<Decoration.Provider<*>>,
    private val testCoroutineStubListener: TestCoroutineStubListener
) : DecoratorConfig<AppendingAllStub> {

    override fun getStub(): AppendingAllStub {
        return AppendingAllStub(testCoroutineStubListener)
    }

    override fun getStubDecorationStrategy() = appendAllStrategy {
        stubDecorationProviders.forEach { append(it) }
    }

    @RpcConfiguration(rpcName = "customDecorationsRpc")
    fun getCustomRpcStrategy() = appendAllStrategy {
        customRpcDecorationProviders.forEach { append(it) }
    }
}
