package io.github.mottljan.testing.testdata

import io.github.mottljan.api.annotation.GlobalDecoratorConfiguration
import io.github.mottljan.api.decoration.Decoration
import io.github.mottljan.api.decorator.GlobalDecoratorConfig
import kotlinx.coroutines.flow.Flow

/**
 * [GlobalDecoratorConfig] implementation used for testing purposes. This class should be changed
 * with caution since it can break tests.
 */
@GlobalDecoratorConfiguration
class TestGlobalDecoratorConfig(private val onHandleException: (Exception) -> Unit) : GlobalDecoratorConfig {

    override val decorations = listOf(
        globalDecorationA,
        globalDecorationB,
        globalDecorationC
    )

    override fun handleException(exception: Exception) {
        onHandleException(exception)
    }
}

internal val globalDecorationA = GlobalDecorationA()
internal val globalDecorationB = GlobalDecorationB()
internal val globalDecorationC = GlobalDecorationC()

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
}

internal class GlobalDecorationA : GlobalDecoration() {

    override val id = ID

    companion object {

        val ID = Decoration.Id(GlobalDecorationA::class.qualifiedName!!)
    }
}

internal class GlobalDecorationB : GlobalDecoration() {

    override val id = ID

    companion object {

        val ID = Decoration.Id(GlobalDecorationB::class.qualifiedName!!)
    }
}

internal class GlobalDecorationC : GlobalDecoration() {

    override val id = ID

    companion object {

        val ID = Decoration.Id(GlobalDecorationC::class.qualifiedName!!)
    }
}
