package ru.citeck.ecos.records3.test.bean

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
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
