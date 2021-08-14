package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decoration.DispatcherSwappingDecoration
import api.decorator.DecoratorConfig
import io.grpc.Channel
import kotlinx.coroutines.Dispatchers
import v1.ArticleGrpcKt

@DecoratorConfiguration
internal class ArticleStubDecoratorConfig(private val channel: Channel) : DecoratorConfig<ArticleGrpcKt.ArticleCoroutineStub> {

    override fun getStub(): ArticleGrpcKt.ArticleCoroutineStub {
        return ArticleGrpcKt.ArticleCoroutineStub(channel)
    }

    override fun getDecorationProviders(): List<Decoration.Provider<*>> {
        return listOf(DispatcherSwappingDecoration.Provider(Decoration.InitStrategy.SINGLETON, Dispatchers.IO))
    }
}
