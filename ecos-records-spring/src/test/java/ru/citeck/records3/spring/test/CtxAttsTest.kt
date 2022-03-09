package ru.citeck.records3.spring.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsProvider

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [TestApp::class])
@Import(CtxAttsTest.TestConfig::class)
class CtxAttsTest {

    companion object {
        const val TEST_ATT_NAME = "test-ctx-att"
        const val TEST_ATT_VALUE = "test-ctx-att-value"
    }

    @Autowired
    lateinit var records: RecordsService

    @Test
    fun test() {
        val value = records.getAtt("", "$$TEST_ATT_NAME")
        assertThat(value).isEqualTo(DataValue.createStr(TEST_ATT_VALUE))
    }

    @TestConfiguration
    open class TestConfig {

        @Bean
        open fun testCtxAttsProvider(): CtxAttsProvider {
            return object : CtxAttsProvider {
                override fun fillContextAtts(attributes: MutableMap<String, Any?>) {
                    attributes[TEST_ATT_NAME] = TEST_ATT_VALUE
                }
            }
        }
    }
}
