package ru.citeck.ecos.records3.utils

import ru.citeck.ecos.records2.RecordRef

object RecordRefUtils {

    fun mapAppIdAndSourceId(
        recordRef: RecordRef,
        currentAppName: String,
        mapping: Map<String, String>?
    ): RecordRef {
        if (RecordRef.isEmpty(recordRef) || mapping.isNullOrEmpty()) {
            return recordRef
        }
        var targetId = ""
        if (recordRef.appName.isNotBlank()) {
            targetId = mapping[recordRef.appName + "/" + recordRef.sourceId] ?: ""
        }
        if (targetId.isBlank() && (recordRef.appName == currentAppName || recordRef.appName.isEmpty())) {
            targetId = mapping[recordRef.sourceId] ?: ""
        }
        if (targetId.isBlank()) {
            return recordRef
        }
        val appDelimIdx = targetId.indexOf('/')
        return if (appDelimIdx >= 0 && appDelimIdx < targetId.length - 1) {
            RecordRef.create(
                targetId.substring(0, appDelimIdx),
                targetId.substring(appDelimIdx + 1),
                recordRef.id
            )
        } else {
            RecordRef.create(
                recordRef.appName,
                targetId,
                recordRef.id
            )
        }
    }
}
