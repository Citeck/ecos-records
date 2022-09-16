package ru.citeck.ecos.records3.record.atts

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.mixin.MixinContext

interface RecordAttsService {

    fun <T> doWithSchema(attributes: Map<String, *>, rawAtts: Boolean, action: (List<SchemaAtt>) -> T): T

    fun <T> doWithSchema(atts: List<SchemaAtt>, rawAtts: Boolean, action: (List<SchemaAtt>) -> T): T

    fun <T : Any> getAtts(value: Any?, attributes: Class<T>): T

    fun getAtts(value: Any?, attributes: Map<String, String>): RecordAtts

    fun getAtts(value: Any?, attributes: Map<String, String>, rawAtts: Boolean): RecordAtts

    fun getAtts(value: Any?, attributes: Collection<String>): RecordAtts

    fun getAtts(value: Any?, attributes: Collection<String>, rawAtts: Boolean): RecordAtts

    fun <T : Any> getAtts(values: List<*>, attributes: Class<T>): List<T>

    fun getAtts(values: List<*>, attributes: Collection<String>): List<RecordAtts>

    fun getAtts(values: List<*>, attributes: Collection<String>, rawAtts: Boolean): List<RecordAtts>

    fun getAtts(values: List<*>, attributes: Map<String, String>): List<RecordAtts>

    fun getAtts(values: List<*>, attributes: Map<String, String>, rawAtts: Boolean): List<RecordAtts>

    fun getAtts(values: List<*>, attributes: Map<String, String>, rawAtts: Boolean, mixins: MixinContext): List<RecordAtts>

    fun getAtts(values: List<*>, attributes: List<SchemaAtt>, rawAtts: Boolean, mixins: MixinContext): List<RecordAtts>

    fun getId(value: Any?, defaultRef: RecordRef): RecordRef

    fun getAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        mixins: MixinContext,
        recordRefs: List<RecordRef>
    ): List<RecordAtts>
}
