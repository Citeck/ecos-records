package ru.citeck.ecos.records3.spring.web.interceptor

interface AuthHeaderProvider {

    fun getAuthHeader(userName: String): String?

    fun getSystemAuthHeader(userName: String): String?
}
