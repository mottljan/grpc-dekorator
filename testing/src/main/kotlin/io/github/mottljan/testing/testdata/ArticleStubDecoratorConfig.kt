package io.github.mottljan.testing.testdata

import io.github.mottljan.api.annotation.DecoratorConfiguration
import io.github.mottljan.api.decoration.DispatcherSwappingDecoration
import io.github.mottljan.api.decoration.appendAllStrategy
import io.github.mottljan.api.decorator.DecoratorConfig
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
