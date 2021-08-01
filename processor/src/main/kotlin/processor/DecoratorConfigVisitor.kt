@file:Suppress("DEPRECATION_ERROR")

package processor

import api.annotation.DecoratorConfiguration
import api.decorator.DecoratorConfig
import api.internal.decorator.CoroutineStubDecorator
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream
import java.lang.StringBuilder

private const val PACKAGE_NAME = "stub.decorator.wtf" // TODO Update once known and also update packages in modules

/**
 * Visitor which handles [DecoratorConfiguration] annotations and generates decorator classes based
 * on the provided configuration by the annotated class.
 */
internal class DecoratorConfigVisitor(private val environment: SymbolProcessorEnvironment) : KSVisitorVoid() {

    private val logger = environment.logger

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val implementsRequiredInterface = classDeclaration.superTypes.any {
            it.resolve().declaration.qualifiedName!!.asString() == DecoratorConfig::class.qualifiedName
        }
        if (implementsRequiredInterface) {
            generateStubDecorator(classDeclaration)
        } else {
            val message = "Class ${classDeclaration.simpleName.asString()} annotated with ${DecoratorConfiguration::class.simpleName} " +
                "does not implement ${DecoratorConfig::class.simpleName}"
            logger.error(message, classDeclaration)
        }
    }

    private fun generateStubDecorator(classDeclaration: KSClassDeclaration) {
        val stubResolvedType = classDeclaration.getAllFunctions().findStubReference().resolve()
        val stubSimpleName = stubResolvedType.declaration.simpleName.asString()
        val stubDecoratorSimpleName = "${stubSimpleName}Decorator"

        val fileOutputStream = createDecoratorFile(name = stubDecoratorSimpleName)
        fileOutputStream += DecoratorFileContentGenerator(
            environment = environment,
            stubSimpleName = stubSimpleName,
            stubResolvedType = stubResolvedType,
            stubDecoratorSimpleName = stubDecoratorSimpleName,
        ).generate()
        fileOutputStream.close()
    }

    private fun Sequence<KSFunctionDeclaration>.findStubReference(): KSTypeReference {
        return find { it.simpleName.asString() == DecoratorConfig<*>::provideStub.name }!!.returnType!!
    }

    private fun createDecoratorFile(name: String): OutputStream {
        return environment.codeGenerator.createNewFile(
            dependencies = Dependencies(false),
            packageName = PACKAGE_NAME,
            fileName = name
        )
    }
}

private class DecoratorFileContentGenerator(
    environment: SymbolProcessorEnvironment,
    stubSimpleName: String,
    private val stubResolvedType: KSType,
    private val stubDecoratorSimpleName: String,
) {
    private val stubPropertyName = stubSimpleName.replaceFirstChar { it.lowercaseChar() }

    private val logger = environment.logger

    fun generate(): String {
        return buildString {
            appendPackage()
            appendClassHeaderAndProperties()
            appendDecoratingFunctions()
            append("}")
        }
    }

    private fun StringBuilder.appendPackage() {
        append("package ${PACKAGE_NAME}\n\n")
    }

    private fun StringBuilder.appendClassHeaderAndProperties() {
        val stubQualifiedName = stubResolvedType.declaration.qualifiedName!!.asString()
        val decoratorConfigPropertyName = "decoratorConfig"
        append(
            """
            @Suppress("DEPRECATION_ERROR")
            class $stubDecoratorSimpleName(
                $decoratorConfigPropertyName: ${DecoratorConfig::class.qualifiedName}<$stubQualifiedName>
            ) : ${CoroutineStubDecorator::class.qualifiedName}() {
                
                private val $stubPropertyName = $decoratorConfigPropertyName.${DecoratorConfig<*>::provideStub.name}()
                private val $DECORATIONS_PROPERTY_NAME = $decoratorConfigPropertyName.${DecoratorConfig<*>::provideDecorations.name}()
            """.trimIndent()
        )
        append("\n")
    }

    private fun StringBuilder.appendDecoratingFunctions() {
        (stubResolvedType.declaration as KSClassDeclaration)
            .getDeclaredFunctions()
            .filter { !it.isConstructor() }
            .filter { !it.modifiers.contains(Modifier.OVERRIDE) }
            .forEach { appendDecoratingFunction(it) }
    }

    private fun StringBuilder.appendDecoratingFunction(originalFunctionDeclaration: KSFunctionDeclaration) {
        append("\n")

        val funSimpleName = originalFunctionDeclaration.simpleName.asString()
        val originalResolvedReturnType = originalFunctionDeclaration.returnType!!.resolve()

        when {
            originalFunctionDeclaration.modifiers.contains(Modifier.SUSPEND) -> {
                appendDecoratingFunction(
                    originalFunctionDeclaration = originalFunctionDeclaration,
                    funSimpleName = funSimpleName,
                    originalResolvedReturnType = originalResolvedReturnType,
                    funModifiers = "suspend",
                    iteratorHelperMethodName = "applyNextDecorationOrCallRpc"
                )
            }
            originalResolvedReturnType.isFlow() -> {
                appendDecoratingFunction(
                    originalFunctionDeclaration = originalFunctionDeclaration,
                    funSimpleName = funSimpleName,
                    originalResolvedReturnType = originalResolvedReturnType,
                    funModifiers = "",
                    iteratorHelperMethodName = "applyNextDecorationOrCallStreamRpc"
                )
            }
            else -> {
                val message = "Decorating fun for $funSimpleName not generated! Only suspend fun or fun returning Flow is supported."
                append("    // $message\n")
                logger.warn(message, originalFunctionDeclaration)
            }
        }
    }

    private fun StringBuilder.appendDecoratingFunction(
        originalFunctionDeclaration: KSFunctionDeclaration,
        funSimpleName: String,
        originalResolvedReturnType: KSType,
        funModifiers: String,
        iteratorHelperMethodName: String,
    ) {
        var modifiers = funModifiers
        if (modifiers.isNotEmpty()) {
            modifiers += " "
        }

        append("    ${modifiers}fun $funSimpleName")
        appendDecoratingFunctionParameters(originalFunctionDeclaration)
        appendDecoratingFunctionReturnType(originalFunctionDeclaration, originalResolvedReturnType)
        append(" {\n")
        append("        return $DECORATIONS_PROPERTY_NAME.iterator().$iteratorHelperMethodName {\n")
        append("            $stubPropertyName.$funSimpleName")
        appendOriginalFunCallParams(originalFunctionDeclaration)
        append("        }\n")
        append("    }\n")
    }

    private fun StringBuilder.appendDecoratingFunctionParameters(originalFunctionDeclaration: KSFunctionDeclaration) {
        append("(")

        val params = originalFunctionDeclaration.parameters
        when (params.size) {
            0 -> append(")")
            1 -> {
                append(params[0].toFunParamDeclaration())
                append(")")
            }
            else -> {
                append("\n")
                originalFunctionDeclaration.parameters.forEach { parameter ->
                    append("        ${parameter.toFunParamDeclaration()},\n")
                }
                append("    )")
            }
        }
        append(": ")
    }

    private fun KSValueParameter.toFunParamDeclaration(): String {
        val paramTypeName = type.resolve().declaration.qualifiedName!!.asString()
        return "${getName()}: $paramTypeName"
    }

    private fun KSValueParameter.getName() = name!!.asString()

    /**
     * Appends either simple type (for suspend fun) or type with one type argument
     * (for streaming fun returning Flow) as a return type of the decorating function.
     */
    private fun StringBuilder.appendDecoratingFunctionReturnType(
        originalFunctionDeclaration: KSFunctionDeclaration,
        originalResolvedReturnType: KSType
    ) {
        append(originalResolvedReturnType.declaration.qualifiedName!!.asString())
        val typeArgs = originalFunctionDeclaration.returnType?.element?.typeArguments
        if (!typeArgs.isNullOrEmpty()) {
            append("<")
            append(typeArgs[0].type!!.resolve().declaration.qualifiedName!!.asString())
            append(">")
        }
    }

    private fun StringBuilder.appendOriginalFunCallParams(originalFunctionDeclaration: KSFunctionDeclaration) {
        append("(")

        val params = originalFunctionDeclaration.parameters
        when (params.size) {
            0 -> append(")")
            1 -> {
                append(params[0].getName())
                append(")")
            }
            else -> {
                append("\n")
                originalFunctionDeclaration.parameters.forEach { parameter ->
                    append("                ${parameter.getName()},\n")
                }
                append("            )")
            }
        }
        append("\n")
    }

    private fun KSType.isFlow(): Boolean {
        return declaration.qualifiedName!!.asString() == Flow::class.qualifiedName
    }

    companion object {

        private const val DECORATIONS_PROPERTY_NAME = "decorations"
    }
}
