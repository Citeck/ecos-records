package ru.citeck.ecos.records3.record.request.ctxatts

import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.read.ParseUtils
import ru.citeck.ecos.records3.record.atts.value.impl.AttFuncValue
import ru.citeck.ecos.records3.record.atts.value.impl.auth.AuthContextValue
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

class StdCtxAttsProvider(services: RecordsServiceFactory) : CtxAttsProvider {

    companion object {
        const val ORDER = 1000f

        private val strCtxAtt = AttFuncValue { it }
        private val refCtxAtt = AttFuncValue { EntityRef.valueOf(it) }
        private val numCtxAtt = AttFuncValue { ParseUtils.parseNumValue(it) }
        private val authCtxAtt = AuthContextValue()
    }

    private val props = services.webappProps
    private val webUrl = normalizeWebUrl(props.webUrl)

    override fun fillContextAtts(attributes: MutableMap<String, Any?>) {

        attributes["now"] = { Instant.now() }
        attributes["str"] = strCtxAtt
        attributes["num"] = numCtxAtt
        attributes["ref"] = refCtxAtt
        attributes["auth"] = authCtxAtt

        attributes["appName"] = props.appName
        attributes["appInstanceId"] = props.appInstanceId
        attributes["webUrl"] = webUrl

        val user = AuthContext.getCurrentUser()
        if (user.isNotBlank()) {
            attributes["user"] = EntityRef.create("emodel", "person", user)
        }
    }

    /**
     * Web url always ends with '/' to allow templates like '{{$webUrl}}v2/dashboard'
     */
    private fun normalizeWebUrl(webUrl: String): String {
        return if (webUrl.isEmpty() || webUrl.endsWith("/")) {
            webUrl
        } else {
            "$webUrl/"
        }
    }

    override fun getOrder() = ORDER
}
