package ru.citeck.ecos.records3.record.mixin.external

interface ExtAttHandlerContext {

    fun <T> computeIfAbsent(key: Any, action: (Any) -> T): T
}
