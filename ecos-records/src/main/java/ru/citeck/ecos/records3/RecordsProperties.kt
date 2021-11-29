package ru.citeck.ecos.records3

class RecordsProperties {

    var appName = ""
    var appInstanceId = ""
    var rest: RestProps = RestProps()
    var apps: Map<String, App> = emptyMap()
    var sourceIdMapping: Map<String, String> = emptyMap()
    var peopleSourceId: String = ""
    var tls: Tls = Tls()

    /**
     * Used by gateway.
     */
    var gatewayMode = false
    var defaultApp: String = ""

    class App {
        var recBaseUrl: String = ""
        var recUserBaseUrl: String = ""
        var auth: Authentication? = null
        var tls: AppTls = AppTls()
    }

    class RestProps {
        @Deprecated("use tls properties instead")
        var secure: Boolean = false
    }

    class Authentication {
        var username: String? = null
        var password: String? = null
    }

    class Tls {
        var enabled: Boolean = false
        var keyStore: String = ""
        var keyStoreType: String = "PKCS12"
        var keyStorePassword: String? = null
        var keyStoreKeyAlias: String? = null
        var trustStore: String = ""
        var trustStoreType: String = "PKCS12"
        var trustStorePassword: String? = null
    }

    class AppTls {
        var enabled: Boolean? = null
    }
}
