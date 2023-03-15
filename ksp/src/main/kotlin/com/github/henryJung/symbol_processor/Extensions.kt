package com.github.henryJung.symbol_processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import kotlin.reflect.KClass

fun KSType.getName() = declaration.getName()
fun KSDeclaration.getName() = simpleName.asString()
fun KSClassDeclaration.className() = simpleName.getShortName()

fun KSClassDeclaration.isDataClass() = modifiers.contains(Modifier.DATA)

fun <T : Annotation> KSClassDeclaration.extractAnnotation(findAnnotation: KClass<T>) =
    annotations.first { targetClassAnnotations -> targetClassAnnotations.shortName.asString() == findAnnotation.simpleName }

@Throws(IllegalArgumentException::class)
fun KSPLogger.logAndThrowError(errorMessage: String, targetClass: KSClassDeclaration) {
    error(errorMessage, targetClass as KSNode)
    throw IllegalArgumentException(errorMessage)
}

fun KSDeclaration.containsSupertype(resolver: Resolver, searchedSuperType: KSDeclaration): Boolean {
    val classDeclaration =
        this.qualifiedName?.let(resolver::getClassDeclarationByName) ?: return false
    val containsSuperType = classDeclaration.superTypes.any { superType ->
        val comparableSuperTypeDeclaration = superType.resolve().declaration
        searchedSuperType.compareByQualifiedName(comparableSuperTypeDeclaration)
                || comparableSuperTypeDeclaration.containsSupertype(resolver, searchedSuperType)
    }

    return containsSuperType
}

fun KSType.compareByDeclaration(otherDeclaration: KSDeclaration): Boolean =
    this.declaration.compareByQualifiedName(otherDeclaration)

fun KSDeclaration.compareByQualifiedName(other: KSDeclaration): Boolean {
    val thisName = this.packageName.asString() + "." + this.simpleName.asString()
    val otherName = other.packageName.asString() + "." + other.simpleName.asString()
    return thisName == otherName
}

fun KSType.markedNullableAsString() = if (isMarkedNullable) "?" else ""
