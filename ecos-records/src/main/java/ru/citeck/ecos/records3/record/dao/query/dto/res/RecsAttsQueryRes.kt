package ru.citeck.ecos.records3.record.dao.query.dto.res

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts

/**
 * Used to deserialize query result with RecordAtts.
 */
class RecsAttsQueryRes : RecsQueryRes<RecordAtts> {

    constructor()

    constructor(other: RecsAttsQueryRes) : super(other)

    fun addSourceId(sourceId: String): RecsAttsQueryRes {
        for (record in getRecords()) {
            record.setId(RecordRef.create(sourceId, record.getId().toString()))
        }
        return this
    }

    fun addAppName(appName: String): RecsAttsQueryRes {
        setRecords(getRecords().map { it.withId(it.getId().addAppName(appName)) })
        return this
    }
}
