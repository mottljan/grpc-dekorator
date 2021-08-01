package processor

import kotlinx.coroutines.flow.flowOf
import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decoration.DispatcherSwappingDecoration
import api.decorator.DecoratorConfig
import kotlinx.coroutines.Dispatchers

class TestCoroutineStub {

    suspend fun rpc(request: String) = ""

    suspend fun rpcWithMultipleArgs(
        firstArg: String,
        secondArg: Int,
        thirdArg: Boolean,
        fourthArg: Unit
    ) = false

    suspend fun rpcWithoutArgs() = 0

    suspend fun rpcReturningUnit(request: String) {}

    fun notSupportedFunForDecoration() {}

    fun streamingRpc(request: String) = flowOf("first")

    fun streamingRpcWithoutArgs() = flowOf(0, 1, 2)

    fun streamingRpcWithMultipleArgs(
        firstArg: String,
        secondArg: Int,
        thirdArg: Boolean,
        fourthArg: Unit
    ) = flowOf(Unit)
}

@DecoratorConfiguration
class TestCoroutineStubDecoratorConfig : DecoratorConfig<TestCoroutineStub> {

    override fun provideStub(): TestCoroutineStub {
        return TestCoroutineStub()
    }

    override fun provideDecorations(): List<Decoration> {
        return listOf(DispatcherSwappingDecoration(Dispatchers.IO))
    }
}
