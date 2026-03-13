# Using ProtoStream with Kotlin

ProtoStream supports Kotlin entities via [kapt](https://kotlinlang.org/docs/kapt.html) (Kotlin Annotation Processing Tool). This guide covers how to annotate Kotlin classes and configure your build.

## Key rule: use `@get:ProtoField`

Kotlin properties have private backing fields. The ProtoStream annotation processor rejects `@ProtoField` on private fields. Use the `@get:` use-site target to place the annotation on the generated getter method instead:

```kotlin
// Wrong - annotation lands on the private backing field
@ProtoField(number = 1)
var name: String? = null

// Correct - annotation lands on the getter method
@get:ProtoField(number = 1)
var name: String? = null
```

This applies to both `var` and `val` properties, including data class constructor parameters.

## Annotating Kotlin classes

### Mutable class

```kotlin
class Person {
    @get:ProtoField(number = 1)
    var name: String? = null

    @get:ProtoField(number = 2, defaultValue = "0")
    var age: Int = 0
}
```

### Immutable data class

Use `@ProtoFactory` on the constructor and `@get:ProtoField` on each property:

```kotlin
data class Person @ProtoFactory constructor(
    @get:ProtoField(number = 1) val name: String,
    @get:ProtoField(number = 2, defaultValue = "0") val age: Int,
    @get:ProtoField(number = 3) val email: String?
)
```

### Data class with default values

Kotlin default parameter values work as expected:

```kotlin
data class Config @ProtoFactory constructor(
    @get:ProtoField(number = 1) val name: String,
    @get:ProtoField(number = 2, defaultValue = "8080") val port: Int = 8080,
    @get:ProtoField(number = 3) val description: String? = null
)
```

### Enums

Enum annotations do not need the `@get:` prefix:

```kotlin
enum class Status {
    @ProtoEnumValue(number = 0) ACTIVE,
    @ProtoEnumValue(number = 1) INACTIVE,
    @ProtoEnumValue(number = 2) PENDING
}
```

### Collections

```kotlin
class Order {
    @get:ProtoField(number = 1)
    var id: String? = null

    @get:ProtoField(number = 2)
    var items: List<String>? = null
}
```

### Boolean `is`-prefix properties

Kotlin boolean properties named with an `is` prefix (e.g. `isActive`) generate non-standard accessor names (`isActive()` / `setActive()`). ProtoStream handles this correctly:

```kotlin
class Feature {
    @get:ProtoField(number = 1, defaultValue = "false")
    var isEnabled: Boolean = false

    @get:ProtoField(number = 2, defaultValue = "false")
    var isVisible: Boolean = false
}
```

## Defining a schema

Define your schema interface exactly as in Java:

```kotlin
@ProtoSchema(
    schemaFileName = "my_schema.proto",
    schemaPackageName = "com.example",
    includeClasses = [Person::class, Status::class, Order::class],
    syntax = ProtoSyntax.PROTO3
)
interface MySchema : GeneratedSchema
```

The annotation processor generates `MySchemaImpl`. Use it to register your schema:

```kotlin
val ctx = ProtobufUtil.newSerializationContext()
MySchemaImpl().register(ctx)

// Serialize
val bytes = ProtobufUtil.toWrappedByteArray(ctx, person)

// Deserialize
val restored = ProtobufUtil.fromWrappedByteArray<Person>(ctx, bytes)
```

## Build configuration

### Maven

```xml
<properties>
    <version.kotlin>2.1.10</version.kotlin>
    <version.protostream>6.0.6</version.protostream>
</properties>

<dependencies>
    <dependency>
        <groupId>org.jetbrains.kotlin</groupId>
        <artifactId>kotlin-stdlib</artifactId>
        <version>${version.kotlin}</version>
    </dependency>
    <dependency>
        <groupId>org.infinispan.protostream</groupId>
        <artifactId>protostream</artifactId>
        <version>${version.protostream}</version>
    </dependency>
    <dependency>
        <groupId>org.infinispan.protostream</groupId>
        <artifactId>protostream-processor</artifactId>
        <version>${version.protostream}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>
    <plugins>
        <!-- Kotlin plugin must be declared before maven-compiler-plugin -->
        <plugin>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-plugin</artifactId>
            <version>${version.kotlin}</version>
            <executions>
                <execution>
                    <id>kapt</id>
                    <goals>
                        <goal>kapt</goal>
                    </goals>
                    <configuration>
                        <sourceDirs>
                            <sourceDir>${project.basedir}/src/main/kotlin</sourceDir>
                        </sourceDirs>
                        <annotationProcessorPaths>
                            <annotationProcessorPath>
                                <groupId>org.infinispan.protostream</groupId>
                                <artifactId>protostream-processor</artifactId>
                                <version>${version.protostream}</version>
                            </annotationProcessorPath>
                        </annotationProcessorPaths>
                    </configuration>
                </execution>
                <execution>
                    <id>compile</id>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                    <configuration>
                        <sourceDirs>
                            <sourceDir>${project.basedir}/src/main/kotlin</sourceDir>
                            <sourceDir>${project.build.directory}/generated-sources/kapt/compile</sourceDir>
                        </sourceDirs>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <executions>
                <execution>
                    <id>default-compile</id>
                    <phase>none</phase>
                </execution>
                <execution>
                    <id>java-compile</id>
                    <phase>compile</phase>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                    <configuration>
                        <proc>none</proc>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Gradle (Kotlin DSL)

```kotlin
plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("kapt") version "2.1.10"
}

dependencies {
    implementation("org.infinispan.protostream:protostream:6.0.6")
    kapt("org.infinispan.protostream:protostream-processor:6.0.6")
}
```

### Gradle (Groovy DSL)

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm' version '2.1.10'
    id 'org.jetbrains.kotlin.kapt' version '2.1.10'
}

dependencies {
    implementation 'org.infinispan.protostream:protostream:6.0.6'
    kapt 'org.infinispan.protostream:protostream-processor:6.0.6'
}
```

## Kotlin-specific considerations

### Visibility

Annotated properties and classes must be `public` or `protected`. Kotlin `internal` visibility causes name mangling in bytecode that the annotation processor cannot resolve.

### Nullability and proto3

Proto3 does not distinguish between null and default values for scalar types. A null `String?` will deserialize as `""`, a null `Int?` as `0`, etc. Design your Kotlin types accordingly if using proto3.

### `@ProtoFactory` is required for `val` properties

Kotlin `val` properties have no setter. Without `@ProtoFactory`, the processor cannot instantiate and populate the object. Always use `@ProtoFactory` on the constructor of classes with `val`-only properties.

### kapt status

kapt is in maintenance mode. JetBrains recommends [KSP](https://kotlinlang.org/docs/ksp-overview.html) (Kotlin Symbol Processing) for new projects. ProtoStream currently requires kapt. KSP support may be added in a future release.
