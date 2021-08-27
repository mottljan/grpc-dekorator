@file:Suppress("DEPRECATION_ERROR")

package processor

import api.annotation.DecoratorConfiguration
import api.annotation.RpcConfiguration
import api.decorator.DecoratorConfig
import api.decorator.GlobalDecoratorConfig
import api.internal.decorator.CoroutineStubDecorator
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
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream
import java.util.Locale
import kotlin.text.StringBuilder

private const val PACKAGE_NAME = "stub.decorator.wtf" // TODO Update once known and also update packages in modules

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
        val visibilityModifier = resolveVisibilityModifierForDecorator()
        val decoratorClassName = getStubDecoratorName(stubSimpleName)

        val decoratorConfigArgName = "decoratorConfig"
        val decoratorConfigArgDeclaration = "$decoratorConfigArgName: ${decoratorConfigDeclaration.qualifiedName!!.asString()}"

        val globalDecoratorConfigArgName = "globalDecoratorConfig"
        val globalDecoratorConfigArgDeclaration = "$globalDecoratorConfigArgName: ${globalDecoratorConfigResult.getConfigTypeQualifiedNameOrEmpty()}"
        val globalDecorationProviders = if (globalDecoratorConfigResult == GlobalDecoratorConfigResult.Missing) {
            "emptyList()"
        } else {
            "$globalDecoratorConfigArgName.${GlobalDecoratorConfig::decorationProviders.name}"
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
            |) : ${CoroutineStubDecorator::class.qualifiedName}() {
            |    
            |    private val $stubPropertyName = $decoratorConfigArgName.${DecoratorConfig<*>::getStub.name}()
            |    private val $STUB_DECORATION_PROVIDERS_PROPERTY_NAME = resolveDecorationProvidersBasedOnStrategy(
            |        $globalDecorationProviders,
            |        $decoratorConfigArgName.${DecoratorConfig<*>::getStubDecorationStrategy.name}()
            |    )
            """.trimMargin()
        )
        appendRpcsDecorationProvidersIfAny(decoratorConfigArgName)
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

    private fun GlobalDecoratorConfigResult.getConfigTypeQualifiedNameOrEmpty(): String {
        return if (this is GlobalDecoratorConfigResult.Exists) configTypeQualifiedName else ""
    }

    private fun StringBuilder.appendRpcsDecorationProvidersIfAny(decoratorConfigArgName: String) {
        rpcConfigResults.forEach { rpcConfigResult ->
            append("\n")
            val rpcProvidersPropName = getRpcDecorationProvidersPropName(rpcConfigResult.rpcName)
            append(
                """
                |    private val $rpcProvidersPropName = resolveDecorationProvidersBasedOnStrategy(
                |        $STUB_DECORATION_PROVIDERS_PROPERTY_NAME,
                |        $decoratorConfigArgName.${rpcConfigResult.rpcConfigMethodName}()
                |    )
                """.trimMargin()
            )
        }
    }

    private fun getRpcDecorationProvidersPropName(rpcName: String): String {
        return "$rpcName$RPC_DECORATION_PROVIDERS_PROPERTY_NAME_SUFFIX"
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
        appendDecoratingFunctionParameters(originalFunctionDeclaration)
        appendDecoratingFunctionReturnType(originalFunctionDeclaration, originalResolvedReturnType)
        append(" {\n")
        append("        return ${selectCorrectProvidersProperty(rpcName)}.iterator().$iteratorHelperMethodName {\n")
        append("            $stubPropertyName.$rpcName")
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

    private fun selectCorrectProvidersProperty(rpcName: String): String {
        val rpcHasCustomDecorations = rpcConfigResults.any { it.rpcName == rpcName }
        return if (rpcHasCustomDecorations) {
            getRpcDecorationProvidersPropName(rpcName)
        } else {
            STUB_DECORATION_PROVIDERS_PROPERTY_NAME
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

        private const val STUB_DECORATION_PROVIDERS_PROPERTY_NAME = "stubDecorationProviders"
        private const val RPC_DECORATION_PROVIDERS_PROPERTY_NAME_SUFFIX = "_decorationProviders"
    }
}
