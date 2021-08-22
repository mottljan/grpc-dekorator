package api.decoration

import kotlinx.coroutines.flow.Flow
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

internal class DslTest {

    @Test
    fun `creates replaceAll strategy with multiple decoration providers`() {
        val firstProvider = createTestDecorationProvider()
        val secondProvider = createTestDecorationProvider()

        val strategy = Decoration.Strategy.replaceAll {
            replaceWith(firstProvider)
            replaceWith(secondProvider)
        }

        strategy shouldBeInstanceOf Decoration.Strategy.ReplaceAll::class
        (strategy as Decoration.Strategy.ReplaceAll).providers shouldBeEqualTo listOf(firstProvider, secondProvider)
    }

    @Test
    fun `creates appendAll strategy with multiple decoration providers`() {
        val firstProvider = createTestDecorationProvider()
        val secondProvider = createTestDecorationProvider()

        val strategy = Decoration.Strategy.appendAll {
            append(firstProvider)
            append(secondProvider)
        }

        strategy shouldBeInstanceOf Decoration.Strategy.AppendAll::class
        (strategy as Decoration.Strategy.AppendAll).providers shouldBeEqualTo listOf(firstProvider, secondProvider)
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
        val strategy = Decoration.Strategy.custom {
            removeWithId(firstRemoveId)
            replace(firstReplaceId) with firstReplaceProvider
            append(firstAppendProvider)

            removeWithId(secondRemoveId)
            replace(secondReplaceId) with secondReplaceProvider
            append(secondAppendProvider)
        }

        // Assert
        strategy shouldBeInstanceOf Decoration.Strategy.Custom::class

        val actions = (strategy as Decoration.Strategy.Custom).actions
        (actions[0] as Decoration.Strategy.Custom.Action.Remove).providerId shouldBeEqualTo firstRemoveId

        val firstReplaceAction = (actions[1] as Decoration.Strategy.Custom.Action.Replace)
        firstReplaceAction.oldProviderId shouldBeEqualTo firstReplaceId
        firstReplaceAction.newProvider shouldBeEqualTo firstReplaceProvider

        (actions[2] as Decoration.Strategy.Custom.Action.Append).provider shouldBeEqualTo firstAppendProvider

        (actions[3] as Decoration.Strategy.Custom.Action.Remove).providerId shouldBeEqualTo secondRemoveId

        val secondReplaceAction = (actions[4] as Decoration.Strategy.Custom.Action.Replace)
        secondReplaceAction.oldProviderId shouldBeEqualTo secondReplaceId
        secondReplaceAction.newProvider shouldBeEqualTo secondReplaceProvider

        (actions[5] as Decoration.Strategy.Custom.Action.Append).provider shouldBeEqualTo secondAppendProvider

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
