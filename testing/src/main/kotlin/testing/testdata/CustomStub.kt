package testing.testdata

import api.annotation.DecoratorConfiguration
import api.annotation.RpcConfiguration
import api.decoration.AppendAllStrategy
import api.decoration.CustomStrategy
import api.decoration.Decoration
import api.decoration.customStrategy
import api.decorator.DecoratorConfig

/**
 * "Stub" class used for generating decorator for testing purposes. It is focused on [CustomStrategy]
 * testing. This class should be changed with caution since it can break tests.
 */
internal class CustomStub {

    suspend fun rpc() = Unit

    suspend fun customRpc() = Unit
}

/**
 * [DecoratorConfig] used for testing purposes. It is focused on [CustomStrategy] testing.
 * This class should be changed with caution since it can break tests.
 */
@DecoratorConfiguration
internal class CustomStubDecoratorConfig(
    private val replaceStubProvider: Decoration.Provider<*>,
    private val appendStubProvider: Decoration.Provider<*>,
    private val replaceCustomRpcProvider: Decoration.Provider<*>?,
    private val appendCustomRpcProvider: Decoration.Provider<*>?,
) : DecoratorConfig<CustomStub> {

    override fun getStub(): CustomStub {
        return CustomStub()
    }

    override fun getStubDecorationStrategy() = customStrategy {
        removeProviderWithId(GlobalDecorationA.Provider.ID)
        replace(GlobalDecorationB.Provider.ID) with replaceStubProvider
        append(appendStubProvider)
    }

    @RpcConfiguration(rpcName = "customRpc")
    fun getCustomRpcDecorationStrategy() = customStrategy {
        removeProviderWithId(appendStubProvider.id)
        replaceCustomRpcProvider?.let { replace(replaceStubProvider.id) with it }
        appendCustomRpcProvider?.let { append(it) }
    }
}
