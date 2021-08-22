package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decorator.DecoratorConfig

internal class RemoveExceptionStub {

    suspend fun rpc() = Unit
}

@DecoratorConfiguration
internal class RemoveExceptionDecoratorConfig : DecoratorConfig<RemoveExceptionStub> {

    override fun getStub(): RemoveExceptionStub {
        return RemoveExceptionStub()
    }

    override fun getDecorationStrategy() = Decoration.Strategy.custom {
        removeWithId(Decoration.Provider.Id("Non-existing ID"))
    }
}
