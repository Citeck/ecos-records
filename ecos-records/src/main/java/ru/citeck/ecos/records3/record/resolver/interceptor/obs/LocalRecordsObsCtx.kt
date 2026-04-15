package ru.citeck.ecos.records3.record.resolver.interceptor.obs

import ru.citeck.ecos.micrometer.obs.EcosObsContext
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttType
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef

class LocalRecordsObsCtx private constructor() {

    companion object {
        private const val NAME_SCOPE = "ecos.records.local"
    }

    class Query(
        val query: RecordsQuery,
        val attributes: List<SchemaAtt>,
        val rawAtts: Boolean,
        val services: RecordsServiceFactory
    ) : EcosObsContext(NAME) {

        companion object {
            const val NAME = "$NAME_SCOPE.query"
        }
    }

    class Mutate(
        val sourceId: String,
        val record: LocalRecordAtts,
        val attsToLoad: List<SchemaAtt>,
        val rawAtts: Boolean,
        val services: RecordsServiceFactory
    ) : EcosObsContext(NAME) {

        companion object {
            const val NAME = "$NAME_SCOPE.mutate"
        }
    }

    class GetAtts(
        val sourceId: String,
        val recordIds: List<String>,
        val attributes: List<SchemaAtt>,
        val rawAtts: Boolean,
        val services: RecordsServiceFactory
    ) : EcosObsContext(NAME) {

        companion object {
            const val NAME = "$NAME_SCOPE.get-atts"
        }
    }

    class Delete(
        val sourceId: String,
        val recordIds: List<String>,
        val services: RecordsServiceFactory
    ) : EcosObsContext(NAME) {

        companion object {
            const val NAME = "$NAME_SCOPE.delete"
        }
    }

    class DaoQuery(
        val sourceId: String,
        val query: RecordsQuery
    ) : EcosObsContext(NAME) {

        companion object {
            const val NAME = "$NAME_SCOPE.dao.query"
        }
    }

    class DaoGetAtts(
        val sourceId: String,
        val recordIds: List<String>
    ) : EcosObsContext(NAME) {

        companion object {
            const val NAME = "$NAME_SCOPE.dao.get-atts"
        }
    }

    class RefsPreload(
        val refs: List<EntityRef>,
        val attsToLoad: Map<String, String>
    ) : EcosObsContext(NAME) {

        companion object {
            const val NAME = "$NAME_SCOPE.refs-preload"
        }
    }

    class Mixin(
        val path: String,
        val mixinClass: String
    ) : EcosObsContext(NAME) {

        companion object {
            const val NAME = "$NAME_SCOPE.mixin"
        }
    }

    class ComputedAtt(
        val id: String,
        val type: RecordComputedAttType,
        val path: String
    ) : EcosObsContext(NAME) {

        companion object {
            const val NAME = "$NAME_SCOPE.computed-att"
        }
    }
}
