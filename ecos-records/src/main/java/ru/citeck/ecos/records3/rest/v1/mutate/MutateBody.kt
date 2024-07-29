package ru.citeck.ecos.records3.rest.v1.mutate

import com.fasterxml.jackson.annotation.JsonSetter
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.rest.v1.RequestBody
import ru.citeck.ecos.records3.rest.v1.RestUtils
import ru.citeck.ecos.records3.security.HasSensitiveData
import java.util.*

class MutateBody : RequestBody, HasSensitiveData<MutateBody> {

    private var records: MutableList<RecordAtts> = ArrayList()
    var attributes: List<Map<String, Any?>> = emptyList()
    var rawAtts = false

    constructor() : super()

    constructor(other: MutateBody) : super(other) {
        this.records = ArrayList(other.records.map { RecordAtts(it) })
    }

    fun setRecord(record: RecordAtts) {
        records.add(record)
    }

    fun getRecords(): List<RecordAtts> {
        return records
    }

    fun setRecords(records: List<RecordAtts>?) {
        if (records != null) {
            this.records = ArrayList(records)
        } else {
            this.records = ArrayList()
        }
    }

    fun addRecord(meta: RecordAtts) {
        records.add(meta)
    }

    @JsonSetter
    fun setAttributes(attributes: Any?) {
        this.attributes = RestUtils.prepareReqAttsAsListOfMap(attributes)
    }

    override fun withoutSensitiveData(): MutateBody {
        val newBody = MutateBody(this)
        newBody.records = newBody.records.map { it.withoutSensitiveData() }.toMutableList()
        return newBody
    }
}
