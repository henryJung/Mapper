# Mapper

Mapper는 Model(Data class)간 Mapping해주는 extension function을 만들어주는 **Kotlin Symbol Processing (KSP)** 플러그인 입니다.

Layer간 Model 변환을 위해 Mapper class를 정의해야하는데 이 것을 KSP를 통해 자동으로 생성해 불필요한 코드 작성을 줄일 수 있습니다.

`@Mapper` 를 통해 Target data class와 Source data class의 Mapper를 extension function으로 만들어주어 개발 생산성에 기여합니다.

---

## Mapper 사용 방법

`MockEntity`(Source data class)와 `TestModel`(Target data class)이 있다고 가정하고 이 두개의 Model을 서로 연결해서 사용해야 한다면 다음과 같이 사용하실 수 있습니다.

`MockEntity`(Source data class)는 다음과 같습니다.
```kt
data class MockEntity(
    val id: Long,
    val name2 : String,
    val mockString:String
)
```

연결을 원하는 `TestModel`(Target data class)는 다음과 같습니다.
```kt
data class TestModel(
    val id : Long,
    val name : String
)
```

이 두개의 Model을 연결하기 위해서는 Source data class(`MockEntity`)에 `@Mapper([TargetClass]:Class)`를 지정하면 빌드 시 Extension function이 해당 패키지 하단 mapper 패키지 안에 자동 생성됩니다.

Target data class → Source data class 는 `[Target data class].to[Source data clas]()`
Source data class ← Target data class 는 `[Source data clas].to[Target data class]()`

### Code level

1. Source data class(**Data class만 가능**) 상단에 `@Mapper([TargetClass]:Class)`어노테이션을 삽입.
  - 만약, property의 이름이 맞지 않아 맞춰줄 필요가 있으면 `@MapperProperty("[Name]")` 을 해당 property에 어노테이션을 삽입.(gson `@SerializedName("[Name]")` 과 동일한 방식)

`MockEntity`(Source data class)
```kt
import com.github.henryJung.annotation.Mapper
import com.github.henryJung.annotation.MapperProperty
import com.github.henryJung.app.TestModel

@Mapper(TestModel::class)
data class MockEntity(
    val id: Long,
    @MapperProperty("name")
    val name2 : String,
    val mockString:String
)
```
2. 빌드 후 해당 패키지 하단에 mapper 패키지 안에 Extension function이 양방향으로 생성 된다.
    
```kt
import com.github.henryJung.app.TestModel
import com.github.henryJung.app.model.MockEntity
import kotlin.String

fun MockEntity.toTestModel(
) = TestModel(
	id = this.id,
	name = this.name2,
)

fun TestModel.toMockEntity(
	mockString: String
) = MockEntity(
	id = this.id,
	name2 = this.name,
	mockString = mockString
)
```

3. 사용하는 부분에서 생성된 extension function을 호출하여 사용.


---

## Setup
프로젝트에서 **Mapper** KSP plugin를 설치하기 위해서는 다음과 같이 설정합니다.

#### 1. KSP (Kotlin Symbol Processing) plugin 추가합니다.

> KSP 리포지토리의 공식 GitHub 릴리스 페이지에서 일치하는 KSP 버전을 찾습니다.
예를 들어 Kotlin 버전을 사용하는 경우 최신 KSP는 1.8.0-1.0.8.

<details open>
  <summary>groovy - build.gradle(:project-name)</summary>

```groovy
plugins {
    id 'com.google.devtools.ksp' version '1.8.0-1.0.8'
}
```
</details>

<details>
  <summary>kotlin - build.gradle.kts(:project-name)</summary>  

```kt
plugins {
    id("com.google.devtools.ksp") version "1.8.0-1.0.8"
}
```
</details>

#### 2. Dependency를 추가합니다.
**Mapper** plugin을 dependency에 추가합니다.

<details open>
  <summary>groovy - build.gradle(:module-name)</summary>

```groovy
repositories {
    maven { url = 'https://jitpack.io' }    
}

dependencies {
    // ..
    implementation 'com.github.henryJung.mapper:annotation:1.0.0'
    ksp 'com.github.henryJung.mapper:ksp:1.0.0'
}
```
</details>

<details>
  <summary>kotlin - build.gradle.kts(:module-name)</summary>  

```kt
repositories {
    maven { url = uri("https://jitpack.io") }    
}

dependencies {
 // ..
 implementation("com.github.henryJung.mapper:annotation:1.0.0")
 ksp("com.github.henryJung.mapper:ksp:1.0.0")
}
```
</details>

#### 3. source set을 추가합니다.

<details>
    <summary>Android project</summary>

```kt
android {
    sourceSets.configureEach {
        kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
    }
}
 ```

</details>

<details>
<summary>Kotlin JVM or equal project</summary>

```kt
kotlin.sourceSets {
    getByName(name) {
        kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin")
    }
}
 ```
</details>

---

License
=======
    Copyright 2023 henryJung

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
