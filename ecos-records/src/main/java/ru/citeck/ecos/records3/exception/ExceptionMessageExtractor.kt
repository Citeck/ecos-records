package ru.citeck.ecos.records3.exception

interface ExceptionMessageExtractor<T : Throwable> {

    fun getMessage(exception: T): String?

    fun getOrder(): Float
}
