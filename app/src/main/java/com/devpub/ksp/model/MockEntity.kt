package com.devpub.ksp.model

import com.devpub.annotation.Mapper
import com.devpub.annotation.MapperProperty
import com.devpub.ksp.TestModel

@Mapper(TestModel::class)
data class MockEntity(
    val id: Long,
    @MapperProperty("name")
    val name222 : String,
    val dasd:String
)
