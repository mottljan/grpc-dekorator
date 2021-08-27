package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decoration.DispatcherSwappingDecoration
import api.decorator.DecoratorConfig
import io.grpc.Channel
import kotlinx.coroutines.Dispatchers
import v1.ArticleGrpcKt

@DecoratorConfiguration
class ArticleStubDecoratorConfig(private val channel: Channel) : DecoratorConfig<ArticleGrpcKt.ArticleCoroutineStub> {

    override fun getStub(): ArticleGrpcKt.ArticleCoroutineStub {
        return ArticleGrpcKt.ArticleCoroutineStub(channel)
    }

    override fun getStubDecorationStrategy() = Decoration.Strategy.appendAll {
        append(DispatcherSwappingDecoration.Provider(Decoration.InitStrategy.SINGLETON, Dispatchers.IO))
    }
}