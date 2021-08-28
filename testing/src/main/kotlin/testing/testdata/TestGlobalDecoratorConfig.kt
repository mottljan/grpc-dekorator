package testing.testdata

import api.annotation.GlobalDecoratorConfiguration
import api.decoration.Decoration
import api.decorator.GlobalDecoratorConfig
import kotlinx.coroutines.flow.Flow

@GlobalDecoratorConfiguration
class TestGlobalDecoratorConfig(private val onHandleException: (Exception) -> Unit) : GlobalDecoratorConfig {

    override val decorationProviders = listOf(
        globalDecorationAProvider,
        globalDecorationBProvider,
        globalDecorationCProvider
    )

    override fun handleException(exception: Exception) {
        onHandleException(exception)
    }
}

internal val globalDecorationAProvider = GlobalDecorationA.Provider()
internal val globalDecorationBProvider = GlobalDecorationB.Provider()
internal val globalDecorationCProvider = GlobalDecorationC.Provider()

abstract class GlobalDecoration : Decoration {

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

    abstract class Provider<D : GlobalDecoration>(factory: () -> D) : Decoration.Provider<D>(
        Decoration.InitStrategy.SINGLETON,
        factory
    ) {

        fun clearTimes() {
            getDecoration().clearTimes()
        }
    }
}

internal class GlobalDecorationA : GlobalDecoration() {

    class Provider : GlobalDecoration.Provider<GlobalDecorationA>(::GlobalDecorationA) {

        override val id = ID

        companion object {

            val ID = Id(Provider::class.qualifiedName!!)
        }
    }
}

internal class GlobalDecorationB : GlobalDecoration() {

    class Provider : GlobalDecoration.Provider<GlobalDecorationB>(::GlobalDecorationB) {

        override val id = ID

        companion object {

            val ID = Id(Provider::class.qualifiedName!!)
        }
    }
}

internal class GlobalDecorationC : GlobalDecoration() {

    class Provider : GlobalDecoration.Provider<GlobalDecorationC>(::GlobalDecorationC) {

        override val id = ID

        companion object {

            val ID = Id(Provider::class.qualifiedName!!)
        }
    }
}
