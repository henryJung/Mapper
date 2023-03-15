package com.devpub.annotation

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class MapperProperty(val alias: String = "")