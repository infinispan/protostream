package org.infinispan.protostream.processor.tests.kotlin.testdomain

import org.infinispan.protostream.annotations.ProtoEnumValue
import org.infinispan.protostream.annotations.ProtoFactory
import org.infinispan.protostream.annotations.ProtoField

/**
 * Scenario 1: Mutable class with var properties.
 * Kotlin generates standard get/set JavaBeans methods for var properties.
 * Must use @get:ProtoField to place the annotation on the getter (not the private backing field).
 */
class MutableMessage {
    @get:ProtoField(number = 1)
    var name: String? = null

    @get:ProtoField(number = 2, defaultValue = "0")
    var value: Int = 0

    @get:ProtoField(number = 3)
    var description: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MutableMessage) return false
        return name == other.name && value == other.value && description == other.description
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + value
        result = 31 * result + (description?.hashCode() ?: 0)
        return result
    }
}

/**
 * Scenario 2: Immutable data class using @ProtoFactory on the constructor.
 * All properties are val (no setters generated).
 * Must use @get:ProtoField to avoid annotation landing on the private final backing field.
 */
data class ImmutableMessage @ProtoFactory constructor(
    @get:ProtoField(number = 1) val title: String,
    @get:ProtoField(number = 2, defaultValue = "0") val count: Int,
    @get:ProtoField(number = 3) val tag: String?
)

/**
 * Scenario 3: Boolean properties including Kotlin "is" prefix.
 * Kotlin generates isActive() getter and setActive() setter for `var isActive: Boolean`,
 * which differs from the JavaBeans convention of setIsActive().
 */
class BooleanFieldMessage {
    @get:ProtoField(number = 1, defaultValue = "false")
    var enabled: Boolean = false

    @get:ProtoField(number = 2, defaultValue = "false")
    var isActive: Boolean = false

    @get:ProtoField(number = 3, defaultValue = "false")
    var visible: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BooleanFieldMessage) return false
        return enabled == other.enabled && isActive == other.isActive && visible == other.visible
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + visible.hashCode()
        return result
    }
}

/**
 * Scenario 4: Kotlin enum.
 */
enum class KotlinEnum {
    @ProtoEnumValue(number = 0) FIRST,
    @ProtoEnumValue(number = 1) SECOND,
    @ProtoEnumValue(number = 2) THIRD
}

/**
 * Scenario 5: Class with an enum field.
 */
data class MessageWithEnum @ProtoFactory constructor(
    @get:ProtoField(number = 1) val label: String,
    @get:ProtoField(number = 2) val status: KotlinEnum?
)

/**
 * Scenario 6: Class with collection fields.
 */
class CollectionMessage {
    @get:ProtoField(number = 1)
    var items: List<String>? = null

    @get:ProtoField(number = 2)
    var tags: List<String>? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CollectionMessage) return false
        return items == other.items && tags == other.tags
    }

    override fun hashCode(): Int {
        var result = items?.hashCode() ?: 0
        result = 31 * result + (tags?.hashCode() ?: 0)
        return result
    }
}

/**
 * Scenario 7: Data class with default parameter values.
 * Kotlin generates synthetic constructors with DefaultConstructorMarker.
 */
data class MessageWithDefaults @ProtoFactory constructor(
    @get:ProtoField(number = 1) val name: String,
    @get:ProtoField(number = 2, defaultValue = "42") val quantity: Int = 42,
    @get:ProtoField(number = 3) val note: String? = null
)

/**
 * Scenario 8: Class with @ProtoField on explicit getter methods and @ProtoFactory on constructor.
 */
class GetterAnnotatedMessage @ProtoFactory constructor(
    private val name: String,
    private val score: Int
) {
    @ProtoField(number = 1)
    fun getName(): String = name

    @ProtoField(number = 2, defaultValue = "0")
    fun getScore(): Int = score

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetterAnnotatedMessage) return false
        return name == other.name && score == other.score
    }

    override fun hashCode(): Int = 31 * name.hashCode() + score
}
