package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decoration.AppendAllStrategy
import api.decoration.Decoration
import api.decoration.DispatcherSwappingDecoration
import api.decoration.appendAllStrategy
import api.decorator.DecoratorConfig
import io.grpc.Channel
import kotlinx.coroutines.Dispatchers
import v1.ArticleGrpcKt

/**
 * [DecoratorConfig] used for verification of decorator generation of real gRPC stub
 */
@DecoratorConfiguration
class ArticleStubDecoratorConfig(private val channel: Channel) : DecoratorConfig<ArticleGrpcKt.ArticleCoroutineStub> {

    override fun getStub(): ArticleGrpcKt.ArticleCoroutineStub {
        return ArticleGrpcKt.ArticleCoroutineStub(channel)
    }

    override fun getStubDecorationStrategy() = appendAllStrategy {
        append(DispatcherSwappingDecoration(Dispatchers.IO))
    }
}
