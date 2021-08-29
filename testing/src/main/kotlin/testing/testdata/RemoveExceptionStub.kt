package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.AppendAllStrategy
import api.decoration.CustomStrategy
import api.decoration.Decoration
import api.decoration.customStrategy
import api.decorator.DecoratorConfig

/**
 * "Stub" class used for generating decorator for testing purposes. It is focused on testing of
 * throwing exceptions for [CustomStrategy.Action.Remove].
 * This class should be changed with caution since it can break tests.
 */
internal class RemoveExceptionStub {

    suspend fun rpc() = Unit
}

/**
 * [DecoratorConfig] used for testing purposes. It is focused on testing of throwing exceptions
 * for [CustomStrategy.Action.Remove]. This class should be changed with caution since it can
 * break tests.
 */
@DecoratorConfiguration
internal class RemoveExceptionDecoratorConfig : DecoratorConfig<RemoveExceptionStub> {

    override fun getStub(): RemoveExceptionStub {
        return RemoveExceptionStub()
    }

    override fun getStubDecorationStrategy() = customStrategy {
        removeProviderWithId(Decoration.Provider.Id("Non-existing ID"))
    }
}
