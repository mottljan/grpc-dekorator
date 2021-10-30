package io.github.mottljan.api.internal.decorator

import io.github.mottljan.api.decoration.AppendAllStrategy
import io.github.mottljan.api.decoration.CustomStrategy
import io.github.mottljan.api.decoration.Decoration
import io.github.mottljan.api.decoration.ReplaceAllStrategy
import io.github.mottljan.api.decorator.GlobalDecoratorConfig
import kotlinx.coroutines.flow.Flow

/**
 * Common base class for all generated stub decorators containing helper methods for applying
 * [Decoration]s. This class is intended to be used ONLY by generated decorators and because of that
 * it can not be internal and must be public.
 */
@Deprecated("Should be used only by generated decorators!", level = DeprecationLevel.HIDDEN)
abstract class CoroutineStubDecorator(private val globalDecoratorConfig: GlobalDecoratorConfig? = null) {

    protected suspend fun <Resp> Iterator<Decoration>.applyNextDecorationOrCallRpc(callRpc: suspend () -> Resp): Resp {
        return if (hasNext()) {
            next().decorate { applyNextDecorationOrCallRpc(callRpc) }
        } else {
            callRpc()
        }
    }

    protected fun <Resp> Iterator<Decoration>.applyNextDecorationOrCallStreamRpc(callRpc: () -> Flow<Resp>): Flow<Resp> {
        return if (hasNext()) {
            next().decorateStream { applyNextDecorationOrCallStreamRpc(callRpc) }
        } else {
            callRpc()
        }
    }

    protected fun resolveDecorationsBasedOnStrategy(
        higherLevelDecorations: List<Decoration>,
        strategy: Decoration.Strategy
    ): List<Decoration> {
        return when (strategy) {
            is AppendAllStrategy -> higherLevelDecorations + strategy.decorations
            is ReplaceAllStrategy -> strategy.decorations
            is CustomStrategy -> resolveDecorationsBasedOnCustomStrategy(higherLevelDecorations, strategy)
            // when is exhaustive without else branch, however at compile time it fails that it is not.
            // Probably caused by sealed class descendants not nested inside sealed class.
            else -> throw IllegalStateException("Unsupported strategy")
        }
    }

    private fun resolveDecorationsBasedOnCustomStrategy(
        higherLevelDecorations: List<Decoration>,
        strategy: CustomStrategy
    ): List<Decoration> {
        val resolvedDecorations = higherLevelDecorations.toMutableList()
        strategy.actions.forEach { resolvedDecorations.modifyBasedOnAction(it) }
        return resolvedDecorations
    }

    private fun MutableList<Decoration>.modifyBasedOnAction(action: CustomStrategy.Action) {
        when (action) {
            is CustomStrategy.Action.Remove -> {
                val wasRemoved = removeAll { it.id == action.decorationId }
                if (!wasRemoved) {
                    deliverIllegalStrategyActionException(actionName = "remove", action.decorationId)
                }
            }
            is CustomStrategy.Action.Replace -> {
                var wasReplaced = false
                replaceAll(this) { decoration ->
                    if (decoration.id == action.oldDecorationId) {
                        wasReplaced = true
                        action.newDecoration
                    } else {
                        decoration
                    }
                }
                if (!wasReplaced) {
                    deliverIllegalStrategyActionException(actionName = "replace", action.oldDecorationId)
                }
            }
            is CustomStrategy.Action.Append -> {
                this += action.decoration
            }
        }
    }

    private fun deliverIllegalStrategyActionException(actionName: String, decorationId: Decoration.Id) {
        val exception = IllegalStateException("Tried to $actionName ${Decoration::class.simpleName} " +
            "with id \"$decorationId\", but it was not found")
        globalDecoratorConfig?.handleException(exception) ?: throw exception
    }

    private fun <T> replaceAll(list: MutableList<T>, replace: (T) -> T) {
        val listIterator = list.listIterator()
        while (listIterator.hasNext()) {
            listIterator.set(replace(listIterator.next()))
        }
    }
}
