package ru.citeck.ecos.records3.record.request.ctxatts

import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.impl.AttFuncValue
import ru.citeck.ecos.records3.record.atts.value.impl.auth.AuthContextValue
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

class StdCtxAttsProvider(services: RecordsServiceFactory) : CtxAttsProvider {

    companion object {
        const val ORDER = 1000f

        private val strCtxAtt = AttFuncValue { it }
        private val refCtxAtt = AttFuncValue { EntityRef.valueOf(it) }
        private val numCtxAtt = AttFuncValue { it.toDouble() }
        private val authCtxAtt = AuthContextValue()
    }

    private val props = services.webappProps

    override fun fillContextAtts(attributes: MutableMap<String, Any?>) {

        attributes["now"] = { Instant.now() }
        attributes["str"] = strCtxAtt
        attributes["num"] = numCtxAtt
        attributes["ref"] = refCtxAtt
        attributes["auth"] = authCtxAtt

        attributes["appName"] = props.appName
        attributes["appInstanceId"] = props.appInstanceId

        val user = AuthContext.getCurrentUser()
        if (user.isNotBlank()) {
            attributes["user"] = EntityRef.create("emodel", "person", user)
        }
    }

    override fun getOrder() = ORDER
}
