package ru.citeck.ecos.records3.test.record.atts.schema

import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory
import ecos.com.fasterxml.jackson210.databind.node.MissingNode
import ecos.com.fasterxml.jackson210.databind.node.NullNode
import ecos.com.fasterxml.jackson210.databind.node.TextNode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttValueUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef

class AttValueUtilsTest {

    @Test
    fun test() {

        val nullValues = listOf(
            null,
            DataValue.NULL,
            NullNode.instance,
            MissingNode.getInstance(),
            com.fasterxml.jackson.databind.node.NullNode.instance,
            com.fasterxml.jackson.databind.node.MissingNode.getInstance(),
            EntityRef.EMPTY
        )

        nullValues.forEach {
            assertTrue(AttValueUtils.isNull(it)) {
                if (it == null) {
                    "null"
                } else {
                    it::class.simpleName ?: "null"
                }
            }
        }

        val emptyValues = listOf(
            *nullValues.toTypedArray(),
            "",
            DataValue.createStr(""),
            TextNode.valueOf(""),
            com.fasterxml.jackson.databind.node.TextNode.valueOf(""),
            emptyArray<Any>(),
            emptyList<Any>(),
            DataValue.createArr(),
            JsonNodeFactory.instance.arrayNode(),
            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode()
        )

        emptyValues.forEach {
            assertTrue(AttValueUtils.isEmpty(it)) {
                if (it == null) {
                    "null"
                } else {
                    it::class.simpleName ?: "null"
                }
            }
        }
    }
}
