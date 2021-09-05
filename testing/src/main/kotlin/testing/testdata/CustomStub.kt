package testing.testdata

import api.annotation.DecoratorConfiguration
import api.annotation.RpcConfiguration
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
    private val replaceStubDecoration: Decoration,
    private val appendStubDecoration: Decoration,
    private val replaceCustomRpcDecoration: Decoration?,
    private val appendCustomRpcDecoration: Decoration?,
) : DecoratorConfig<CustomStub> {

    override fun getStub(): CustomStub {
        return CustomStub()
    }

    override fun getStubDecorationStrategy() = customStrategy {
        removeDecorationWithId(GlobalDecorationA.ID)
        replace(GlobalDecorationB.ID) with replaceStubDecoration
        append(appendStubDecoration)
    }

    @RpcConfiguration(rpcName = "customRpc")
    fun getCustomRpcDecorationStrategy() = customStrategy {
        removeDecorationWithId(appendStubDecoration.id)
        replaceCustomRpcDecoration?.let { replace(replaceStubDecoration.id) with it }
        appendCustomRpcDecoration?.let { append(it) }
    }
}
