package ru.citeck.ecos.records3.record.request

interface ContextAttsProvider {

    fun getContextAttributes(): Map<String, Any?>
}
