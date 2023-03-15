package com.github.henryJung.ksp.processing.generator

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.github.henryJung.ksp.getName
import java.lang.StringBuilder

class PackageImports(var targetClassTypeParameters: Set<KSTypeParameter> = mutableSetOf()) {

    private val imports = mutableSetOf<Pair<String, String>>()

    fun addImport(packageName: String, className: String) {
        imports.add(packageName to className)
    }

    fun addImport(ksType: KSType) {
        val declaration = ksType.declaration
        val packageName = declaration.packageName.asString()
        val className = declaration.getName()

        imports.add(packageName to className)
    }

    fun asFormattedImports(): String {
        val importText = StringBuilder("")
        val typeParams = targetClassTypeParameters.map { parameter -> parameter.simpleName.asString() }

        imports.forEachIndexed { index, import ->
            val packageName = import.first
            val className = import.second

            if (typeParams.contains(className)) return@forEachIndexed

            importText.append("$IMPORT_STATEMENT $packageName.$className\n")

            if (imports.size - 1 == index) {
                importText.append("\n")
            }
        }

        return importText.toString()
    }

    companion object {
        private const val IMPORT_STATEMENT = "import"
    }
}