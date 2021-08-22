package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decoration.DispatcherSwappingDecoration
import api.decorator.DecoratorConfig
import kotlinx.coroutines.Dispatchers

internal class ReplaceExceptionStub {

    suspend fun rpc() = Unit
}

@DecoratorConfiguration
internal class ReplaceExceptionDecoratorConfig : DecoratorConfig<ReplaceExceptionStub> {

    override fun getStub(): ReplaceExceptionStub {
        return ReplaceExceptionStub()
    }

    override fun getDecorationStrategy() = Decoration.Strategy.custom {
        val provider = DispatcherSwappingDecoration.Provider(Decoration.InitStrategy.FACTORY, Dispatchers.IO)
        replace(Decoration.Provider.Id("Non-existing ID")) with provider
    }
}
