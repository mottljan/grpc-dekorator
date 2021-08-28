package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decoration.customStrategy
import api.decorator.DecoratorConfig

internal class RemoveExceptionStub {

    suspend fun rpc() = Unit
}

@DecoratorConfiguration
internal class RemoveExceptionDecoratorConfig : DecoratorConfig<RemoveExceptionStub> {

    override fun getStub(): RemoveExceptionStub {
        return RemoveExceptionStub()
    }

    override fun getStubDecorationStrategy() = customStrategy {
        removeWithId(Decoration.Provider.Id("Non-existing ID"))
    }
}
