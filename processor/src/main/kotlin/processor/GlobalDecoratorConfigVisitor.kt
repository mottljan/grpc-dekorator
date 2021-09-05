package processor

import api.annotation.GlobalDecoratorConfiguration
import api.decorator.GlobalDecoratorConfig
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor

/**
 * Processes [GlobalDecoratorConfiguration] and returns [GlobalDecoratorConfigResult]
 */
internal class GlobalDecoratorConfigVisitor(private val environment: SymbolProcessorEnvironment) : KSDefaultVisitor<Unit, GlobalDecoratorConfigResult>() {

    override fun defaultHandler(node: KSNode, data: Unit): GlobalDecoratorConfigResult {
        logAndThrow(DecoratorProcessor.GLOBAL_DECORATOR_CONFIGURATION_CLASS_KIND_ERROR, node)
    }

    private fun logAndThrow(message: String, node: KSNode): Nothing {
        environment.logger.error(message, node)
        throw InvalidGlobalDecoratorConfigurationException()
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit): GlobalDecoratorConfigResult {
        val implementsRequiredInterface = classDeclaration.superTypes.any {
            it.resolve().declaration.qualifiedName!!.asString() == GlobalDecoratorConfig::class.qualifiedName
        }
        return if (implementsRequiredInterface) {
            GlobalDecoratorConfigResult.Exists(classDeclaration.qualifiedName!!.asString())
        } else {
            logAndThrow(DecoratorProcessor.GLOBAL_DECORATOR_CONFIGURATION_IMPL_ERROR, classDeclaration)
        }
    }
}

/**
 * Result of [GlobalDecoratorConfiguration] processing
 */
internal sealed class GlobalDecoratorConfigResult {

    /**
     * Indicates that [GlobalDecoratorConfiguration] is missing
     */
    object Missing : GlobalDecoratorConfigResult()

    /**
     * [GlobalDecoratorConfiguration] exists. [configTypeQualifiedName] is a qualified name of the
     * type of the implementation of [GlobalDecoratorConfig].
     */
    data class Exists(val configTypeQualifiedName: String) : GlobalDecoratorConfigResult()
}

internal class InvalidGlobalDecoratorConfigurationException : Exception("Global decorator configuration is invalid")
