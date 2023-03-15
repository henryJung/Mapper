package com.github.henryJung.ksp.processing.generator.argument

import com.google.devtools.ksp.symbol.KSType

sealed class ArgumentType {
    class ArgumentClass(val ksType: KSType) : ArgumentType()
    object Asterix : ArgumentType()
}