package com.devpub.symbol_processor.mapper.generator.argument

import com.google.devtools.ksp.symbol.KSType

sealed class ArgumentType {
    class ArgumentClass(val ksType: KSType) : ArgumentType()
    object Asterix : ArgumentType()
}