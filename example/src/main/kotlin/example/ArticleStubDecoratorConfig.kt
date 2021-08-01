package example

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decoration.DispatcherSwappingDecoration
import api.decorator.DecoratorConfig
import io.grpc.Channel
import kotlinx.coroutines.Dispatchers
import v1.ArticleGrpcKt

@DecoratorConfiguration
class ArticleStubDecoratorConfig(private val channel: Channel) : DecoratorConfig<ArticleGrpcKt.ArticleCoroutineStub> {

    override fun provideStub(): ArticleGrpcKt.ArticleCoroutineStub {
        return ArticleGrpcKt.ArticleCoroutineStub(channel)
    }

    override fun provideDecorations(): List<Decoration> {
        return listOf(DispatcherSwappingDecoration(Dispatchers.IO))
    }
}
