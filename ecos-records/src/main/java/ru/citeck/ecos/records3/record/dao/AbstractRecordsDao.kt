package ru.citeck.ecos.records3.record.dao

import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.records3.record.mixin.AttMixinsHolder
import java.util.concurrent.CopyOnWriteArrayList

abstract class AbstractRecordsDao : RecordsDao, AttMixinsHolder, ServiceFactoryAware {

    private val mixinsList: MutableList<AttMixin> = CopyOnWriteArrayList()

    protected lateinit var serviceFactory: RecordsServiceFactory
    protected lateinit var predicateService: PredicateService
    protected lateinit var recordsService: RecordsService

    fun addAttributesMixin(mixin: AttMixin) {
        mixinsList.add(mixin)
    }

    /**
     * Remove attributes mixin by reference equality.
     */
    fun removeAttributesMixin(mixin: AttMixin) {
        mixinsList.removeIf { m: AttMixin -> m === mixin }
    }

    override fun toString(): String {
        return "[" + getId() + "](" + javaClass.name + "@" + Integer.toHexString(hashCode()) + ")"
    }

    override fun getMixins(): List<AttMixin> {
        return mixinsList
    }

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        this.serviceFactory = serviceFactory
        recordsService = serviceFactory.recordsServiceV1
        predicateService = serviceFactory.predicateService
    }
}
