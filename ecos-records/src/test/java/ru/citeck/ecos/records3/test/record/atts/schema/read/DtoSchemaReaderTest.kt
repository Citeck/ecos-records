package ru.citeck.ecos.records3.test.record.atts.schema.read

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Suppress("unused")
class DtoSchemaReaderTest {

    @Test
    fun readResultTest() {

        val services = RecordsServiceFactory()
        val schemaReader = services.dtoSchemaReader

        val schema = schemaReader.read(AttsDto::class.java)
        val schemaByAlias = schema.associateBy { it.getAliasForValue() }

        assertThat(schemaByAlias["array"].toString()).isEqualTo("array[]?raw")
        assertThat(schemaByAlias["annotatedArray"].toString()).isEqualTo("array[]?raw")
        assertThat(schemaByAlias["annotatedArray2"].toString()).isEqualTo("array[]?raw|or([])")
        assertThat(schemaByAlias["annotatedArray3"].toString()).isEqualTo("array[]{id,_null}")
        assertThat(schemaByAlias["single"].toString()).isEqualTo("array?raw")
        assertThat(schemaByAlias["complexAtt"]).isEqualTo(
            SchemaAtt.create()
                .withAlias("complexAtt")
                .withName("record")
                .withInner(
                    SchemaAtt.create()
                        .withName("nestedProjects")
                        .withMultiple(true)
                        .withInner(
                            listOf(
                                SchemaAtt.create()
                                    .withAlias("projectRef")
                                    .withName("?id")
                                    .withProcessors(listOf(AttProcDef("or", listOf(DataValue.createStr("")))))
                                    .build(),
                                SchemaAtt.create()
                                    .withAlias("workspaceRef")
                                    .withName("assoc_src_workspaceManagedBy")
                                    .withInner(SchemaAtt.create().withName("?id"))
                                    .withProcessors(listOf(AttProcDef("or", listOf(DataValue.createStr("")))))
                                    .build(),
                            )
                        )
                )
                .withProcessors(listOf(AttProcDef("or", listOf(DataValue.createArr()))))
                .build()
        )
        assertThat(schemaByAlias["complexAtt2"]).isEqualTo(
            SchemaAtt.create()
                .withAlias("complexAtt2")
                .withName("record")
                .withInner(
                    SchemaAtt.create()
                        .withName("nestedProjects")
                        .withMultiple(true)
                        .withInner(
                            listOf(
                                SchemaAtt.create()
                                    .withName("projectRef")
                                    .withInner(SchemaAtt.create().withName("?id"))
                                    .withProcessors(listOf(AttProcDef("or", listOf(DataValue.createStr("")))))
                                    .build(),
                                SchemaAtt.create()
                                    .withName("nullableProjectRef")
                                    .withInner(SchemaAtt.create().withName("?id"))
                                    .build(),
                                SchemaAtt.create()
                                    .withAlias("workspaceRef")
                                    .withName("assoc_src_workspaceManagedBy")
                                    .withInner(SchemaAtt.create().withName("?id"))
                                    .withProcessors(listOf(AttProcDef("or", listOf(DataValue.createStr("")))))
                                    .build(),
                            )
                        )
                )
                .build()
        )

        val schema2 = schemaReader.read(RefsDto::class.java).associateBy { it.getAliasForValue() }
        assertThat(schema2["legacyRecordRef"].toString()).isEqualTo("legacyRecordRef?id")
        assertThat(schema2["entityRef"].toString()).isEqualTo("entityRef?id")
    }

    @Test
    fun arrayTest() {

        val records = RecordsServiceFactory().recordsService
        records.register(InMemDataRecordsDao("test"))

        val elements = listOf("abc", "def", 123, 456.1)

        val ref = records.create(
            "test",
            mapOf(
                "array" to elements
            )
        )

        val atts = records.getAtts(ref, AttsDto::class.java)

        assertThat(atts.array).containsExactlyElementsOf(
            elements.map { DataValue.create(it) }
        )
        assertThat(atts.annotatedArray).containsExactlyElementsOf(
            elements.map { DataValue.create(it) }
        )
        assertThat(atts.annotatedArray2).containsExactlyElementsOf(
            elements.map { DataValue.create(it) }
        )
        assertThat(atts.single).isEqualTo(DataValue.create(elements[0]))
    }

    @Test
    fun nullabilityTest() {

        val services = RecordsServiceFactory()
        val schemaReader = services.dtoSchemaReader

        val schema = schemaReader.read(NullabilityTest::class.java)

        val attsByName = schema.associateBy { it.getAliasForValue() }

        val nullableWithAttName = attsByName["nullableWithAttName"]
        assertThat(nullableWithAttName).isEqualTo(
            SchemaAtt.create()
                .withName("customAtt0")
                .withAlias("nullableWithAttName")
                .withInner(SchemaAtt.create().withName("?disp"))
                .build()
        )

        val notNullableWithAttName = attsByName["notNullableWithAttName"]
        assertThat(notNullableWithAttName).isEqualTo(
            SchemaAtt.create()
                .withAlias("notNullableWithAttName")
                .withName("customAtt1")
                .withInner(SchemaAtt.create().withName("?disp"))
                .withProcessors(listOf(AttProcDef("or", listOf(DataValue.createStr("")))))
                .build()
        )

        val nullableArrWithAttName = attsByName["nullableArrWithAttName"]
        assertThat(nullableArrWithAttName).isEqualTo(
            SchemaAtt.create()
                .withAlias("nullableArrWithAttName")
                .withName("customAtt0")
                .withMultiple(true)
                .withInner(SchemaAtt.create().withName("?str"))
                .build()
        )

        val notNullableArrWithAttName = attsByName["notNullableArrWithAttName"]
        assertThat(notNullableArrWithAttName).isEqualTo(
            SchemaAtt.create()
                .withAlias("notNullableArrWithAttName")
                .withName("customAtt1")
                .withMultiple(true)
                .withInner(SchemaAtt.create().withName("?str"))
                .withProcessors(listOf(AttProcDef("or", listOf(DataValue.createArr()))))
                .build()
        )

        val nullableWithoutAttName = attsByName["nullableWithoutAttName"]
        assertThat(nullableWithoutAttName).isEqualTo(
            SchemaAtt.create()
                .withName("nullableWithoutAttName")
                .withInner(SchemaAtt.create().withName("?disp"))
                .build()
        )

        val notNullableWithoutAttName = attsByName["notNullableWithoutAttName"]
        assertThat(notNullableWithoutAttName).isEqualTo(
            SchemaAtt.create()
                .withName("notNullableWithoutAttName")
                .withInner(SchemaAtt.create().withName("?disp"))
                .withProcessors(listOf(AttProcDef("or", listOf(DataValue.createStr("")))))
                .build()
        )

        val attWithOrProc = attsByName["attWithOrProc"]
        assertThat(attWithOrProc).isEqualTo(
            SchemaAtt.create()
                .withAlias("attWithOrProc")
                .withName("customAtt3")
                .withInner(SchemaAtt.create().withName("?disp"))
                .withProcessors(listOf(AttProcDef("or", listOf(DataValue.createStr("")))))
                .build()
        )

        val dataValue = attsByName["dataValue"]
        assertThat(dataValue).isEqualTo(
            SchemaAtt.create()
                .withName("dataValue")
                .withInner(SchemaAtt.create().withName("?raw"))
                .build()
        )

        val enumWithDefault = attsByName["enumWithDefault"]
        assertThat(enumWithDefault).isEqualTo(
            SchemaAtt.create()
                .withName("enumWithDefault")
                .withInner(SchemaAtt.create().withName("?str"))
                .withProcessors(listOf(AttProcDef("or", listOf(DataValue.createStr("")))))
                .build()
        )

        val enumWithoutDefault = attsByName["enumWithoutDefault"]
        assertThat(enumWithoutDefault).isEqualTo(
            SchemaAtt.create()
                .withName("enumWithoutDefault")
                .withInner(SchemaAtt.create().withName("?str"))
                .build()
        )
    }

    class NullabilityTest(
        @AttName("customAtt0")
        val nullableWithAttName: String?,
        @AttName("customAtt1")
        val notNullableWithAttName: String,
        @AttName("customAtt0[]?str")
        val nullableArrWithAttName: String?,
        @AttName("customAtt1[]?str")
        val notNullableArrWithAttName: String,
        val nullableWithoutAttName: String?,
        val notNullableWithoutAttName: String,
        @AttName("customAtt3!")
        val attWithOrProc: String,
        val dataValue: DataValue,
        val enumWithDefault: EnumWithDefault,
        val enumWithoutDefault: EnumWithoutDefault
    )

    enum class EnumWithDefault {
        @JsonEnumDefaultValue DEFAULT,
        SECOND
    }

    enum class EnumWithoutDefault {
        FIRST,
        SECOND
    }

    @Test
    fun nullabilityWithBuilderTest() {

        val services = RecordsServiceFactory()
        val schemaReader = services.dtoSchemaReader

        val attsByName = schemaReader.read(NullabilityWithBuilderTest::class.java)
            .associateBy { it.getAliasForValue() }

        val enumWithDefault = attsByName["enumWithDefault"]
        assertThat(enumWithDefault).isEqualTo(
            SchemaAtt.create()
                .withName("enumWithDefault")
                .withInner(SchemaAtt.create().withName("?str"))
                .build()
        )

        val enumWithoutDefault = attsByName["enumWithoutDefault"]
        assertThat(enumWithoutDefault).isEqualTo(
            SchemaAtt.create()
                .withName("enumWithoutDefault")
                .withInner(SchemaAtt.create().withName("?str"))
                .build()
        )

        val text = attsByName["text"]
        assertThat(text).isEqualTo(
            SchemaAtt.create()
                .withName("text")
                .withInner(SchemaAtt.create().withName("?disp"))
                .build()
        )

        val text2 = attsByName["text2"]
        assertThat(text2).isEqualTo(
            SchemaAtt.create()
                .withName("text2")
                .withInner(SchemaAtt.create().withName("?disp"))
                .build()
        )
    }

    @JsonDeserialize(builder = NullabilityWithBuilderTest.Builder::class)
    class NullabilityWithBuilderTest(
        val enumWithDefault: EnumWithDefault,
        val enumWithoutDefault: EnumWithoutDefault,
        val text: String,
        val text2: String
    ) {

        class Builder {

            var text: String = ""
            var text2: String = ""
            var enumWithDefault: EnumWithDefault = EnumWithDefault.DEFAULT
            var enumWithoutDefault: EnumWithoutDefault = EnumWithoutDefault.FIRST

            fun withText(text: String?): Builder {
                this.text = text ?: ""
                return this
            }

            fun withText2(text: String?) {
                this.text2 = text ?: ""
            }

            fun withEnumWithDefault(enumWithDefault: EnumWithDefault?): Builder {
                this.enumWithDefault = enumWithDefault ?: EnumWithDefault.DEFAULT
                return this
            }

            fun withEnumWithoutDefault(enumWithoutDefault: EnumWithoutDefault?): Builder {
                this.enumWithoutDefault = enumWithoutDefault ?: EnumWithoutDefault.FIRST
                return this
            }

            fun with(a: String) {}
            fun withA(b: String) {}

            fun build(): NullabilityWithBuilderTest {
                return NullabilityWithBuilderTest(enumWithDefault, enumWithoutDefault, text, text2)
            }
        }
    }

    @Test
    fun notNullButButWithNullSupportTest() {

        val services = RecordsServiceFactory()
        val schemaReader = services.dtoSchemaReader

        val schema = schemaReader.read(NotNullButButWithNullSupportTest::class.java)

        for (att in schema) {
            assertThat(att.processors).isEmpty()
        }

        val srcInst = NotNullButButWithNullSupportTest(DataValue.NULL)
        val tgtInst = services.recordsService.getAtts(srcInst, NotNullButButWithNullSupportTest::class.java)

        assertThat(tgtInst).isEqualTo(srcInst)
    }

    data class NotNullButButWithNullSupportTest(
        val dataValue: DataValue
    )

    class AttsDto(
        val array: List<DataValue>?,
        @AttName("array")
        val annotatedArray: List<DataValue>?,
        @AttName("array[]")
        val annotatedArray2: List<DataValue>,
        @AttName("array[]{id,_null}")
        val annotatedArray3: List<DataValue>?,
        @AttName("array")
        val single: DataValue,
        @AttName("record.nestedProjects[]{projectRef:?id!'',workspaceRef:assoc_src_workspaceManagedBy?id!}!")
        val complexAtt: List<NestedAtts>?,
        @AttName("record.nestedProjects[]")
        val complexAtt2: List<NestedAtts>?
    )

    class NestedAtts(
        val projectRef: EntityRef,
        val nullableProjectRef: EntityRef?,
        @AttName("assoc_src_workspaceManagedBy?id!")
        val workspaceRef: EntityRef,
    )

    class RefsDto(
        val legacyRecordRef: EntityRef?,
        val entityRef: EntityRef?
    )
}
