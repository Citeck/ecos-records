package ru.citeck.ecos.records3.record.request.ctxatts

import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.impl.AttFuncValue
import ru.citeck.ecos.records3.record.atts.value.impl.auth.AuthContextValue
import java.time.Instant

class StdCtxAttsProvider(services: RecordsServiceFactory) : CtxAttsProvider {

    companion object {
        const val ORDER = 1000f

        private val strCtxAtt = AttFuncValue { it }
        private val refCtxAtt = AttFuncValue { RecordRef.valueOf(it) }
        private val authCtxAtt = AuthContextValue()
    }

    private val props = services.properties

    override fun fillContextAtts(attributes: MutableMap<String, Any?>) {

        attributes["now"] = { Instant.now() }
        attributes["str"] = strCtxAtt
        attributes["ref"] = refCtxAtt
        attributes["auth"] = authCtxAtt

        attributes["appName"] = props.appName
        attributes["appInstanceId"] = props.appInstanceId

        val user = AuthContext.getCurrentUser()
        if (user.isNotBlank()) {
            attributes["user"] = RecordRef.create("emodel", "person", user)
        }
    }

    override fun getOrder() = ORDER
}
