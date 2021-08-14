package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decorator.DecoratorConfig

@Suppress("UnusedPrivateMember")
internal class InternalCoroutineStub

@DecoratorConfiguration
internal class InternalCoroutineStubDecoratorConfig : DecoratorConfig<InternalCoroutineStub> {

    override fun getStub(): InternalCoroutineStub {
        return InternalCoroutineStub()
    }

    override fun getDecorationProviders(): List<Decoration.Provider<*>> {
        return emptyList()
    }
}
