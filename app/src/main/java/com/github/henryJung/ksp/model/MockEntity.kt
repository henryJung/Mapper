package com.github.henryJung.ksp.model

import com.devpub.annotation.Mapper
import com.devpub.annotation.MapperProperty
import com.github.henryJung.ksp.TestModel

@Mapper(TestModel::class)
data class MockEntity(
    val id: Long,
    @MapperProperty("name")
    val name222 : String,
    val dasd:String
)
