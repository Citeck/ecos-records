package ru.citeck.ecos.records3.record.request.ctxatts

import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records3.RecordsServiceFactory
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

class CtxAttsService(private val services: RecordsServiceFactory) {

    private var providers: List<CtxAttsProvider> = emptyList()

    init {
        register(services.ctxAttsProviders)
    }

    fun getContextAtts(): Map<String, Any?> {
        val attributes = LinkedHashMap<String, Any?>()
        for (provider in providers) {
            provider.fillContextAtts(attributes)
        }
        return attributes
    }

    @Synchronized
    fun register(provider: CtxAttsProvider) {
        register(listOf(provider))
    }

    @Synchronized
    fun register(providers: List<CtxAttsProvider>) {
        if (providers.isEmpty()) {
            return
        }
        val newRegistry = ArrayList(this.providers)
        for (provider in providers) {
            if (provider is ServiceFactoryAware) {
                provider.setRecordsServiceFactory(services)
            }
            newRegistry.add(provider)
        }
        newRegistry.sortBy { it.getOrder() }
        this.providers = newRegistry
    }

    @Synchronized
    fun unregister(provider: CtxAttsProvider) {
        unregister(listOf(provider))
    }

    @Synchronized
    fun unregister(providers: List<CtxAttsProvider>) {
        if (providers.isEmpty()) {
            return
        }
        val newRegistry = ArrayList(this.providers)
        val identitySet: MutableSet<CtxAttsProvider> = Collections.newSetFromMap(IdentityHashMap(providers.size))
        identitySet.addAll(providers)
        newRegistry.removeIf { identitySet.contains(it) }
        this.providers = newRegistry
    }
}
