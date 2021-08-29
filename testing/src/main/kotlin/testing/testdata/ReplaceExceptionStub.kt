package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.CustomStrategy
import api.decoration.Decoration
import api.decoration.DispatcherSwappingDecoration
import api.decoration.customStrategy
import api.decorator.DecoratorConfig
import kotlinx.coroutines.Dispatchers

/**
 * "Stub" class used for generating decorator for testing purposes. It is focused on testing of
 * throwing exceptions for [CustomStrategy.Action.Replace].
 * This class should be changed with caution since it can break tests.
 */
internal class ReplaceExceptionStub {

    suspend fun rpc() = Unit
}

/**
 * [DecoratorConfig] used for testing purposes. It is focused on testing of throwing exceptions
 * for [CustomStrategy.Action.Replace]. This class should be changed with caution since it can
 * break tests.
 */
@DecoratorConfiguration
internal class ReplaceExceptionDecoratorConfig : DecoratorConfig<ReplaceExceptionStub> {

    override fun getStub(): ReplaceExceptionStub {
        return ReplaceExceptionStub()
    }

    override fun getStubDecorationStrategy() = customStrategy {
        val provider = DispatcherSwappingDecoration.Provider(Decoration.InitStrategy.FACTORY, Dispatchers.IO)
        replace(Decoration.Provider.Id("Non-existing ID")) with provider
    }
}
