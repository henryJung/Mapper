package com.github.henryJung.ksp.processing

import com.github.henryJung.annotation.Mapper
import com.github.henryJung.ksp.getName
import com.github.henryJung.ksp.isDataClass
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.github.henryJung.ksp.logAndThrowError

class MapperSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(annotationName)
        val ret = symbols.filterNot { ksAnnotated -> ksAnnotated.validate() }.toList()

        symbols.filter { ksAnnotated ->
            ksAnnotated is KSClassDeclaration && ksAnnotated.validate()
        }.forEach { ksAnnotated ->
            val classDeclaration = ksAnnotated as KSClassDeclaration
            when (classDeclaration.classKind) {
                ClassKind.CLASS -> {
                    if (classDeclaration.isDataClass()) {
                        classDeclaration.accept(
                            MapperVisitor(codeGenerator, resolver, logger),
                            Unit
                        )
                    } else {
                        logger.error(
                            "${annotationName}은 DataClass만 사용할 수 있습니다.($classDeclaration)",
                            classDeclaration
                        )
                    }
                }
                else -> {
                    logger.logAndThrowError(
                        errorMessage = "Cannot generate function for class `${classDeclaration.getName()}`, " +
                                "class type `${classDeclaration.classKind}` is not supported.",
                        targetClass = classDeclaration
                    )
                }
            }
        }

        return ret
    }

    companion object {
        private val annotationName = Mapper::class.java.canonicalName
    }
}