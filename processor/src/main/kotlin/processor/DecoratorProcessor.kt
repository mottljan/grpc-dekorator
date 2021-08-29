package processor

import api.annotation.DecoratorConfiguration
import api.annotation.GlobalDecoratorConfiguration
import api.annotation.RpcConfiguration
import api.decoration.Decoration
import api.decorator.DecoratorConfig
import api.decorator.GlobalDecoratorConfig
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor

// TODO Revalidate whole solution (API) / try on real app (FN)
// TODO Think about lib name
// TODO Write README
// TODO Solve package name

// TODO testing with kotlin compile testing is not possible due to this error https://github.com/google/ksp/issues/427
/**
 * [SymbolProcessor] processing classes annotated with [DecoratorConfiguration] together
 * with other needed symbols and generating gRPC stub decorator classes based on that.
 */
internal class DecoratorProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val globalDecoratorConfigResult = processGlobalDecoratorConfiguration(resolver)
        processDecoratorConfigurations(resolver, globalDecoratorConfigResult)
        return emptyList()
    }

    private fun processGlobalDecoratorConfiguration(resolver: Resolver): GlobalDecoratorConfigResult {
        val configs = resolver.getSymbolsWithAnnotation(GlobalDecoratorConfiguration::class.qualifiedName!!)
        return when (configs.count()) {
            0 -> GlobalDecoratorConfigResult.Missing
            1 -> configs.first().accept(GlobalDecoratorConfigVisitor(environment), Unit)
            else -> {
                environment.logger.error(GLOBAL_DECORATOR_CONFIGURATION_TOO_MANY_ERROR, configs.first())
                throw InvalidGlobalDecoratorConfigurationException()
            }
        }
    }

    private fun processDecoratorConfigurations(resolver: Resolver, globalDecoratorConfigResult: GlobalDecoratorConfigResult) {
        val decoratorConfigs = resolver.getSymbolsWithAnnotation(DecoratorConfiguration::class.qualifiedName!!)
        decoratorConfigs.filter { it !is KSClassDeclaration }.forEach { notSupportedDeclaration ->
            environment.logger.error(DECORATOR_CONFIGURATION_ERROR, notSupportedDeclaration)
        }
        decoratorConfigs.forEach { symbol -> symbol.accept(DecoratorConfigVisitor(environment, globalDecoratorConfigResult), Unit) }
    }

    companion object {

        val GLOBAL_DECORATOR_CONFIGURATION_TOO_MANY_ERROR = "${GlobalDecoratorConfiguration::class.simpleName} can be used only once"
        val GLOBAL_DECORATOR_CONFIGURATION_CLASS_KIND_ERROR = "${GlobalDecoratorConfiguration::class.simpleName} must annotate class"
        val GLOBAL_DECORATOR_CONFIGURATION_IMPL_ERROR =
            "Class annotated with ${GlobalDecoratorConfiguration::class.simpleName} must implement ${GlobalDecoratorConfig::class.qualifiedName}"
        val GLOBAL_DECORATOR_CONFIGURATION_PROPERTY_ERROR = "${GlobalDecoratorConfig::decorationProviders.name} property has to have a backing field"

        val DECORATOR_CONFIGURATION_ERROR = "Not supported declaration type annotated with ${DecoratorConfiguration::class.simpleName}"

        val RPC_CONFIGURATION_KIND_ERROR = "${RpcConfiguration::class.simpleName} can annotate only functions"
        val RPC_CONFIGURATION_PARAM_ERROR = "Method annotated with ${RpcConfiguration::class.simpleName} can't have any parameters"
        val RPC_CONFIGURATION_RETURN_TYPE_ERROR =
            "Method annotated with ${RpcConfiguration::class.simpleName} has to return ${Decoration.Strategy::class.qualifiedName}"

        fun generateMissingInterfaceImplErrorMsg(className: String): String {
            return "Class $className annotated with ${DecoratorConfiguration::class.simpleName} " +
                "does not implement ${DecoratorConfig::class.simpleName}"
        }

        fun generateNotGeneratedFunctionWarningMsg(funName: String): String {
            return "Decorating fun for $funName not generated! Only suspend fun or fun returning Flow is supported."
        }

        fun generateNonExistingRpcError(invalidRpcName: String): String {
            return "Trying to decorate non-existing RPC with name: $invalidRpcName"
        }
    }

    class Provider : SymbolProcessorProvider {

        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return DecoratorProcessor(environment)
        }
    }
}
