package com.github.henryJung.ksp.processing.generator.argument

data class MatchingArgument(
    val targetClassPropertyName: String,
    val sourceClassPropertyName: String,
    val targetClassPropertyGenericTypeName: String? = null
)