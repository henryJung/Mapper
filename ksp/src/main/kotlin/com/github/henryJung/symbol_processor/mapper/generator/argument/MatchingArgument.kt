package com.github.henryJung.symbol_processor.mapper.generator.argument

data class MatchingArgument(
    val targetClassPropertyName: String,
    val sourceClassPropertyName: String,
    val targetClassPropertyGenericTypeName: String? = null
)