@file:Suppress("DEPRECATION_ERROR")

package io.github.mottljan.processor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import io.github.mottljan.api.annotation.DecoratorConfiguration
import io.github.mottljan.api.annotation.RpcConfiguration
import io.github.mottljan.api.decoration.Decoration
import io.github.mottljan.api.decorator.DecoratorConfig
import io.github.mottljan.api.decorator.GlobalDecoratorConfig
import io.github.mottljan.api.internal.decorator.CoroutineStubDecorator
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream
import java.util.Locale

private const val PACKAGE_NAME = "io.github.mottljan.decorator"

/**
 * Visitor which handles [DecoratorConfiguration] annotations and generates decorator classes based
 * on the provided configuration by the annotated class.
 */
internal class DecoratorConfigVisitor(
    private val environment: SymbolProcessorEnvironment,
    private val globalDecoratorConfigResult: GlobalDecoratorConfigResult
) : KSVisitorVoid() {

    private val logger = environment.logger

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val implementsRequiredInterface = classDeclaration.superTypes.any {
            it.resolve().declaration.qualifiedName!!.asString() == DecoratorConfig::class.qualifiedName
        }
        if (implementsRequiredInterface) {
            val stubResolvedType = classDeclaration.getAllFunctions().resolveStub()
            val rpcConfigResults = classDeclaration.getRpcConfigResults(stubResolvedType)
            generateStubDecorator(classDeclaration, stubResolvedType, rpcConfigResults)
        } else {
            val message = DecoratorProcessor.generateMissingInterfaceImplErrorMsg(classDeclaration.simpleName.asString())
            logger.error(message, classDeclaration)
        }
    }

    private fun Sequence<KSFunctionDeclaration>.resolveStub(): KSType {
        return find { it.simpleName.asString() == DecoratorConfig<*>::getStub.name }!!.returnType!!.resolve()
    }

    private fun KSClassDeclaration.getRpcConfigResults(stubResolvedType: KSType): List<RpcConfigResult> {
        return getAllFunctions()
            .associateWith { funDeclaration ->
                funDeclaration.annotations.find { annotation ->
                    val name = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
                    name == RpcConfiguration::class.qualifiedName
                }
            }
            .filterNot { it.value == null }
            .map { entry ->
                val visitorInputData = RpcConfigurationVisitor.InputData(entry.value!!, stubResolvedType)
                entry.key.accept(RpcConfigurationVisitor(environment), visitorInputData)
            }
    }

    private fun generateStubDecorator(
        decoratorConfigDeclaration: KSClassDeclaration,
        stubResolvedType: KSType,
        rpcConfigResults: List<RpcConfigResult>
    ) {
        val stubSimpleName = stubResolvedType.declaration.simpleName.asString()
        val fileOutputStream = createDecoratorFile(name = getStubDecoratorName(stubSimpleName))
        fileOutputStream += DecoratorFileContentGenerator(
            environment = environment,
            stubSimpleName = stubSimpleName,
            decoratorConfigDeclaration = decoratorConfigDeclaration,
            stubResolvedType = stubResolvedType,
            globalDecoratorConfigResult = globalDecoratorConfigResult,
            rpcConfigResults = rpcConfigResults
        ).generate()
        fileOutputStream.close()
    }

    private fun createDecoratorFile(name: String): OutputStream {
        return environment.codeGenerator.createNewFile(
            // TODO ALL_FILES has to be used instead of this line to keep generated files for multiple
            //  consecutive KSP runs. If Dependencies(false) is used, files are generated for the first run
            //  and for the second it is deleted and not generated again, then the ksp build folder
            //  needs to be deleted to generate files again. Using ALL_FILES the generated files are never
            //  generated for multiple ksp runs and it does not even regenerate files and use caching properly,
            //  so if files already exist, the kspKotlin is UP_TO_DATE and does not run again. Not
            //  sure if bug or I just don't understand how this works.
//            dependencies = Dependencies(false),
            dependencies = Dependencies.ALL_FILES,
            packageName = PACKAGE_NAME,
            fileName = name
        )
    }
}

private fun getStubDecoratorName(stubSimpleName: String) = "${stubSimpleName}Decorator"

private class DecoratorFileContentGenerator(
    environment: SymbolProcessorEnvironment,
    private val stubSimpleName: String,
    private val decoratorConfigDeclaration: KSClassDeclaration,
    private val stubResolvedType: KSType,
    private val globalDecoratorConfigResult: GlobalDecoratorConfigResult,
    private val rpcConfigResults: List<RpcConfigResult>
) {

    private val stubPropertyName = stubSimpleName.replaceFirstChar { it.lowercaseChar() }

    private val logger = environment.logger

    private val decorationsWrapperClassesNames = mutableMapOf<String, String>()

    fun generate(): String {
        return buildString {
            appendPackage()
            appendClassHeaderAndProperties()
            appendDecoratingFunctions()
            appendDecorationsWrapperClasses()
            append("}")
            append("\n")
        }
    }

    private fun StringBuilder.appendPackage() {
        append("package $PACKAGE_NAME\n\n")
    }

    private fun StringBuilder.appendClassHeaderAndProperties() {
        val visibilityModifier = resolveVisibilityModifierForDecorator()
        val decoratorClassName = getStubDecoratorName(stubSimpleName)

        val decoratorConfigArgName = "decoratorConfig"
        val decoratorConfigArgDeclaration = "$decoratorConfigArgName: ${decoratorConfigDeclaration.qualifiedName!!.asString()}"

        val globalDecoratorConfigArgName = "globalDecoratorConfig".orEmptyIfGlobalConfigMissing()
        val globalDecoratorConfigArgDeclaration = "$globalDecoratorConfigArgName: ${globalDecoratorConfigResult.getConfigTypeQualifiedNameOrEmpty()}"
        val globalDecorations = if (globalDecoratorConfigResult == GlobalDecoratorConfigResult.Missing) {
            "emptyList()"
        } else {
            "$globalDecoratorConfigArgName.${GlobalDecoratorConfig::decorations.name}"
        }

        val propsDeclarations = if (globalDecoratorConfigResult == GlobalDecoratorConfigResult.Missing) {
            decoratorConfigArgDeclaration
        } else {
            """
            |$globalDecoratorConfigArgDeclaration,
            |    $decoratorConfigArgDeclaration
            """.trimMargin()
        }

        append(
            """
            |@Suppress("DEPRECATION_ERROR", "PrivatePropertyName")
            |${visibilityModifier}class $decoratorClassName(
            |    $propsDeclarations
            |) : ${CoroutineStubDecorator::class.qualifiedName}($globalDecoratorConfigArgName) {
            |    
            |    private val $stubPropertyName = $decoratorConfigArgName.${DecoratorConfig<*>::getStub.name}()
            |    private val $STUB_DECORATIONS_PROPERTY_NAME = resolveDecorationsBasedOnStrategy(
            |        $globalDecorations,
            |        $decoratorConfigArgName.${DecoratorConfig<*>::getStubDecorationStrategy.name}()
            |    )
            """.trimMargin()
        )
        appendRpcsDecorationsIfAny(decoratorConfigArgName)
        append("\n")
    }

    private fun resolveVisibilityModifierForDecorator(): String {
        return stubResolvedType.declaration.modifiers
            .getVisibility()
            ?.toString()
            ?.lowercase(Locale.getDefault())
            ?.let { "$it " }
            ?: ""
    }

    /**
     * Returns visibility [Modifier] and null if there is no explicit modifier (so public is used)
     */
    private fun Set<Modifier>.getVisibility(): Modifier? {
        return find {
            it == Modifier.PRIVATE ||
                it == Modifier.PROTECTED ||
                it == Modifier.INTERNAL ||
                it == Modifier.PUBLIC
        }
    }

    private fun String.orEmptyIfGlobalConfigMissing(): String {
        return if (globalDecoratorConfigResult is GlobalDecoratorConfigResult.Exists) this else ""
    }

    private fun GlobalDecoratorConfigResult.getConfigTypeQualifiedNameOrEmpty(): String {
        return if (this is GlobalDecoratorConfigResult.Exists) configTypeQualifiedName else ""
    }

    private fun StringBuilder.appendRpcsDecorationsIfAny(decoratorConfigArgName: String) {
        rpcConfigResults.forEach { rpcConfigResult ->
            append("\n")
            val rpcDecorationsWrapperPropName = getRpcDecorationsPropName(rpcConfigResult.rpcName)
            val rpcDecorationsWrapperClassName = rpcDecorationsWrapperPropName.replaceFirstChar { it.uppercaseChar() }

            append(
                """
                |    val $rpcDecorationsWrapperPropName = $rpcDecorationsWrapperClassName(resolveDecorationsBasedOnStrategy(
                |        $STUB_DECORATIONS_PROPERTY_NAME,
                |        $decoratorConfigArgName.${rpcConfigResult.rpcConfigMethodName}()
                |    ))
                """.trimMargin()
            )

            decorationsWrapperClassesNames[rpcConfigResult.rpcName] = rpcDecorationsWrapperClassName
        }
    }

    private fun getRpcDecorationsPropName(rpcName: String): String {
        return "$rpcName$RPC_DECORATIONS_PROPERTY_NAME_SUFFIX"
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

        val rpcName = originalFunctionDeclaration.simpleName.asString()
        val originalResolvedReturnType = originalFunctionDeclaration.returnType!!.resolve()

        when {
            originalFunctionDeclaration.modifiers.contains(Modifier.SUSPEND) -> {
                appendDecoratingFunction(
                    originalFunctionDeclaration = originalFunctionDeclaration,
                    rpcName = rpcName,
                    originalResolvedReturnType = originalResolvedReturnType,
                    funModifiers = "suspend",
                    iteratorHelperMethodName = "applyNextDecorationOrCallRpc"
                )
            }
            originalResolvedReturnType.isFlow() -> {
                appendDecoratingFunction(
                    originalFunctionDeclaration = originalFunctionDeclaration,
                    rpcName = rpcName,
                    originalResolvedReturnType = originalResolvedReturnType,
                    funModifiers = "",
                    iteratorHelperMethodName = "applyNextDecorationOrCallStreamRpc"
                )
            }
            else -> {
                val message = DecoratorProcessor.generateNotGeneratedFunctionWarningMsg(rpcName)
                append("    // $message\n")
                logger.warn(message, originalFunctionDeclaration)
            }
        }
    }

    private fun StringBuilder.appendDecoratingFunction(
        originalFunctionDeclaration: KSFunctionDeclaration,
        rpcName: String,
        originalResolvedReturnType: KSType,
        funModifiers: String,
        iteratorHelperMethodName: String,
    ) {
        var modifiers = funModifiers
        if (modifiers.isNotEmpty()) {
            modifiers += " "
        }

        append("    ${modifiers}fun $rpcName")
        appendDecoratingFunctionParameters(rpcName, originalFunctionDeclaration)
        appendDecoratingFunctionReturnType(originalFunctionDeclaration, originalResolvedReturnType)
        append(" {\n")
        append("        return ${selectCorrectDecorationsProperty(rpcName)}.iterator().$iteratorHelperMethodName {\n")
        append("            $stubPropertyName.$rpcName")
        appendOriginalFunCallParams(originalFunctionDeclaration)
        append("        }\n")
        append("    }\n")
    }

    @Suppress("MoveVariableDeclarationIntoWhen")
    private fun StringBuilder.appendDecoratingFunctionParameters(
        rpcName: String,
        originalFunctionDeclaration: KSFunctionDeclaration
    ) {
        append("(")

        val decorationsWrapperClassName = decorationsWrapperClassesNames[rpcName]

        val rpcParams = originalFunctionDeclaration.parameters
        val totalParamsSize = rpcParams.size + (if (decorationsWrapperClassName == null) 0 else 1)
        when (totalParamsSize) {
            0 -> append(")")
            1 -> {
                rpcParams.getOrNull(0)?.let { append(it.toFunParamDeclaration()) }
                appendCustomDecorationsParamIfNeeded(decorationsWrapperClassName, "", shouldWrapLine = false)
                append(")")
            }
            else -> {
                append("\n")
                val indentation = "        "
                originalFunctionDeclaration.parameters.forEach { parameter ->
                    append("$indentation${parameter.toFunParamDeclaration()},\n")
                }
                appendCustomDecorationsParamIfNeeded(decorationsWrapperClassName, indentation, shouldWrapLine = true)
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

    private fun StringBuilder.appendCustomDecorationsParamIfNeeded(
        decorationsWrapperClassName: String?,
        indentation: String,
        shouldWrapLine: Boolean
    ) {
        if (decorationsWrapperClassName != null) {
            append("${indentation}$RPC_CUSTOM_DECORATIONS_PARAM_NAME: $decorationsWrapperClassName")
            if (shouldWrapLine) {
                append("\n")
            }
        }
    }

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

    private fun selectCorrectDecorationsProperty(rpcName: String): String {
        val rpcHasCustomDecorations = rpcConfigResults.any { it.rpcName == rpcName }
        return if (rpcHasCustomDecorations) {
            "$RPC_CUSTOM_DECORATIONS_PARAM_NAME.$RPC_DECORATIONS_WRAPPER_CLASS_PROP_NAME"
        } else {
            STUB_DECORATIONS_PROPERTY_NAME
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

    private fun StringBuilder.appendDecorationsWrapperClasses() {
        decorationsWrapperClassesNames.values.forEach { className ->
            val propName = RPC_DECORATIONS_WRAPPER_CLASS_PROP_NAME
            append("\n    data class $className(val $propName: List<${Decoration::class.qualifiedName}>)\n")
        }
    }

    companion object {

        private const val STUB_DECORATIONS_PROPERTY_NAME = "stub_decorations"
        private const val RPC_DECORATIONS_PROPERTY_NAME_SUFFIX = "Decorations"
        private const val RPC_DECORATIONS_WRAPPER_CLASS_PROP_NAME = "value"
        private const val RPC_CUSTOM_DECORATIONS_PARAM_NAME = "customDecorations"
    }
}
