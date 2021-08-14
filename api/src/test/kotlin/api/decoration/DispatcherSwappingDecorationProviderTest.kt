package api.decoration

import kotlinx.coroutines.Dispatchers
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldNotBe
import org.junit.jupiter.api.Test

internal class DispatcherSwappingDecorationProviderTest {

    @Test
    fun `create singleton instance for singleton strategy`() {
        val sut = createSut(Decoration.InitStrategy.SINGLETON)

        sut.getDecoration() shouldBe sut.getDecoration()
    }

    private fun createSut(initStrategy: Decoration.InitStrategy): DispatcherSwappingDecoration.Provider {
        return DispatcherSwappingDecoration.Provider(initStrategy, Dispatchers.Main)
    }

    @Test
    fun `create new instance for each invocation for factory strategy`() {
        val sut = createSut(Decoration.InitStrategy.FACTORY)

        sut.getDecoration() shouldNotBe sut.getDecoration()
    }
}
