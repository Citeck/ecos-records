package ru.citeck.ecos.records3.record.dao

import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.records3.record.mixin.AttMixinsHolder
import ru.citeck.ecos.records3.record.mixin.MixinContext

abstract class AbstractRecordsDao : RecordsDao, AttMixinsHolder, ServiceFactoryAware {

    private val mixinContext: MixinContext = MixinContext()

    protected lateinit var serviceFactory: RecordsServiceFactory
    protected lateinit var predicateService: PredicateService
    protected lateinit var recordsService: RecordsService

    fun addAttributesMixin(mixin: AttMixin) {
        mixinContext.addMixin(mixin)
    }

    override fun toString(): String {
        return "[" + getId() + "](" + javaClass.name + "@" + Integer.toHexString(hashCode()) + ")"
    }

    override fun getMixinContext(): MixinContext {
        return mixinContext
    }

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        this.serviceFactory = serviceFactory
        recordsService = serviceFactory.recordsServiceV1
        predicateService = serviceFactory.predicateService
    }
}
