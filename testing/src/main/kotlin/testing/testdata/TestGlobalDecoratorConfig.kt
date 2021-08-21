package testing.testdata

import api.annotation.GlobalDecoratorConfiguration
import api.decoration.Decoration
import api.decorator.GlobalDecoratorConfig
import kotlinx.coroutines.flow.Flow

@GlobalDecoratorConfiguration
class TestGlobalDecoratorConfig : GlobalDecoratorConfig {

    override val decorationProviders = listOf(globalDecorationProvider)
}

internal val globalDecorationProvider = GlobalDecoration.Provider()

class GlobalDecoration : Decoration {

    var suspendFunDecorationNanoTime = 0L
        private set

    var streamingFunDecorationNanoTime = 0L
        private set

    fun clearTimes() {
        suspendFunDecorationNanoTime = 0
        streamingFunDecorationNanoTime = 0
    }

    override suspend fun <Response> decorate(rpc: suspend () -> Response): Response {
        suspendFunDecorationNanoTime = System.nanoTime()
        return rpc()
    }

    override fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response> {
        streamingFunDecorationNanoTime = System.nanoTime()
        return rpc()
    }

    class Provider : Decoration.Provider<GlobalDecoration>(Decoration.InitStrategy.SINGLETON, ::GlobalDecoration) {

        fun clearTimes() {
            getDecoration().clearTimes()
        }
    }
}
