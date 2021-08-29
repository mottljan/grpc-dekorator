package testing.testdata

import api.annotation.DecoratorConfiguration
import api.annotation.RpcConfiguration
import api.decoration.AppendAllStrategy
import api.decoration.Decoration
import api.decoration.ReplaceAllStrategy
import api.decoration.replaceAllStrategy
import api.decorator.DecoratorConfig

/**
 * "Stub" class used for generating decorator for testing purposes. It is focused on
 * [ReplaceAllStrategy] testing. This class should be changed with caution since it can break tests.
 */
internal class ReplacingAllStub {

    suspend fun rpc() = Unit

    suspend fun customRpc() = Unit

    suspend fun anotherCustomRpc() = Unit
}

/**
 * [DecoratorConfig] used for testing purposes. It is focused on [ReplaceAllStrategy] testing.
 * This class should be changed with caution since it can break tests.
 */
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
