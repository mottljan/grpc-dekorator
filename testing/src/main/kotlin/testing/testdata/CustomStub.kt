package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decorator.DecoratorConfig

internal class CustomStub {

    suspend fun rpc() = Unit
}

@DecoratorConfiguration
internal class CustomStubDecoratorConfig(
    private val firstProvider: Decoration.Provider<*>,
    private val secondProvider: Decoration.Provider<*>,
) : DecoratorConfig<CustomStub> {

    override fun getStub(): CustomStub {
        return CustomStub()
    }

    override fun getDecorationStrategy() = Decoration.Strategy.custom {
        removeWithId(GlobalDecorationA.Provider.ID)
        replace(GlobalDecorationB.Provider.ID) with firstProvider
        append(secondProvider)
    }
}
