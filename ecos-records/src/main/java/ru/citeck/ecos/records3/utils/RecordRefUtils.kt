package ru.citeck.ecos.records3.utils

import ru.citeck.ecos.webapp.api.entity.EntityRef

object RecordRefUtils {

    fun mapAppIdAndSourceId(
        originalRef: EntityRef,
        currentAppName: String,
        mapping: Map<String, String>?
    ): EntityRef {
        if (EntityRef.isEmpty(originalRef) || mapping.isNullOrEmpty()) {
            return originalRef
        }
        val refToMap = originalRef.withDefaultAppName(currentAppName)

        var targetId = ""
        if (refToMap.getAppName().isNotBlank()) {
            targetId = mapping[refToMap.getAppName() + "/" + refToMap.getSourceId()] ?: ""
        }
        val appName = refToMap.getAppName()
        if (targetId.isBlank() && (appName == currentAppName || appName.isEmpty())) {
            targetId = mapping[refToMap.getSourceId()] ?: ""
        }
        if (targetId.isBlank()) {
            return originalRef
        }
        val appDelimIdx = targetId.indexOf('/')
        return if (appDelimIdx >= 0 && appDelimIdx < targetId.length - 1) {
            val targetAppName = targetId.substring(0, appDelimIdx)
            if (targetAppName == currentAppName && originalRef.getAppName().isEmpty()) {
                EntityRef.create(
                    targetId.substring(appDelimIdx + 1),
                    refToMap.getLocalId()
                )
            } else {
                EntityRef.create(
                    targetAppName,
                    targetId.substring(appDelimIdx + 1),
                    refToMap.getLocalId()
                )
            }
        } else {
            EntityRef.create(
                originalRef.getAppName(),
                targetId,
                refToMap.getLocalId()
            )
        }
    }
}
