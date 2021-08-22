package api.decoration

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
    sealed class Strategy {

        data class ReplaceAll(val providers: List<Provider<*>>) : Strategy()

        data class AppendAll(val providers: List<Provider<*>>) : Strategy()

        data class Custom(val actions: List<Action>) : Strategy() {

            sealed interface Action {

                data class Remove(val providerId: Provider.Id) : Action

                data class Replace(val oldProviderId: Provider.Id, val newProvider: Provider<*>) : Action

                data class Append(val provider: Provider<*>) : Action
            }
        }

        companion object {

            fun replaceAll(block: ReplaceAllStrategyDefinition.() -> Unit): Strategy {
                val decorationProviders = ReplaceAllStrategyDefinition().apply(block).decorationProviders
                return ReplaceAll(decorationProviders)
            }

            fun appendAll(block: AppendAllStrategyDefinition.() -> Unit): Strategy {
                val decorationProviders = AppendAllStrategyDefinition().apply(block).decorationProviders
                return AppendAll(decorationProviders)
            }

            fun custom(block: CustomStrategyDefinition.() -> Unit): Strategy {
                val actions = CustomStrategyDefinition().apply(block).actions
                return Custom(actions)
            }
        }
    }
}

abstract class BaseAllStrategyDefinition internal constructor() {

    protected val _decorationProviders = mutableListOf<Decoration.Provider<*>>()
    val decorationProviders: List<Decoration.Provider<*>> get() = _decorationProviders
}

class ReplaceAllStrategyDefinition internal constructor() : BaseAllStrategyDefinition() {

    fun replaceWith(decorationProvider: Decoration.Provider<*>) {
        _decorationProviders += decorationProvider
    }
}

class AppendAllStrategyDefinition internal constructor() : BaseAllStrategyDefinition() {

    fun append(decorationProvider: Decoration.Provider<*>) {
        _decorationProviders += decorationProvider
    }
}

class CustomStrategyDefinition internal constructor()  {

    private val _actions = mutableListOf<Decoration.Strategy.Custom.Action>()
    val actions: List<Decoration.Strategy.Custom.Action> get() = _actions

    fun removeWithId(providerId: Decoration.Provider.Id) {
        _actions += Decoration.Strategy.Custom.Action.Remove(providerId)
    }

    fun replace(providerId: Decoration.Provider.Id) = Replace(providerId)

    fun append(decorationProvider: Decoration.Provider<*>) {
        _actions += Decoration.Strategy.Custom.Action.Append(decorationProvider)
    }

    inner class Replace internal constructor(private val providerId: Decoration.Provider.Id) {

        infix fun with(decorationProvider: Decoration.Provider<*>) {
            _actions += Decoration.Strategy.Custom.Action.Replace(providerId, decorationProvider)
        }
    }
}
