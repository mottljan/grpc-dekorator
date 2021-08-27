package testing.testdata

import api.annotation.DecoratorConfiguration
import api.annotation.RpcConfiguration
import api.decoration.Decoration
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

    override fun getStubDecorationStrategy() = Decoration.Strategy.replaceAll {
        stubDecorationProviders.forEach { replaceWith(it) }
    }

    @RpcConfiguration(rpcName = "customRpc")
    fun getCustomRpcStrategy() = Decoration.Strategy.replaceAll {
        customRpcDecorationProviders.forEach { replaceWith(it) }
    }

    @RpcConfiguration(rpcName = "anotherCustomRpc")
    fun getAnotherCustomRpcStrategy() = Decoration.Strategy.replaceAll {
        anotherCustomRpcDecorationProviders.forEach { replaceWith(it) }
    }
}
