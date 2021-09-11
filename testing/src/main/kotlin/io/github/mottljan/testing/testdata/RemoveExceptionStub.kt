package io.github.mottljan.testing.testdata

import io.github.mottljan.api.annotation.DecoratorConfiguration
import io.github.mottljan.api.decoration.CustomStrategy
import io.github.mottljan.api.decoration.Decoration
import io.github.mottljan.api.decoration.customStrategy
import io.github.mottljan.api.decorator.DecoratorConfig

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
        removeDecorationWithId(Decoration.Id("Non-existing ID"))
    }
}
