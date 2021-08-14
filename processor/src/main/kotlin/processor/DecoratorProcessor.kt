package processor

import api.annotation.DecoratorConfiguration
import api.decorator.DecoratorConfig
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

// TODO support for Global decorations
// TODO support for apply strategy of stubs decorations (append to global, override all from global, override just some of them, etc.)
// TODO support for decorations of particular methods together with apply strategy

// TODO Solve package name

// TODO testing with kotlin compile testing is not possible due to this error https://github.com/google/ksp/issues/427
/**
 * [SymbolProcessor] processing classes annotated with [DecoratorConfiguration] together
 * with other needed symbols and generating gRPC stub decorator classes based on that.
 */
internal class DecoratorProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val decoratorConfigs = resolver.getSymbolsWithAnnotation(DecoratorConfiguration::class.qualifiedName!!)
        decoratorConfigs.filter { it !is KSClassDeclaration }.forEach { notSupportedDeclaration ->
            environment.logger.error(DECORATOR_CONFIGURATION_ERROR, notSupportedDeclaration)
        }
        decoratorConfigs.forEach { symbol -> symbol.accept(DecoratorConfigVisitor(environment), Unit) }
        return emptyList()
    }

    companion object {

        val DECORATOR_CONFIGURATION_ERROR = "Not supported declaration type annotated with ${DecoratorConfiguration::class.simpleName}"

        fun generateMissingInterfaceImplErrorMsg(className: String): String {
            return "Class $className annotated with ${DecoratorConfiguration::class.simpleName} " +
                "does not implement ${DecoratorConfig::class.simpleName}"
        }

        fun generateNotGeneratedFunctionWarningMsg(funName: String): String {
            return "Decorating fun for $funName not generated! Only suspend fun or fun returning Flow is supported."
        }
    }

    class Provider : SymbolProcessorProvider {

        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return DecoratorProcessor(environment)
        }
    }
}
