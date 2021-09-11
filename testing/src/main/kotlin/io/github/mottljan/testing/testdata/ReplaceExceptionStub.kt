package io.github.mottljan.testing.testdata

import io.github.mottljan.api.annotation.DecoratorConfiguration
import io.github.mottljan.api.decoration.CustomStrategy
import io.github.mottljan.api.decoration.Decoration
import io.github.mottljan.api.decoration.DispatcherSwappingDecoration
import io.github.mottljan.api.decoration.customStrategy
import io.github.mottljan.api.decorator.DecoratorConfig
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
        replace(Decoration.Id("Non-existing ID")) with DispatcherSwappingDecoration(Dispatchers.IO)
    }
}
