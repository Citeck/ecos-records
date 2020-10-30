package ru.citeck.ecos.records3.record.op.atts.service

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.mixin.AttMixin
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt

interface RecordAttsService {

    fun <T: Any> getAtts(value: Any?, attributes: Class<T>): T

    fun getAtts(value: Any?, attributes: Map<String, String>): RecordAtts

    fun getAtts(value: Any?, attributes: Map<String, String>, rawAtts: Boolean): RecordAtts

    fun getAtts(value: Any?, attributes: Collection<String>): RecordAtts

    fun getAtts(value: Any?, attributes: Collection<String>, rawAtts: Boolean): RecordAtts

    fun <T: Any> getAtts(values: List<*>, attributes: Class<T>): List<T>

    fun getAtts(values: List<*>, attributes: Collection<String>): List<RecordAtts>

    fun getAtts(values: List<*>, attributes: Collection<String>, rawAtts: Boolean): List<RecordAtts>

    fun getAtts(values: List<*>, attributes: Map<String, String>): List<RecordAtts>

    fun getAtts(values: List<*>, attributes: Map<String, String>, rawAtts: Boolean): List<RecordAtts>

    fun getAtts(values: List<*>, attributes: Map<String, String>, rawAtts: Boolean, mixins: List<AttMixin>): List<RecordAtts>

    fun getAtts(values: List<*>, attributes: List<SchemaAtt>, rawAtts: Boolean, mixins: List<AttMixin>): List<RecordAtts>

    fun getAtts(values: List<*>,
                attributes: List<SchemaAtt>,
                rawAtts: Boolean,
                mixins: List<AttMixin>,
                recordRefs: List<RecordRef>): List<RecordAtts>
}
