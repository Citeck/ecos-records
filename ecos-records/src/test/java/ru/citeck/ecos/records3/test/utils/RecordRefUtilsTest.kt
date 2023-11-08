package ru.citeck.ecos.records3.test.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.utils.RecordRefUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordRefUtilsTest {

    @Test
    fun mapAppIdAndSourceIdTest() {

        val currentAppName = "test-app"
        fun mapTest(srcId: String, mapping: Map<String, String>?, expected: String) {
            val res = RecordRefUtils.mapAppIdAndSourceId(
                EntityRef.create(srcId, "localId"),
                currentAppName,
                mapping
            )
            val appName = res.getAppName()
            var resSrc = res.getSourceId()
            if (appName.isNotBlank()) {
                resSrc = "$appName/$resSrc"
            }
            assertThat(resSrc).isEqualTo(expected)
        }

        mapTest("custom", null, "custom")
        mapTest("custom", mapOf("custom" to "abc"), "abc")
        mapTest("app/custom", mapOf("custom" to "abc"), "app/custom")
        mapTest("test-app/custom", mapOf("custom" to "abc"), "test-app/abc")
        mapTest("custom", mapOf("test-app/custom" to "abc"), "abc")
        mapTest("custom", mapOf("other-app/custom" to "abc"), "custom")
        mapTest("test-app/custom", mapOf("test-app/custom" to "abc"), "test-app/abc")
        mapTest("test-app/custom", mapOf("test-app/custom" to "emodel/abc"), "emodel/abc")
        mapTest("custom", mapOf("test-app/custom" to "emodel/abc"), "emodel/abc")
    }
}
