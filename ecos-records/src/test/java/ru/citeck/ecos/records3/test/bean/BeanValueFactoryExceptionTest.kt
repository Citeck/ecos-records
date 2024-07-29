package ru.citeck.ecos.records3.test.bean

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import java.lang.RuntimeException

class BeanValueFactoryExceptionTest {

    @Test
    fun exceptionTest() {

        val records = RecordsServiceFactory().recordsService
        val exception = assertThrows<RuntimeException> {
            records.getAtt(DtoTest(), "?str")
        }
        assertThat(exception::class.java == RuntimeException::class.java).isTrue
        assertThat(exception.message).isEqualTo("toString exception")
    }

    class DtoTest {

        @AttName(ScalarType.STR_SCHEMA)
        override fun toString(): String {
            throw RuntimeException("toString exception")
        }
    }
}
