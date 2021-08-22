package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decorator.DecoratorConfig

internal class ReplacingAllStub {

    suspend fun rpc() = Unit
}

@DecoratorConfiguration
internal class ReplacingAllStubDecoratorConfig(
    private val testDecorationProviders: List<Decoration.Provider<*>>,
) : DecoratorConfig<ReplacingAllStub> {

    override fun getStub(): ReplacingAllStub {
        return ReplacingAllStub()
    }

    override fun getDecorationStrategy() = Decoration.Strategy.replaceAll {
        testDecorationProviders.forEach { replaceWith(it) }
    }
}
