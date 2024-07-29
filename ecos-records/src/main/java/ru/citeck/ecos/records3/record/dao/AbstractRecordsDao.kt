package ru.citeck.ecos.records3.record.dao

import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.records3.record.mixin.AttMixinsHolder
import ru.citeck.ecos.records3.record.mixin.MixinContext
import ru.citeck.ecos.records3.record.mixin.MixinContextImpl

abstract class AbstractRecordsDao @JvmOverloads constructor(
    private val addGlobalMixins: Boolean = true
) : RecordsDao, AttMixinsHolder, ServiceFactoryAware {

    private val mixinContext = MixinContextImpl()

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
        recordsService = serviceFactory.recordsService
        predicateService = serviceFactory.predicateService
        if (addGlobalMixins) {
            mixinContext.addMixinsProvider(serviceFactory.globalAttMixinsProvider)
        }
    }
}
