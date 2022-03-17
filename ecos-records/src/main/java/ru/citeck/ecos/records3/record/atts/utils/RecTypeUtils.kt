package ru.citeck.ecos.records3.record.atts.utils

import ru.citeck.ecos.records2.RecordRef

object RecTypeUtils {

    private const val ECOS_TYPE_APP = "emodel"
    private const val ECOS_TYPE_SOURCE_ID = "type"
    private const val ECOS_TYPE_APP_SOURCE_ID_PREFIX = "$ECOS_TYPE_APP/$ECOS_TYPE_SOURCE_ID@"

    fun anyTypeToRef(type: Any?): RecordRef {
        type ?: return RecordRef.EMPTY
        return when (type) {
            is RecordRef -> type
            is String -> when {
                type.isBlank() -> RecordRef.EMPTY
                type.startsWith(ECOS_TYPE_APP_SOURCE_ID_PREFIX) -> RecordRef.valueOf(type)
                !type.contains('@') -> RecordRef.create(ECOS_TYPE_APP, ECOS_TYPE_SOURCE_ID, type)
                else -> {
                    val ref = RecordRef.valueOf(type)
                    if (ref.appName.isBlank()) {
                        ref.withAppName(ECOS_TYPE_APP)
                    } else {
                        ref
                    }
                }
            }
            else -> RecordRef.EMPTY
        }
    }
}
