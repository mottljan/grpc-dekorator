package testing.testdata

import api.annotation.DecoratorConfiguration
import api.annotation.RpcConfiguration
import api.decoration.Decoration
import api.decoration.customStrategy
import api.decorator.DecoratorConfig

internal class CustomStub {

    suspend fun rpc() = Unit

    suspend fun customRpc() = Unit
}

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
        removeWithId(GlobalDecorationA.Provider.ID)
        replace(GlobalDecorationB.Provider.ID) with replaceStubProvider
        append(appendStubProvider)
    }

    @RpcConfiguration(rpcName = "customRpc")
    fun getCustomRpcDecorationStrategy() = customStrategy {
        removeWithId(appendStubProvider.id)
        replaceCustomRpcProvider?.let { replace(replaceStubProvider.id) with it }
        appendCustomRpcProvider?.let { append(it) }
    }
}
