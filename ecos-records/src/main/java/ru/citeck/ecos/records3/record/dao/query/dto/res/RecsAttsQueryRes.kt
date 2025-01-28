package ru.citeck.ecos.records3.record.dao.query.dto.res

import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * Used to deserialize query result with RecordAtts.
 */
class RecsAttsQueryRes : RecsQueryRes<RecordAtts> {

    constructor()

    constructor(other: RecsAttsQueryRes) : super(other)

    fun addSourceId(sourceId: String): RecsAttsQueryRes {
        for (record in getRecords()) {
            record.setId(EntityRef.create(sourceId, record.getId().toString()))
        }
        return this
    }

    fun addAppName(appName: String): RecsAttsQueryRes {
        setRecords(getRecords().map { it.withId(it.getId().withAppName(appName)) })
        return this
    }
}
