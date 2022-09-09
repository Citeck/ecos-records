package ru.citeck.ecos.records3.exception

open class RecordsException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
