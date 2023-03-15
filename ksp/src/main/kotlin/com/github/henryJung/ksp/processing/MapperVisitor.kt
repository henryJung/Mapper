package com.github.henryJung.ksp.processing

import com.github.henryJung.annotation.Mapper
import com.github.henryJung.ksp.className
import com.github.henryJung.ksp.extractAnnotation
import com.github.henryJung.ksp.processing.generator.MappingFunctionGenerator
import com.github.henryJung.ksp.processing.generator.PackageImports
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import java.io.OutputStream

class MapperVisitor(
    private val codeGenerator: CodeGenerator,
    private val resolver: Resolver,
    private val logger: KSPLogger
) : KSVisitorVoid() {

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        classDeclaration.primaryConstructor!!.accept(this, data)
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        val annotatedClass = function.parentDeclaration as KSClassDeclaration
        val annotation = annotatedClass.extractAnnotation(Mapper::class)
        val argumentClassType = annotation.arguments.first().value as KSType
        val targetClass =
            resolver.getClassDeclarationByName(argumentClassType.declaration.qualifiedName!!)

        if (targetClass == null) {
            logger.error("${annotation}에 Mapping 할 Class를 찾을 수 없습니다.", annotatedClass)
            return
        }

        val packageImports = PackageImports()
        val mappingFunctionGenerator = MappingFunctionGenerator(
            resolver = resolver,
            logger = logger
        )
        val extensionFunctions = StringBuilder("")
        extensionFunctions.append(
            mappingFunctionGenerator.generateMappingFunction(
                sourceClass = annotatedClass,
                targetClass = targetClass,
                packageImports = packageImports
            )
        )

        extensionFunctions.append(
            mappingFunctionGenerator.generateMappingFunction(
                sourceClass = targetClass,
                targetClass = annotatedClass,
                packageImports = packageImports,
            )
        )

        generateCode(
            annotation = annotation,
            containingFile = function.containingFile!!,
            sourceClass = annotatedClass,
            packageImports = packageImports,
            extensionFunctions = extensionFunctions.toString()
        )
    }

    private fun generateCode(
        annotation: KSAnnotation,
        containingFile: KSFile,
        sourceClass: KSClassDeclaration,
        packageImports: PackageImports,
        extensionFunctions: String
    ) {
        val classPackage = sourceClass.packageName.asString()
        val className = sourceClass.className()
        val annotationName = annotation.shortName.getShortName()
        val packageName = "$classPackage.${annotationName.lowercase()}"

        codeGenerator.createNewFile(
            dependencies = Dependencies(true, containingFile),
            packageName = packageName,
            fileName = "$className$annotationName"
        ).use { generatedFileOutputStream ->
            if (packageImports.targetClassTypeParameters.isNotEmpty()) {
                generatedFileOutputStream.appendText(SUPPRESS_UNCHECKED_CAST)
            }
            generatedFileOutputStream.appendText("$PACKAGE $packageName\n\n")
            generatedFileOutputStream.appendText(packageImports.asFormattedImports())
            generatedFileOutputStream.appendText(extensionFunctions)
        }
    }

    private fun OutputStream.appendText(str: String) {
        this.write(str.toByteArray())
    }

    companion object {
        private const val PACKAGE = "package"
        private const val SUPPRESS_UNCHECKED_CAST = "@file:Suppress(\"UNCHECKED_CAST\")\n\n"
    }
}