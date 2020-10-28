package ru.citeck.ecos.records3.record.op.query.dto

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts

/**
 * Used to deserialize query result with RecordAtts.
 */
class RecordsAttsQueryRes : RecsQueryRes<RecordAtts> {

    constructor()

    constructor(other: RecordsAttsQueryRes) : super(other)

    fun addSourceId(sourceId: String): RecordsAttsQueryRes {
        for (record in getRecords()) {
            record.setId(RecordRef.create(sourceId, record.getId().toString()))
        }
        return this
    }

    fun addAppName(appName: String): RecordsAttsQueryRes {
        setRecords(getRecords().map { it.withId(it.getId().addAppName(appName)) })
        return this
    }
}
