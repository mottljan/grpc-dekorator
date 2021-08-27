package processor

import api.annotation.RpcConfiguration
import api.decoration.Decoration
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.visitor.KSDefaultVisitor

/**
 * TODO add class description
 */
internal class RpcConfigurationVisitor(
    environment: SymbolProcessorEnvironment
) : KSDefaultVisitor<RpcConfigurationVisitor.InputData, RpcConfigResult>() {

    private val logger = environment.logger

    override fun defaultHandler(node: KSNode, data: InputData): RpcConfigResult {
        logAndThrow(DecoratorProcessor.RPC_CONFIGURATION_KIND_ERROR, node)
    }

    private fun logAndThrow(message: String, node: KSNode): Nothing {
        logger.error(message, node)
        throw InvalidRpcConfigurationException()
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: InputData): RpcConfigResult {
        val returnTypeName = function.returnType?.resolve()?.declaration?.qualifiedName?.asString()
        when {
            returnTypeName != Decoration.Strategy::class.qualifiedName -> {
                logAndThrow(DecoratorProcessor.RPC_CONFIGURATION_RETURN_TYPE_ERROR, function)
            }
            function.parameters.isNotEmpty() -> {
                logAndThrow(DecoratorProcessor.RPC_CONFIGURATION_PARAM_ERROR, function)
            }
            else -> {
                val rpcName = data.rpcConfigAnnotation.getRpcName()
                validateThatRpcExists(data.stubResolvedType, rpcName)
                val rpcConfigMethodName = function.simpleName.asString()
                return RpcConfigResult(rpcName = rpcName, rpcConfigMethodName = rpcConfigMethodName)
            }
        }
    }

    private fun KSAnnotation.getRpcName(): String {
        val rpcName = arguments.find { it.name?.asString() == RpcConfiguration::rpcName.name }?.value as? String
        if (rpcName == null) {
            logger.error(
                "${RpcConfiguration::class.simpleName} does not contain param " +
                    "${RpcConfiguration::rpcName.name} of type String",
                this
            )
        }
        return rpcName ?: ""
    }

    private fun validateThatRpcExists(stubResolvedType: KSType, rpcName: String) {
        val stubClassDeclaration = stubResolvedType.declaration as? KSClassDeclaration
        if (stubClassDeclaration == null) {
            logger.error("Stub has to be a class")
        } else {
            val rpcExists = stubClassDeclaration.getAllFunctions().any {
                it.simpleName.asString() == rpcName
            }
            if (!rpcExists) {
                logger.error(DecoratorProcessor.generateNonExistingRpcError(rpcName), stubClassDeclaration)
            }
        }
    }

    data class InputData(val rpcConfigAnnotation: KSAnnotation, val stubResolvedType: KSType)
}

data class RpcConfigResult(val rpcName: String, val rpcConfigMethodName: String)

internal class InvalidRpcConfigurationException : Exception("RPC configuration is invalid")
