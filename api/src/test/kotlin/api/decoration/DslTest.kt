package api.decoration

import api.decorator.DecoratorConfig
import kotlinx.coroutines.flow.Flow
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

internal class DslTest {

    private val decoratorConfig = TestDecoratorConfig()

    @Test
    fun `creates replaceAll strategy with multiple decoration providers`() {
        val firstProvider = createTestDecorationProvider()
        val secondProvider = createTestDecorationProvider()

        val strategy = decoratorConfig.replaceAllStrategy {
            replaceWith(firstProvider)
            replaceWith(secondProvider)
        }

        strategy shouldBeInstanceOf ReplaceAllStrategy::class
        (strategy as ReplaceAllStrategy).providers shouldBeEqualTo listOf(firstProvider, secondProvider)
    }

    @Test
    fun `creates appendAll strategy with multiple decoration providers`() {
        val firstProvider = createTestDecorationProvider()
        val secondProvider = createTestDecorationProvider()

        val strategy = decoratorConfig.appendAllStrategy {
            append(firstProvider)
            append(secondProvider)
        }

        strategy shouldBeInstanceOf AppendAllStrategy::class
        (strategy as AppendAllStrategy).providers shouldBeEqualTo listOf(firstProvider, secondProvider)
    }

    @Test
    fun `creates custom strategy with all supported actions`() {
        // Arrange
        val firstRemoveId = Decoration.Provider.Id("firstRemoveId")
        val secondRemoveId = Decoration.Provider.Id("secondRemoveId")

        val firstReplaceId = Decoration.Provider.Id("firstReplaceId")
        val secondReplaceId = Decoration.Provider.Id("secondReplaceId")
        val firstReplaceProvider = createTestDecorationProvider()
        val secondReplaceProvider = createTestDecorationProvider()

        val firstAppendProvider = createTestDecorationProvider()
        val secondAppendProvider = createTestDecorationProvider()

        // Act
        val strategy = decoratorConfig.customStrategy {
            removeWithId(firstRemoveId)
            replace(firstReplaceId) with firstReplaceProvider
            append(firstAppendProvider)

            removeWithId(secondRemoveId)
            replace(secondReplaceId) with secondReplaceProvider
            append(secondAppendProvider)
        }

        // Assert
        strategy shouldBeInstanceOf CustomStrategy::class

        val actions = (strategy as CustomStrategy).actions
        (actions[0] as CustomStrategy.Action.Remove).providerId shouldBeEqualTo firstRemoveId

        val firstReplaceAction = (actions[1] as CustomStrategy.Action.Replace)
        firstReplaceAction.oldProviderId shouldBeEqualTo firstReplaceId
        firstReplaceAction.newProvider shouldBeEqualTo firstReplaceProvider

        (actions[2] as CustomStrategy.Action.Append).provider shouldBeEqualTo firstAppendProvider

        (actions[3] as CustomStrategy.Action.Remove).providerId shouldBeEqualTo secondRemoveId

        val secondReplaceAction = (actions[4] as CustomStrategy.Action.Replace)
        secondReplaceAction.oldProviderId shouldBeEqualTo secondReplaceId
        secondReplaceAction.newProvider shouldBeEqualTo secondReplaceProvider

        (actions[5] as CustomStrategy.Action.Append).provider shouldBeEqualTo secondAppendProvider

    }
}

private fun createTestDecorationProvider() = TestDecoration.Provider()

private class TestDecoration : Decoration {

    override suspend fun <Response> decorate(rpc: suspend () -> Response): Response = rpc()

    override fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response> = rpc()

    class Provider : Decoration.Provider<TestDecoration>(Decoration.InitStrategy.FACTORY, { TestDecoration() }) {

        override val id = ID

        companion object {

            val ID = Id(Provider::class.qualifiedName!!)
        }
    }
}

private class TestDecoratorConfig : DecoratorConfig<Unit> {

    override fun getStub() = Unit
}
