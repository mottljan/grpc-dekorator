package example

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration
import api.decoration.ExceptionMapper
import api.decoration.ExceptionMappingDecoration
import api.decorator.DecoratorConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

@DecoratorConfiguration
class FakeCoroutineStubDecoratorConfig : DecoratorConfig<FakeCoroutineStub> {

    override fun provideStub(): FakeCoroutineStub {
        return FakeCoroutineStub()
    }

    override fun provideDecorations(): List<Decoration> {
        return listOf(ExceptionMappingDecoration(ExceptionMapperImpl()), LoggingDecoration())
    }
}

private class ExceptionMapperImpl : ExceptionMapper {

    override fun mapException(throwable: Throwable): Throwable {
        return CustomException(throwable)
    }
}

class CustomException(cause: Throwable) : Exception(cause)

private class LoggingDecoration : Decoration {

    override suspend fun <Response> decorate(rpc: suspend () -> Response): Response {
        println("starting logging decoration ...")
        return rpc().also { println("finishing logging decoration ...") }
    }

    override fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response> {
        println("starting streaming logging decoration ...")
        return rpc().onEach {
            println("onEach called with $it")
        }.onCompletion {
            println("finishing streaming logging decoration ...")
        }
    }
}
