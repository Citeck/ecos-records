package ru.citeck.ecos.records3.record.ref

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.EntityRefFactory

class RecordEntityRefFactory : EntityRefFactory {

    // this code required to use RecordRef as EntityRef in runtime for backward compatibility
    override fun getEntityRef(appName: String, sourceId: String, localId: String): EntityRef {
        @Suppress("DEPRECATION")
        return RecordRef.create(appName, sourceId, localId)
    }

    override fun getPriority(): Int {
        return 10
    }
}
