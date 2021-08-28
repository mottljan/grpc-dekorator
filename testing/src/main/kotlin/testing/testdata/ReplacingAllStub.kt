package testing.testdata

import api.annotation.DecoratorConfiguration
import api.annotation.RpcConfiguration
import api.decoration.Decoration
import api.decoration.replaceAllStrategy
import api.decorator.DecoratorConfig

internal class ReplacingAllStub {

    suspend fun rpc() = Unit

    suspend fun customRpc() = Unit

    suspend fun anotherCustomRpc() = Unit
}

@DecoratorConfiguration
internal class ReplacingAllStubDecoratorConfig(
    private val stubDecorationProviders: List<Decoration.Provider<*>>,
    private val customRpcDecorationProviders: List<Decoration.Provider<*>>,
    private val anotherCustomRpcDecorationProviders: List<Decoration.Provider<*>>,
) : DecoratorConfig<ReplacingAllStub> {

    override fun getStub(): ReplacingAllStub {
        return ReplacingAllStub()
    }

    override fun getStubDecorationStrategy() = replaceAllStrategy {
        stubDecorationProviders.forEach { replaceWith(it) }
    }

    @RpcConfiguration(rpcName = "customRpc")
    fun getCustomRpcStrategy() = replaceAllStrategy {
        customRpcDecorationProviders.forEach { replaceWith(it) }
    }

    @RpcConfiguration(rpcName = "anotherCustomRpc")
    fun getAnotherCustomRpcStrategy() = replaceAllStrategy {
        anotherCustomRpcDecorationProviders.forEach { replaceWith(it) }
    }
}
