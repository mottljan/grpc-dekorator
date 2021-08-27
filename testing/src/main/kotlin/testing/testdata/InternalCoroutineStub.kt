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

    // TODO solve case when you do not want to actually use any strategy to provide your own decorations, but just reuse the top ones
    override fun getStubDecorationStrategy() = Decoration.Strategy.appendAll {}
}
