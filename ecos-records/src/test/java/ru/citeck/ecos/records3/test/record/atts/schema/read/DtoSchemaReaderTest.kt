package ru.citeck.ecos.records3.test.record.atts.schema.read

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
        assertThat(schemaByAlias["annotatedArray2"].toString()).isEqualTo("array[]?raw")
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

    class AttsDto(
        val array: List<DataValue>,
        @AttName("array")
        val annotatedArray: List<DataValue>,
        @AttName("array[]")
        val annotatedArray2: List<DataValue>,
        @AttName("array[]{id,_null}")
        val annotatedArray3: List<DataValue>,
        @AttName("array")
        val single: DataValue,
        @AttName("record.nestedProjects[]{projectRef:?id!'',workspaceRef:assoc_src_workspaceManagedBy?id!}!")
        val complexAtt: List<NestedAtts>?,
        @AttName("record.nestedProjects[]")
        val complexAtt2: List<NestedAtts>?
    )

    class NestedAtts(
        val projectRef: EntityRef,
        @AttName("assoc_src_workspaceManagedBy?id!")
        val workspaceRef: EntityRef,
    )

    class RefsDto(
        val legacyRecordRef: EntityRef,
        val entityRef: EntityRef
    )
}
