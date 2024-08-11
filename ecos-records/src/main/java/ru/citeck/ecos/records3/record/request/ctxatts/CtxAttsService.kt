package ru.citeck.ecos.records3.record.request.ctxatts

import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records3.RecordsServiceFactory
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

class CtxAttsService(private val services: RecordsServiceFactory) {

    private var providers: List<CtxAttsProvider> = emptyList()

    private val thisLock = ReentrantLock()

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

    fun register(provider: CtxAttsProvider) {
        thisLock.lock()
        try {
            register(listOf(provider))
        } finally {
            thisLock.unlock()
        }
    }

    fun register(providers: List<CtxAttsProvider>) {
        thisLock.lock()
        try {
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
        } finally {
            thisLock.unlock()
        }
    }

    fun unregister(provider: CtxAttsProvider) {
        thisLock.lock()
        try {
            unregister(listOf(provider))
        } finally {
            thisLock.unlock()
        }
    }

    fun unregister(providers: List<CtxAttsProvider>) {
        thisLock.lock()
        try {
            if (providers.isEmpty()) {
                return
            }
            val newRegistry = ArrayList(this.providers)
            val identitySet: MutableSet<CtxAttsProvider> = Collections.newSetFromMap(IdentityHashMap(providers.size))
            identitySet.addAll(providers)
            newRegistry.removeIf { identitySet.contains(it) }
            this.providers = newRegistry
        } finally {
            thisLock.unlock()
        }
    }
}
