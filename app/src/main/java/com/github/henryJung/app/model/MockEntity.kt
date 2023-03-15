package com.github.henryJung.app.model

import com.github.henryJung.annotation.Mapper
import com.github.henryJung.annotation.MapperProperty
import com.github.henryJung.app.TestModel

@Mapper(TestModel::class)
data class MockEntity(
    val id: Long,
    @MapperProperty("name")
    val name222 : String,
    val dasd:String
)
