package api.internal.decorator

import api.decoration.AppendAllStrategy
import api.decoration.CustomStrategy
import api.decoration.Decoration
import api.decoration.ReplaceAllStrategy
import kotlinx.coroutines.flow.Flow

/**
 * Common base class for all generated stub decorators containing helper methods for applying
 * [Decoration]s. This class is intended to be used ONLY by generated decorators and because of that
 * it can not be internal and must be public.
 */
@Deprecated("Should be used only by generated decorators!", level = DeprecationLevel.HIDDEN)
abstract class CoroutineStubDecorator {

    protected suspend fun <Resp> Iterator<Decoration.Provider<*>>.applyNextDecorationOrCallRpc(callRpc: suspend () -> Resp): Resp {
        return if (hasNext()) {
            next().getDecoration().decorate { applyNextDecorationOrCallRpc(callRpc) }
        } else {
            callRpc()
        }
    }

    protected fun <Resp> Iterator<Decoration.Provider<*>>.applyNextDecorationOrCallStreamRpc(callRpc: () -> Flow<Resp>): Flow<Resp> {
        return if (hasNext()) {
            next().getDecoration().decorateStream { applyNextDecorationOrCallStreamRpc(callRpc) }
        } else {
            callRpc()
        }
    }

    // TODO Rename globalProviders to something else when this is used for particular RPC decoration resolution too
    protected fun resolveDecorationProvidersBasedOnStrategy(
        globalProviders: List<Decoration.Provider<*>>,
        strategy: Decoration.Strategy
    ): List<Decoration.Provider<*>> {
        return when (strategy) {
            is AppendAllStrategy -> globalProviders + strategy.providers
            is ReplaceAllStrategy -> strategy.providers
            is CustomStrategy -> resolveProvidersBasedOnCustomStrategy(strategy, globalProviders)
            // when is exhaustive without else branch, however at compile time it fails that it is not.
            // Probably caused by sealed class descendants not nested inside sealed class.
            else -> throw IllegalStateException("Unsupported strategy")
        }
    }

    private fun resolveProvidersBasedOnCustomStrategy(
        strategy: CustomStrategy,
        globalProviders: List<Decoration.Provider<*>>
    ): List<Decoration.Provider<*>> {
        val resolvedProviders = globalProviders.toMutableList()
        strategy.actions.forEach { resolvedProviders.modifyBasedOnAction(it) }
        return resolvedProviders
    }

    private fun MutableList<Decoration.Provider<*>>.modifyBasedOnAction(action: CustomStrategy.Action) {
        when (action) {
            is CustomStrategy.Action.Remove -> {
                val wasRemoved = removeIf { it.id == action.providerId }
                if (!wasRemoved) {
                    throwIllegalStrategyActionException(actionName = "remove", action.providerId)
                }
            }
            is CustomStrategy.Action.Replace -> {
                var wasReplaced = false
                replaceAll { provider ->
                    if (provider.id == action.oldProviderId) {
                        wasReplaced = true
                        action.newProvider
                    } else {
                        provider
                    }
                }
                if (!wasReplaced) {
                    throwIllegalStrategyActionException(actionName = "replace", action.oldProviderId)
                }
            }
            is CustomStrategy.Action.Append -> {
                this += action.provider
            }
        }
    }

    private fun throwIllegalStrategyActionException(actionName: String, providerId: Decoration.Provider.Id): Nothing {
        throw IllegalStateException("Tried to $actionName ${Decoration.Provider::class.simpleName} " +
            "with id \"$providerId\", but it was not found")
    }
}
