package api.decoration

import api.decorator.DecoratorConfig
import kotlinx.coroutines.flow.Flow
import java.lang.IllegalStateException

/**
 * Decoration of the RPCs of the gRPC stubs. [Decoration] instances are used by the generated
 * stub decorator (see [DecoratorConfig]). It enables to specify an additional functionality to the
 * RPC like logging, exception mapping, etc. and reuse this easily across multiple RPCs and stubs.
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

        /**
         * [id] of the [Provider] which has to be unique for each [Provider] class. It is [id] of
         * the particular [Provider] subclass and not an instance!
         */
        abstract val id: Id

        init {
            if (initStrategy == InitStrategy.SINGLETON) {
                singleton = factory()
            }
        }

        /**
         * Returns [Decoration] by applying provided [initStrategy]
         */
        fun getDecoration(): D {
            return when (initStrategy) {
                InitStrategy.SINGLETON -> singleton ?: throw IllegalStateException("Singleton instance should have been already initialized")
                InitStrategy.LAZY -> singleton ?: factory().also { singleton = it }
                InitStrategy.FACTORY -> factory()
            }
        }

        /**
         * [Id] of the [Provider] which has to be unique to a specific [Provider] subclass and not
         * an instance. Proper [Id]s are important for applying a [Decoration.Strategy] correctly.
         */
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

    /**
     * [Strategy] for resolving a final list of [Decoration.Provider]s to be applied to RPCs. It can
     * be used in two contexts: whole stub or particular RPC. [Strategy] enables to specify how the
     * unique list of [Decoration.Provider]s of stub or RPC should be "merged" together with the
     * higher level one list (either global [Decoration.Provider]s or stub ones based on the context).
     */
    sealed class Strategy
}

/**
 * Creates a [Decoration.Strategy] which doesn't change a higher level list of [Decoration.Provider]s
 * in any way.
 */
@Suppress("unused") // Extension to limit the scope where this can be used
fun DecoratorConfig<*>.noChangesStrategy(): Decoration.Strategy {
    return appendAllStrategy {}
}
