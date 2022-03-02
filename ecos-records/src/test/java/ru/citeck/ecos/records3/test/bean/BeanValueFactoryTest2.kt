package ru.citeck.ecos.records3.test.bean

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.AttEdge
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import kotlin.test.assertEquals

class BeanValueFactoryTest2 {

    @Test
    fun test() {

        val services = RecordsServiceFactory()

        services.recordsServiceV1.register(object : RecordsQueryDao, RecordAttsDao {
            override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {
                return RecsQueryRes.of(TestClass(RecordRef.valueOf("test@test")))
            }
            override fun getRecordAtts(recordId: String): Any? {
                return TestClass(RecordRef.valueOf("test@test"))
            }
            override fun getId() = "test"
        })

        assertEquals("test@test", services.recordsServiceV1.getAtt(RecordRef.valueOf("test@test"), "?id").asText())
        val queryRecord = services.recordsServiceV1.query(
            RecordsQuery.create {
                sourceId = "test"
            },
            mapOf(Pair("id", "?id"))
        ).getRecords()[0]

        assertEquals("test@test", queryRecord.getAtt("id").asText())
        assertEquals("test@test", queryRecord.getId().toString())
    }

    @Test
    fun mapHasTest() {

        val map = mapOf(
            "field" to "value"
        )
        val records = RecordsServiceFactory().recordsServiceV1

        assertThat(records.getAtt(map, "_has.field?bool").asBoolean()).isTrue
        assertThat(records.getAtt(map, "_has.field123?bool").asBoolean()).isFalse
    }

    @Test
    fun extensionTest() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        val dto = ExtensionTest(ExtensionInnerTest("value0", 999))

        assertEquals(dto.inner.field0, records.getAtt(dto, "field0").asText())
        assertEquals(dto.inner.field1, records.getAtt(dto, "field1").asInt())
        assertEquals(dto.getCustom(), records.getAtt(dto, "custom").asText())

        assertEquals("abc", records.getAtt(DispNameDispNameDto(), "?disp").asText())
        assertEquals("abc", records.getAtt(DispNameLabelDto(), "?disp").asText())
        assertEquals("abc", records.getAtt(DispNameNameDto(), "?disp").asText())
        assertEquals("abc", records.getAtt(DispNameDispNameWithLabelDto(), "?disp").asText())
        assertEquals("abc", records.getAtt(DispNameDispNameWithLabelWithScalarMirrorDto(), "?disp").asText())
        assertEquals("abc", records.getAtt(DispNameDispNameWithLabelWithScalarDto(), "?disp").asText())
        assertEquals("abc", records.getAtt(DispNameDispNameWithLabelWithScalarDto(), "_disp").asText())
    }

    @Test
    fun testGetAs() {

        val records = RecordsServiceFactory().recordsServiceV1

        val bean = BeanWithStrFunctions()
        val getAtt = { arg: String -> records.getAtt(bean, arg) }

        assertThat(getAtt("_as.test").asText()).isEqualTo("test-postfix")
        assertThat(getAtt("_has.value?bool").asBoolean()).isEqualTo(true)
        assertThat(getAtt("_has.value2?bool").asBoolean()).isEqualTo(false)
        assertThat(getAtt("_edge.unknown.title").asText()).isEqualTo("")
        assertThat(getAtt("_edge.bean.title").asText()).isEqualTo("edge-title")

        assertThat(records.getAtt(BeanWithCustomGetEdge(), "_edge.bean.title").asText()).isEqualTo("edge-title")

        println(java.lang.Boolean::class == Boolean::class)
    }

    class BeanWithStrFunctions {
        fun getAs(arg: String): Any {
            return "$arg-postfix"
        }
        fun getEdge(arg: String): AttEdge? {
            if (arg != "bean") {
                return null
            }
            return object : AttEdge {
                override fun getTitle(): MLText {
                    return MLText("edge-title")
                }
            }
        }
        fun has(arg: String): Boolean {
            return arg == "value"
        }
    }

    class BeanWithCustomGetEdge {
        fun getEdge(arg: String): CustomEdge? {
            if (arg != "bean") {
                return null
            }
            return CustomEdge()
        }
        class CustomEdge : AttEdge {
            override fun getTitle(): MLText {
                return MLText("edge-title")
            }
        }
    }

    class DispNameDispNameWithLabelWithScalarMirrorDto {
        fun getDisplayName(): MLText {
            return MLText("abcdef")
        }
        fun getLabel(): MLText {
            return MLText("abcdef")
        }
        @AttName("_disp")
        fun getCustomDispName(): MLText {
            return MLText("abc")
        }
    }

    class DispNameDispNameWithLabelWithScalarDto {
        fun getDisplayName(): MLText {
            return MLText("abcdef")
        }
        fun getLabel(): MLText {
            return MLText("abcdef")
        }
        @AttName("?disp")
        fun getCustomDispName(): MLText {
            return MLText("abc")
        }
    }

    class DispNameDispNameWithLabelDto {
        fun getDisplayName(): MLText {
            return MLText("abc")
        }
        fun getLabel(): MLText {
            return MLText("abcdef")
        }
    }

    class DispNameDispNameDto {
        fun getDisplayName(): MLText {
            return MLText("abc")
        }
    }

    class DispNameLabelDto {
        fun getLabel(): MLText {
            return MLText("abc")
        }
    }

    class DispNameNameDto {
        fun getName(): MLText {
            return MLText("abc")
        }
    }

    class TestClass(val id: RecordRef) {

        fun getAtt(): String {
            return id.id
        }
    }

    class ExtensionTest(
        @AttName("...")
        val inner: ExtensionInnerTest
    ) {
        fun getCustom(): String {
            return "custom"
        }
    }

    data class ExtensionInnerTest(val field0: String, val field1: Int)
}
