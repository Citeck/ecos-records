package ru.citeck.ecos.records3.record.atts.schema.resolver

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.utils.LibsUtils
import ru.citeck.ecos.records3.record.atts.value.HasListView
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.lang.reflect.Array

object AttValueUtils {

    @JvmStatic
    fun rawToListWithoutNullValues(rawValue: Any?): List<Any> {

        if (rawValue == null || isNull(rawValue)) {
            return emptyList()
        }

        if (rawValue.javaClass.isArray) {
            return if (rawValue.javaClass == ByteArray::class.java) {
                listOf(rawValue)
            } else {
                val length = Array.getLength(rawValue)
                if (length == 0) {
                    emptyList()
                } else {
                    val result = ArrayList<Any>(length)
                    for (i in 0 until length) {
                        val element = Array.get(rawValue, i)
                        if (!isNull(element)) {
                            result.add(element)
                        }
                    }
                    result
                }
            }
        }

        if (rawValue is HasListView<*>) {
            return iterableToListWithoutNullValues(rawValue.getListView())
        } else if (rawValue is DataValue) {
            return if (rawValue.isArray()) {
                if (rawValue.size() == 0) {
                    emptyList()
                } else {
                    iterableToListWithoutNullValues(rawValue)
                }
            } else if (rawValue.isNull()) {
                emptyList()
            } else {
                listOf(rawValue)
            }
        } else if (rawValue is ArrayNode) {
            return iterableToListWithoutNullValues(rawValue)
        } else if (LibsUtils.isJacksonPresent() && rawValue is com.fasterxml.jackson.databind.node.ArrayNode) {
            return iterableToListWithoutNullValues(rawValue)
        } else if (rawValue is Collection<*>) {
            return iterableToListWithoutNullValues(rawValue)
        }
        return listOf(rawValue)
    }

    @JvmStatic
    fun <T> iterableToListWithoutNullValues(value: Iterable<T>): List<Any> {

        if (value is Collection<*>) {
            when (value.size) {
                0 -> return emptyList()
                1 -> {
                    val element = if (value is List<*>) {
                        value[0]
                    } else {
                        val iter = value.iterator()
                        if (iter.hasNext()) {
                            iter.next()
                        } else {
                            null
                        }
                    }
                    return if (element == null || isNull(element)) {
                        emptyList()
                    } else {
                        listOf(element)
                    }
                }
                else -> {}
            }
        }
        val result = ArrayList<Any>()
        for (it in value) {
            if (it != null && !isNull(it)) {
                result.add(it)
            }
        }
        return if (result.isEmpty()) {
            emptyList()
        } else {
            result
        }
    }

    @JvmStatic
    fun isEmpty(rawValue: Any?): Boolean {
        rawValue ?: return true
        if (isNull(rawValue)) {
            return true
        }
        if (rawValue is String) {
            return rawValue.isEmpty()
        }
        if (rawValue is DataValue) {
            return rawValue.isTextual() && rawValue.asText().isEmpty() ||
                rawValue.isArray() && rawValue.size() == 0
        }
        if (rawValue is JsonNode) {
            return rawValue.isTextual && rawValue.asText().isEmpty() ||
                rawValue.isArray && rawValue.size() == 0
        }
        if (LibsUtils.isJacksonPresent()) {
            if (rawValue is com.fasterxml.jackson.databind.JsonNode) {
                return rawValue.isTextual && rawValue.asText().isEmpty() ||
                    rawValue.isArray && rawValue.size() == 0
            }
        }
        if (rawValue is Collection<*>) {
            return rawValue.isEmpty()
        }
        if (rawValue::class.java.isArray) {
            return Array.getLength(rawValue) == 0
        }
        return false
    }

    @JvmStatic
    fun isNull(rawValue: Any?): Boolean {

        if (rawValue == null ||
            rawValue is EntityRef && EntityRef.isEmpty(rawValue) ||
            rawValue is DataValue && rawValue.isNull()
        ) {

            return true
        }
        if (rawValue is JsonNode) {
            return rawValue.isNull || rawValue.isMissingNode
        }
        if (LibsUtils.isJacksonPresent()) {
            if (rawValue is com.fasterxml.jackson.databind.JsonNode) {
                return rawValue.isNull || rawValue.isMissingNode
            }
        }
        return false
    }
}
