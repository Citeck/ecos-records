package ru.citeck.ecos.records3.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateWithAnyResDao
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*
import kotlin.collections.HashMap

class SourceIdWithAppInstanceIdTest {

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi? {
                return EcosWebAppApiMock()
            }
        }
        val records = services.recordsService
        records.register(TestDao())

        val res = records.mutate("test@", mapOf("aa" to "bb"))

        assertThat(records.getAtt(res, "aa").asText()).isEqualTo("bb")
    }

    class TestDao : RecordMutateWithAnyResDao, RecordAttsDao, ServiceFactoryAware {

        private lateinit var appName: String
        private lateinit var instanceId: String

        private val records = HashMap<String, ObjectData>()

        override fun getId(): String {
            return "test"
        }

        override fun getRecordAtts(recordId: String): Any? {
            return records[recordId]
        }

        override fun mutateForAnyRes(record: LocalRecordAtts): Any {
            val recId = UUID.randomUUID().toString()
            records[recId] = record.getAtts()
            return EntityRef.create("$appName:$instanceId", getId(), recId)
        }

        override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
            appName = serviceFactory.webappProps.appName
            instanceId = serviceFactory.webappProps.appInstanceId
        }
    }
}
