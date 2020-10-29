package ru.citeck.ecos.records2

class RecordsProperties {

    var appName = ""
    var appInstanceId = ""
    var rest: RestProps = RestProps()
    var apps: Map<String, App> = emptyMap()
    var sourceIdMapping: Map<String, String> = emptyMap()

    /**
     * Disable remote records for testing.
     */
    var forceLocalMode = false

    /**
     * Used by gateway.
     */
    var gatewayMode = false
    var defaultApp: String = ""

    class App {
        var recBaseUrl: String? = null
        var recUserBaseUrl: String? = null
        var auth: Authentication? = null
    }

    class RestProps {
        var secure: Boolean = false
    }

    class Authentication {
        var username: String? = null
        var password: String? = null
    }
}
