package api.decoration

import api.decorator.DecoratorConfig
import kotlinx.coroutines.flow.Flow
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

internal class DslTest {

    private val decoratorConfig = TestDecoratorConfig()

    @Test
    fun `creates replaceAll strategy with multiple decorations`() {
        val firstDecoration = createTestDecoration()
        val secondDecoration = createTestDecoration()

        val strategy = decoratorConfig.replaceAllStrategy {
            replaceWith(firstDecoration)
            replaceWith(secondDecoration)
        }

        strategy shouldBeInstanceOf ReplaceAllStrategy::class
        (strategy as ReplaceAllStrategy).decorations shouldBeEqualTo listOf(firstDecoration, secondDecoration)
    }

    @Test
    fun `creates appendAll strategy with multiple decorations`() {
        val firstDecoration = createTestDecoration()
        val secondDecoration = createTestDecoration()

        val strategy = decoratorConfig.appendAllStrategy {
            append(firstDecoration)
            append(secondDecoration)
        }

        strategy shouldBeInstanceOf AppendAllStrategy::class
        (strategy as AppendAllStrategy).decorations shouldBeEqualTo listOf(firstDecoration, secondDecoration)
    }

    @Test
    fun `creates custom strategy with all supported actions`() {
        // Arrange
        val firstRemoveId = Decoration.Id("firstRemoveId")
        val secondRemoveId = Decoration.Id("secondRemoveId")

        val firstReplaceId = Decoration.Id("firstReplaceId")
        val secondReplaceId = Decoration.Id("secondReplaceId")
        val firstReplaceDecoration = createTestDecoration()
        val secondReplaceDecoration = createTestDecoration()

        val firstAppendDecoration = createTestDecoration()
        val secondAppendDecoration = createTestDecoration()

        // Act
        val strategy = decoratorConfig.customStrategy {
            removeDecorationWithId(firstRemoveId)
            replace(firstReplaceId) with firstReplaceDecoration
            append(firstAppendDecoration)

            removeDecorationWithId(secondRemoveId)
            replace(secondReplaceId) with secondReplaceDecoration
            append(secondAppendDecoration)
        }

        // Assert
        strategy shouldBeInstanceOf CustomStrategy::class

        val actions = (strategy as CustomStrategy).actions
        (actions[0] as CustomStrategy.Action.Remove).decorationId shouldBeEqualTo firstRemoveId

        val firstReplaceAction = (actions[1] as CustomStrategy.Action.Replace)
        firstReplaceAction.oldDecorationId shouldBeEqualTo firstReplaceId
        firstReplaceAction.newDecoration shouldBeEqualTo firstReplaceDecoration

        (actions[2] as CustomStrategy.Action.Append).decoration shouldBeEqualTo firstAppendDecoration

        (actions[3] as CustomStrategy.Action.Remove).decorationId shouldBeEqualTo secondRemoveId

        val secondReplaceAction = (actions[4] as CustomStrategy.Action.Replace)
        secondReplaceAction.oldDecorationId shouldBeEqualTo secondReplaceId
        secondReplaceAction.newDecoration shouldBeEqualTo secondReplaceDecoration

        (actions[5] as CustomStrategy.Action.Append).decoration shouldBeEqualTo secondAppendDecoration
    }
}

private fun createTestDecoration() = TestDecoration()

private class TestDecoration : Decoration {

    override val id = ID

    override suspend fun <Response> decorate(rpc: suspend () -> Response): Response = rpc()

    override fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response> = rpc()

    companion object {

        val ID = Decoration.Id(TestDecoration::class.qualifiedName!!)
    }
}

private class TestDecoratorConfig : DecoratorConfig<Unit> {

    override fun getStub() = Unit
}
