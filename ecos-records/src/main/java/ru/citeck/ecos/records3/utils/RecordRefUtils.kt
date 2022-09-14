package ru.citeck.ecos.records3.utils

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef

object RecordRefUtils {

    fun mapAppIdAndSourceId(
        recordRef: RecordRef,
        currentAppName: String,
        mapping: Map<String, String>?
    ): RecordRef {
        return RecordRef.valueOf(mapAppIdAndSourceId(recordRef as EntityRef, currentAppName, mapping))
    }

    fun mapAppIdAndSourceId(
        recordRef: EntityRef,
        currentAppName: String,
        mapping: Map<String, String>?
    ): EntityRef {
        if (EntityRef.isEmpty(recordRef) || mapping.isNullOrEmpty()) {
            return recordRef
        }
        var targetId = ""
        if (recordRef.getAppName().isNotBlank()) {
            targetId = mapping[recordRef.getAppName() + "/" + recordRef.getSourceId()] ?: ""
        }
        if (targetId.isBlank() && (recordRef.getAppName() == currentAppName || recordRef.getAppName().isEmpty())) {
            targetId = mapping[recordRef.getSourceId()] ?: ""
        }
        if (targetId.isBlank()) {
            return recordRef
        }
        val appDelimIdx = targetId.indexOf('/')
        return if (appDelimIdx >= 0 && appDelimIdx < targetId.length - 1) {
            EntityRef.create(
                targetId.substring(0, appDelimIdx),
                targetId.substring(appDelimIdx + 1),
                recordRef.getLocalId()
            )
        } else {
            EntityRef.create(
                recordRef.getAppName(),
                targetId,
                recordRef.getLocalId()
            )
        }
    }
}
