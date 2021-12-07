package ru.citeck.ecos.records2.rest

data class RemoteAppInfo(
    val recordsBaseUrl: String,
    val recordsUserBaseUrl: String,
    val host: String,
    val ip: String,
    val port: Int,
    val securePortEnabled: Boolean
) {

    companion object {

        @JvmField
        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): RemoteAppInfo {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    class Builder() {

        var recordsBaseUrl: String = ""
        var recordsUserBaseUrl: String = ""
        var host: String = ""
        var ip: String = ""
        var port = 0
        var securePortEnabled: Boolean = false

        constructor(base: RemoteAppInfo) : this() {
            recordsBaseUrl = base.recordsBaseUrl
            recordsUserBaseUrl = base.recordsUserBaseUrl
            host = base.host
            ip = base.ip
            port = base.port
            securePortEnabled = base.securePortEnabled
        }

        fun withRecordsBaseUrl(recordsBaseUrl: String?): Builder {
            this.recordsBaseUrl = recordsBaseUrl ?: ""
            return this
        }

        fun withRecordsUserBaseUrl(recordsUserBaseUrl: String?): Builder {
            this.recordsUserBaseUrl = recordsUserBaseUrl ?: ""
            return this
        }

        fun withHost(host: String?): Builder {
            this.host = host ?: ""
            return this
        }

        fun withIp(ip: String?): Builder {
            this.ip = ip ?: ""
            return this
        }

        fun withPort(port: Int): Builder {
            this.port = port
            return this
        }

        fun withSecurePortEnabled(securePortEnabled: Boolean?): Builder {
            this.securePortEnabled = securePortEnabled ?: false
            return this
        }

        fun build(): RemoteAppInfo {
            return RemoteAppInfo(
                recordsBaseUrl,
                recordsUserBaseUrl,
                host,
                ip,
                port,
                securePortEnabled
            )
        }
    }
}
