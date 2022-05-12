package ru.citeck.ecos.records3

class RecordsProperties {

    @Deprecated("use EcosWebAppContext.getProperties().appName")
    var appName = ""
    @Deprecated("use EcosWebAppContext.getProperties().appInstanceId")
    var appInstanceId = ""
    var sourceIdMapping: Map<String, String> = emptyMap()

    var defaultApp: String = ""
}
