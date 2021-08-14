package api.decoration

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldNotBe
import org.junit.jupiter.api.Test

internal class ExceptionMappingDecorationProviderTest {

    @Test
    fun `create singleton instance for singleton strategy`() {
        val sut = createSut(Decoration.InitStrategy.SINGLETON)

        sut.getDecoration() shouldBe sut.getDecoration()
    }

    private fun createSut(initStrategy: Decoration.InitStrategy): ExceptionMappingDecoration.Provider {
        return ExceptionMappingDecoration.Provider(initStrategy, object : ExceptionMapper {

            override fun mapException(throwable: Throwable) = throwable
        })
    }

    @Test
    fun `create new instance for each invocation for factory strategy`() {
        val sut = createSut(Decoration.InitStrategy.FACTORY)

        sut.getDecoration() shouldNotBe sut.getDecoration()
    }
}
