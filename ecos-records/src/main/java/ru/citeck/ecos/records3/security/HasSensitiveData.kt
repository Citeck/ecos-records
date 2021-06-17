package ru.citeck.ecos.records3.security

interface HasSensitiveData<T : Any> {

    fun withoutSensitiveData(): T
}
