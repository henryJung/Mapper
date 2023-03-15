package com.github.henryJung.ksp.processing.generator

import com.github.henryJung.annotation.MapperProperty
import com.github.henryJung.ksp.*
import com.github.henryJung.ksp.processing.generator.argument.ArgumentType
import com.github.henryJung.ksp.processing.generator.argument.MatchingArgument
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import kotlin.text.StringBuilder

class MappingFunctionGenerator(
    private val resolver: Resolver,
    private val logger: KSPLogger
) {

    fun generateMappingFunction(
        sourceClass: KSClassDeclaration,
        targetClass: KSClassDeclaration,
        packageImports: PackageImports,
    ): String {
        val targetClassName = targetClass.className()
        val targetPackageName = targetClass.containingFile!!.packageName.asString()
        val targetClassTypeParameters = targetClass.typeParameters

        packageImports.targetClassTypeParameters += targetClassTypeParameters
        packageImports.addImport(targetPackageName, targetClassName)
        packageImports.addImport(
            sourceClass.packageName.asString(),
            sourceClass.simpleName.asString()
        )

        return generateExtensionMapperFunction(
            sourceClass = sourceClass,
            targetClass = targetClass,
            targetClassTypeParameters = targetClassTypeParameters,
            targetClassName = targetClassName,
            packageImports = packageImports,
        )
    }

    private fun generateExtensionMapperFunction(
        sourceClass: KSClassDeclaration,
        targetClass: KSClassDeclaration,
        targetClassTypeParameters: List<KSTypeParameter>,
        targetClassName: String,
        packageImports: PackageImports,
    ): String {
        val sourceClassName = sourceClass.toString()
        val extensionFunctions = StringBuilder("")

        val (
            missingArguments: List<KSValueParameter>,
            matchingArguments: List<MatchingArgument>
        ) = getMatchingAndMissingArguments(
            targetClass = targetClass,
            sourceClass = sourceClass,
            targetClassTypeParameters = targetClassTypeParameters,
            packageImports = packageImports,
            sourceClassName = sourceClassName,
            targetClassName = targetClassName,
        )

        extensionFunctions.append("$KOTLIN_FUNCTION_KEYWORD ")

        appendTargetClassTypeParameter(
            extensionFunctions,
            targetClassTypeParameters,
            packageImports,
            missingArguments.isNotEmpty()
        )
        extensionFunctions.append(
            generateExtensionFunctionName(
                sourceClass,
                targetClass,
                packageImports
            )
        )
        extensionFunctions.append("$OPEN_FUNCTION\n")

        if (missingArguments.isNotEmpty()) {
            if (matchingArguments.isEmpty()) {
                logger.warn(
                    message = "$sourceClassName 클래스에서 ${targetClassName}으로 Mapping 가능한 인수가 없습니다.\n" +
                            "$sourceClassName 클래스의 ${MapperProperty::class.simpleName}로 인수 값을 맞추어 주세요.",
                    symbol = targetClass
                )
            }

            missingArguments.forEachIndexed { missingArgumentIndex, missingArgument ->
                extensionFunctions.append(
                    convertMissingArgumentToDeclarationText(
                        isLastIndex = missingArguments.lastIndex == missingArgumentIndex,
                        missingArgument = missingArgument,
                        packageImports = packageImports,
                        targetClass = targetClass
                    )
                )
            }
        }

        extensionFunctions.append("$CLOSE_FUNCTION = $targetClassName$OPEN_FUNCTION\n")

        matchingArguments.forEachIndexed { index, matchingArgument ->
            val lineEnding = getArgumentLineEnding(
                hasNextLine = missingArguments.lastIndex != index || missingArguments.isNotEmpty()
            )
            extensionFunctions.append("\t${matchingArgument.targetClassPropertyName} = this.${matchingArgument.sourceClassPropertyName}")

            matchingArgument.targetClassPropertyGenericTypeName?.let { targetClassPropertyGenericTypeName ->
                extensionFunctions.append(" as $targetClassPropertyGenericTypeName")
            }

            extensionFunctions.append("$lineEnding\n")
        }

        missingArguments.forEachIndexed { index, paramName ->
            val lineEnding =
                getArgumentLineEnding(hasNextLine = missingArguments.lastIndex != index)
            extensionFunctions.append("\t$paramName = $paramName$lineEnding\n")
        }

        extensionFunctions.append("$CLOSE_FUNCTION\n\n")

        return extensionFunctions.toString()
    }

    private fun appendTargetClassTypeParameter(
        extensionFunctions: StringBuilder,
        targetClassTypeParameters: List<KSTypeParameter>,
        packageImports: PackageImports,
        isMissingArguments: Boolean = false
    ) {
        if (targetClassTypeParameters.isNotEmpty()) {
            extensionFunctions.append(DIAMOND_OPERATOR_OPEN)
            targetClassTypeParameters.forEachIndexed { index, targetClassTypeParameter ->
                val separator = getArgumentLineEnding(
                    hasNextLine = targetClassTypeParameters.lastIndex != index,
                    addSpace = true
                )

                extensionFunctions.append(targetClassTypeParameter.name.asString())
                if (isMissingArguments) {
                    targetClassTypeParameter.bounds.firstOrNull()?.let { upperBound ->
                        packageImports.addImport(upperBound.resolve())
                        extensionFunctions.append(": $upperBound")
                    }
                }
                extensionFunctions.append(separator)
            }
            extensionFunctions.append(DIAMOND_OPERATOR_CLOSE)
        }
    }

    private fun convertMissingArgumentToDeclarationText(
        isLastIndex: Boolean,
        missingArgument: KSValueParameter,
        packageImports: PackageImports,
        targetClass: KSClassDeclaration
    ): String {
        val missingArgumentDeclarationText = StringBuilder("")
        val argumentTypes = mutableListOf<ArgumentType>()
        val missingArgumentType = missingArgument.type.resolve()

        packageImports.addImport(missingArgumentType)

        missingArgumentType.arguments.forEach { ksTypeArgument ->
            if (ksTypeArgument.variance == Variance.STAR) {
                argumentTypes.add(ArgumentType.Asterix)
            } else {
                val argumentClass = ksTypeArgument.type?.resolve()

                if (argumentClass != null) {
                    argumentTypes.add(ArgumentType.ArgumentClass(argumentClass))
                } else {
                    logger.logAndThrowError(
                        errorMessage = "${missingArgument.name}의 Type을 확인할 수 없습니다.",
                        targetClass = targetClass
                    )
                }
            }
        }

        missingArgumentDeclarationText.append("\t${missingArgument.name?.asString()}: ${missingArgumentType.getName()}")

        if (argumentTypes.isNotEmpty()) {
            missingArgumentDeclarationText.append(DIAMOND_OPERATOR_OPEN)
            argumentTypes.forEachIndexed { argumentTypeIndex, argumentType ->
                val typeSeparator = getArgumentLineEnding(
                    hasNextLine = argumentTypes.lastIndex != argumentTypeIndex,
                    addSpace = true
                )
                when (argumentType) {
                    is ArgumentType.ArgumentClass -> {
                        val argumentClass = argumentType.ksType

                        missingArgumentDeclarationText.append(
                            convertTypeArgumentToString(
                                argumentClass.getName(),
                                ArrayDeque(argumentClass.arguments)
                            )
                        )
                        missingArgumentDeclarationText.append(argumentClass.markedNullableAsString() + typeSeparator)
                        packageImports.addImport(argumentClass)
                    }
                    ArgumentType.Asterix -> {
                        missingArgumentDeclarationText.append("*$typeSeparator")
                    }
                }
            }
            missingArgumentDeclarationText.append(DIAMOND_OPERATOR_CLOSE)
        }

        missingArgumentDeclarationText.append(missingArgumentType.markedNullableAsString())

        val lineEnding = getArgumentLineEnding(hasNextLine = !isLastIndex)
        missingArgumentDeclarationText.append("$lineEnding\n")

        return missingArgumentDeclarationText.toString()
    }

    private fun getMatchingAndMissingArguments(
        targetClass: KSClassDeclaration,
        sourceClass: KSClassDeclaration,
        targetClassTypeParameters: List<KSTypeParameter>,
        packageImports: PackageImports,
        sourceClassName: String,
        targetClassName: String,
    ): Pair<MutableList<KSValueParameter>, MutableList<MatchingArgument>> {
        val propertyAnnotation = MapperProperty::class.simpleName
        val missingArguments = mutableListOf<KSValueParameter>()
        val matchingArguments = mutableListOf<MatchingArgument>()


        targetClass.primaryConstructor?.parameters?.forEach { targetParameter ->
            val targetParameterName = targetParameter.name?.asString()!!
            var matchingArgument: MatchingArgument? = null

            val targetAlias = targetParameter.annotations
                .firstOrNull { ksAnnotation -> ksAnnotation.shortName.asString() == propertyAnnotation }
                ?.arguments
                ?.firstOrNull()
                ?.value as? String

            sourceClass.getAllProperties().forEach { sourceParameter ->
                val sourceParameterName = sourceParameter.simpleName.asString()
                val sourceAlias = sourceParameter.annotations
                    .firstOrNull { ksAnnotation -> ksAnnotation.shortName.asString() == propertyAnnotation }
                    ?.arguments
                    ?.firstOrNull()
                    ?.value as? String

                if (sourceParameterName == targetParameterName
                    || sourceAlias == targetParameterName
                    || targetAlias == sourceParameterName
                ) {

                    val targetParameterType = targetParameter.type.resolve()
                    val sourceParameterType = sourceParameter.type.resolve()
                    val referencedTargetGenericTypeParameter =
                        targetClassTypeParameters.firstOrNull { targetTypeParam ->
                            targetTypeParam.simpleName.asString() == targetParameterType.getName()
                        }

                    val targetTypeParamUpperBoundDeclaration =
                        referencedTargetGenericTypeParameter
                            ?.bounds
                            ?.firstOrNull()
                            ?.resolve()
                            ?.declaration

                    if (targetTypeParamUpperBoundDeclaration != null
                        && sourceParameterType.declaration.containsSupertype(
                            resolver,
                            targetTypeParamUpperBoundDeclaration
                        )
                        && isKSTypeAssignable(
                            sourceParameterType = sourceParameterType,
                            targetParameterType = targetParameterType,
                            isGenericType = true
                        )
                    ) {
                        matchingArgument = MatchingArgument(
                            targetClassPropertyName = targetParameterName,
                            sourceClassPropertyName = sourceParameterName,
                            targetClassPropertyGenericTypeName = run {
                                if (targetTypeParamUpperBoundDeclaration.containingFile != null) {
                                    packageImports.addImport(
                                        targetTypeParamUpperBoundDeclaration.packageName.asString(),
                                        targetTypeParamUpperBoundDeclaration.getName()
                                    )
                                }
                                targetTypeParamUpperBoundDeclaration.getName() + targetParameterType.markedNullableAsString()
                            }
                        )
                    } else if (isKSTypeAssignable(
                            sourceParameterType = sourceParameterType,
                            targetParameterType = targetParameterType,
                            isGenericType = referencedTargetGenericTypeParameter != null
                        )
                    ) {
                        matchingArgument = MatchingArgument(
                            targetClassPropertyName = targetParameterName,
                            sourceClassPropertyName = sourceParameterName,
                            targetClassPropertyGenericTypeName = referencedTargetGenericTypeParameter?.let { typeParam ->
                                typeParam.simpleName.asString() + targetParameterType.markedNullableAsString()
                            }
                        )
                    } else {
                        logger.warn(
                            "$targetClassName 클래스의 `${targetParameterName} 속성에 대해 " +
                                    "$sourceClassName 클래스에서 일치하는 Parameter를 찾았지만 " +
                                    "${sourceParameterType}이 대상 유형 ${targetParameterType}과 일치하지 않습니다.",
                            targetClass
                        )
                    }
                }
            }

            if (matchingArgument != null) {
                matchingArguments.add(matchingArgument!!)
            } else if (!targetParameter.hasDefault) {
                missingArguments.add(targetParameter)
            }
        }

        return Pair(missingArguments, matchingArguments)
    }

    private fun convertTypeArgumentToString(
        typeText: String,
        typeParametersDequeue: ArrayDeque<KSTypeArgument>,
        shouldAddOpenOperator: Boolean = true
    ): String {
        val typeParameter = typeParametersDequeue.removeFirstOrNull() ?: return typeText
        val resolvedTypeParameter = typeParameter.type?.resolve() ?: return typeText
        val appendedTypeText = StringBuilder(typeText)

        if (shouldAddOpenOperator) {
            appendedTypeText.append(DIAMOND_OPERATOR_OPEN)
        }

        appendedTypeText.append(resolvedTypeParameter.getName())
        appendedTypeText.append(
            convertTypeArgumentToString(
                "",
                ArrayDeque(resolvedTypeParameter.arguments)
            )
        )

        val typeParamLineEnding = getArgumentLineEnding(
            hasNextLine = typeParametersDequeue.isNotEmpty(),
            addSpace = true
        )

        return if (typeParametersDequeue.isNotEmpty()) {
            appendedTypeText.append(typeParamLineEnding)

            convertTypeArgumentToString(
                typeText = appendedTypeText.toString(),
                typeParametersDequeue = typeParametersDequeue,
                shouldAddOpenOperator = false
            )
        } else {
            appendedTypeText.append(DIAMOND_OPERATOR_CLOSE)
            appendedTypeText.append(typeParamLineEnding)
            appendedTypeText.toString()
        }
    }

    private fun isKSTypeAssignable(
        sourceParameterType: KSType,
        targetParameterType: KSType,
        isGenericType: Boolean
    ): Boolean {
        if (sourceParameterType.isMarkedNullable && !targetParameterType.isMarkedNullable) {
            return false
        }
        if (!isGenericType && !sourceParameterType.compareByDeclaration(targetParameterType.declaration)) {
            return false
        }

        return isGenericType || sourceParameterType.arguments.matches(targetParameterType.arguments)
    }

    private fun List<KSTypeArgument>.matches(otherArguments: List<KSTypeArgument>): Boolean {
        if (isEmpty() == otherArguments.isEmpty()) {
            return true
        }
        if (size != otherArguments.size) {
            return false
        }
        return this.all { thisArgument ->
            otherArguments.firstOrNull { otherArgument ->
                val argTypeFromThis = thisArgument.type?.resolve() ?: return@all false
                val argTypeFromOther = otherArgument.type?.resolve() ?: return@all false
                val hasSameTypeName =
                    argTypeFromThis.compareByDeclaration(argTypeFromOther.declaration)
                val matchesNullability =
                    !argTypeFromThis.isMarkedNullable || !argTypeFromOther.isMarkedNullable

                return@firstOrNull hasSameTypeName &&
                        matchesNullability &&
                        (thisArgument.type?.resolve()?.arguments?.matches(argTypeFromOther.arguments) == true)
            } != null
        }
    }

    private fun generateExtensionFunctionName(
        sourceClass: KSDeclaration,
        targetClass: KSDeclaration,
        packageImports: PackageImports
    ): String {
        val sourceClassName = sourceClass.getName()
        val targetClassName = targetClass.getName()

        val sourceClassType = sourceClass.typeParameters.firstOrNull()?.let { ksTypeParameter ->
            val upperBound =
                ksTypeParameter.bounds.firstOrNull()?.resolve()?.let { upperBoundType ->
                    packageImports.addImport(upperBoundType)
                    upperBoundType.getName()
                } ?: ""
            DIAMOND_OPERATOR_OPEN + upperBound + DIAMOND_OPERATOR_CLOSE
        } ?: ""

        return "$sourceClassName$sourceClassType.to$targetClassName"
    }

    private fun getArgumentLineEnding(
        hasNextLine: Boolean,
        addSpace: Boolean = false
    ): String = if (hasNextLine) "," + if (addSpace) " " else "" else ""

    companion object {
        private const val DIAMOND_OPERATOR_OPEN = "<"
        private const val DIAMOND_OPERATOR_CLOSE = ">"
        private const val KOTLIN_FUNCTION_KEYWORD = "fun"
        private const val CLOSE_FUNCTION = ")"
        private const val OPEN_FUNCTION = "("
    }
}
