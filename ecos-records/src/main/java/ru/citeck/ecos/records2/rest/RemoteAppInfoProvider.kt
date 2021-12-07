package ru.citeck.ecos.records2.rest

interface RemoteAppInfoProvider {

    fun getAppInfo(appName: String): RemoteAppInfo?
}
