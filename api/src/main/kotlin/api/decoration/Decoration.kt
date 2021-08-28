package api.decoration

import api.decorator.DecoratorConfig
import kotlinx.coroutines.flow.Flow
import java.lang.IllegalStateException

/**
 * Decoration of the RPCs of the gRPC stubs. [Decoration] instances are used by the generated
 * stub decorator.
 */
interface Decoration {

    /**
     * Decorates suspend [rpc]. It must call [rpc] and return [Response] to the caller.
     */
    suspend fun <Response> decorate(rpc: suspend () -> Response): Response

    /**
     * Decorates streaming [rpc] returning [Flow].
     * It must call [rpc] and return [Flow] to the caller.
     */
    fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response>

    /**
     * Provider of the [Decoration] instances. It is responsible for proper creation of [Decoration]s
     * based on the [initStrategy]. Since this class is responsible for proper [initStrategy],
     * [factory] always has to create a new instance when called.
     */
    abstract class Provider<D : Decoration>(
        private val initStrategy: InitStrategy,
        private val factory: () -> D
    ) {

        private var singleton: D? = null

        abstract val id: Id

        init {
            if (initStrategy == InitStrategy.SINGLETON) {
                singleton = factory()
            }
        }

        /**
         * Returns [Decoration] applying provided [initStrategy]
         */
        fun getDecoration(): D {
            return when (initStrategy) {
                InitStrategy.SINGLETON -> singleton ?: throw IllegalStateException("Singleton instance should have been already initialized")
                InitStrategy.LAZY -> singleton ?: factory().also { singleton = it }
                InitStrategy.FACTORY -> factory()
            }
        }

        data class Id(val value: String)
    }

    /**
     * Initialization strategy of the [Decoration]
     */
    enum class InitStrategy {

        /**
         * Only one instance of [Decoration] will be created as soon as possible
         */
        SINGLETON,

        /**
         * Same as [SINGLETON] but instance will be created lazily when really needed (first RPC is called)
         */
        LAZY,

        /**
         * New instance of [Decoration] will be created for each RPC call
         */
        FACTORY
    }

    // TODO docs
    sealed class Strategy
}

// Extension to limit the scope where this can be used
@Suppress("unused")
fun DecoratorConfig<*>.noChangesStrategy(): Decoration.Strategy {
    return appendAllStrategy {}
}
