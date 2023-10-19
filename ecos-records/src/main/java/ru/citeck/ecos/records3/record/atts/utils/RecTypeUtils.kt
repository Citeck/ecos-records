package ru.citeck.ecos.records3.record.atts.utils

import ru.citeck.ecos.webapp.api.entity.EntityRef

object RecTypeUtils {

    private const val ECOS_TYPE_APP = "emodel"
    private const val ECOS_TYPE_SOURCE_ID = "type"
    private const val ECOS_TYPE_APP_SOURCE_ID_PREFIX = "$ECOS_TYPE_APP/$ECOS_TYPE_SOURCE_ID@"

    fun anyTypeToRef(type: Any?): EntityRef {
        type ?: return EntityRef.EMPTY
        return when (type) {
            is EntityRef -> type
            is String -> when {
                type.isBlank() -> EntityRef.EMPTY
                type.startsWith(ECOS_TYPE_APP_SOURCE_ID_PREFIX) -> EntityRef.valueOf(type)
                !type.contains('@') -> EntityRef.create(ECOS_TYPE_APP, ECOS_TYPE_SOURCE_ID, type)
                else -> {
                    val ref = EntityRef.valueOf(type)
                    if (ref.getAppName().isBlank()) {
                        ref.withAppName(ECOS_TYPE_APP)
                    } else {
                        ref
                    }
                }
            }
            else -> EntityRef.EMPTY
        }
    }
}
