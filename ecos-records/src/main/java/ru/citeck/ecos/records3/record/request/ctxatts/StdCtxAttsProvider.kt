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

        if (props.peopleSourceId.isNotBlank()) {
            attributes["user"] = getCurrentUserRef(props.peopleSourceId)
        }
    }

    private fun getCurrentUserRef(peopleSourceId: String): RecordRef {
        val currentUser = AuthContext.getCurrentUser()
        if (currentUser.isEmpty() || peopleSourceId.isBlank()) {
            return RecordRef.EMPTY
        }
        return RecordRef.valueOf(peopleSourceId + RecordRef.SOURCE_DELIMITER + currentUser)
    }

    override fun getOrder() = ORDER
}
