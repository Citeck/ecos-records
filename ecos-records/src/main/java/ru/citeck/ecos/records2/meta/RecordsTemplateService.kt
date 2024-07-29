package ru.citeck.ecos.records2.meta

import ru.citeck.ecos.commons.utils.TmplUtils
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.atts.value.RecordAttValueCtx
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordsTemplateService : ServiceFactoryAware {

    private lateinit var recordsService: RecordsService

    fun <T> resolve(template: T, recordRef: EntityRef?): T {
        return if (EntityRef.isEmpty(recordRef)) {
            template
        } else {
            resolve(template, recordRef as Any?)
        }
    }

    fun <T> resolve(template: T, record: Any?): T {
        if (template == null || record == null) {
            return template
        }
        val atts = TmplUtils.getAtts(template)
        if (atts.isEmpty()) {
            return template
        }
        val valueCtx = if (record is AttValueCtx) {
            record
        } else {
            RecordAttValueCtx(record, recordsService)
        }
        val attsRes = valueCtx.getAtts(atts)
        return TmplUtils.applyAtts(template, attsRes)
            ?: error("Apply atts can't be performed. Template: $template Record: $record")
    }

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        this.recordsService = serviceFactory.recordsService
    }
}
