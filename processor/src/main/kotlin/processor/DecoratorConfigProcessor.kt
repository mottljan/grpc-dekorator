package processor

import api.annotation.DecoratorConfiguration
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

// TODO Test processor
// TODO Solve package name
/**
 * [SymbolProcessor] processing classes annotated with [DecoratorConfiguration] and generating
 * gRPC stub decorator classes based on that.
 */
internal class DecoratorConfigProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val decoratorConfigs = resolver.getSymbolsWithAnnotation(DecoratorConfiguration::class.qualifiedName!!)
        decoratorConfigs.filter { it !is KSClassDeclaration }.forEach { notSupportedDeclaration ->
            environment.logger.error(
                "Not supported declaration type annotated with ${DecoratorConfiguration::class.simpleName}",
                notSupportedDeclaration
            )
        }
        decoratorConfigs.forEach { symbol -> symbol.accept(DecoratorConfigVisitor(environment), Unit) }
        return emptyList()
    }
}
