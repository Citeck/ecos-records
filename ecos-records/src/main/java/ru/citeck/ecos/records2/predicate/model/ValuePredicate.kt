package ru.citeck.ecos.records2.predicate.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import ru.citeck.ecos.commons.data.DataValue
import java.util.*

class ValuePredicate : AttributePredicate {

    enum class Type {

        EQ,
        GT,
        GE,
        LT,
        LE,
        LIKE,
        IN,
        CONTAINS;

        @JsonValue
        fun asString(): String {
            return name.lowercase()
        }

        companion object {
            @JvmStatic
            @JsonCreator
            fun fromString(type: String): Type {
                return valueOf(type.uppercase())
            }
        }
    }

    companion object {

        @JvmStatic
        fun getTypes(): List<String> {
            return Type.values().map { it.asString() }
        }

        @JvmStatic
        fun equal(attribute: String?, value: Any?): ValuePredicate {
            return ValuePredicate(attribute, Type.EQ, value)
        }

        @JvmStatic
        fun eq(attribute: String?, value: Any?): ValuePredicate {
            return equal(attribute, value)
        }

        @JvmStatic
        fun contains(attribute: String?, value: Any?): ValuePredicate {
            return ValuePredicate(attribute, Type.CONTAINS, value)
        }

        @JvmStatic
        fun like(attribute: String?, value: Any?): ValuePredicate {
            return ValuePredicate(attribute, Type.LIKE, value)
        }

        @JvmStatic
        fun gt(attribute: String?, value: Any?): ValuePredicate {
            return ValuePredicate(attribute, Type.GT, value)
        }

        @JvmStatic
        fun ge(attribute: String?, value: Any?): ValuePredicate {
            return ValuePredicate(attribute, Type.GE, value)
        }

        @JvmStatic
        fun lt(attribute: String?, value: Any?): ValuePredicate {
            return ValuePredicate(attribute, Type.LT, value)
        }

        @JvmStatic
        fun le(attribute: String?, value: Any?): ValuePredicate {
            return ValuePredicate(attribute, Type.LE, value)
        }
    }

    @JsonProperty("val")
    private var value: DataValue = DataValue.NULL

    @JsonProperty("t")
    private var type = Type.EQ

    constructor()

    constructor(attribute: String?, type: Type, value: Any?) : this(attribute, type, DataValue.create(value))

    constructor(attribute: String?, type: Type, value: DataValue?) {
        this.setAttribute(attribute)
        this.setValue(value)
        this.type = type
    }

    @JsonProperty("t")
    fun getType(): Type {
        return type
    }

    fun setType(type: Type) {
        this.type = type
    }

    fun getValue(): DataValue {
        return value
    }

    fun setValue(value: Any?) {
        this.value = if (value is DataValue) value else DataValue.create(value)
    }

    @JsonProperty("v")
    fun setVal(value: Any?) {
        setValue(value)
    }

    override fun <T : Predicate> copy(): T {
        val predicate = ValuePredicate()
        predicate.setAttribute(getAttribute())
        predicate.type = type
        predicate.setValue(getValue())
        @Suppress("UNCHECKED_CAST")
        return predicate as T
    }

    override fun toString(): String {
        return "{\"t\":\"${type.asString()}\",\"att\":\"${getAttribute()}\",\"val\":$value}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }
        other as ValuePredicate
        return getAttribute() == other.getAttribute() &&
            value == other.value &&
            type == other.type
    }

    override fun hashCode(): Int {
        return Objects.hash(getAttribute(), type, value)
    }
}
