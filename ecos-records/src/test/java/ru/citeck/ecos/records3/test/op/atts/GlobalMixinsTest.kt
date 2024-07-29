package ru.citeck.ecos.records3.test.op.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.records3.record.mixin.AttMixin

class GlobalMixinsTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val dao = InMemDataRecordsDao("test")

        val records = services.recordsService
        records.register(dao)

        val ref = records.create("test", mapOf("field" to "def"))

        val att0 = records.getAtt(ref, "field").asText()
        assertThat(att0).isEqualTo("def")

        services.globalAttMixinsProvider.addMixin(ConstMixin("field", "global"))
        val att1 = records.getAtt(ref, "field").asText()
        assertThat(att1).isEqualTo("global")

        dao.addAttributesMixin(ConstMixin("field", "local"))
        val att2 = records.getAtt(ref, "field").asText()
        assertThat(att2).isEqualTo("local")
    }

    class ConstMixin(val att: String, val value: Any) : AttMixin {
        override fun getAtt(path: String, value: AttValueCtx): Any = this.value
        override fun getProvidedAtts() = listOf(att)
    }
}
