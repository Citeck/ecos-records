package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * Tests for the or-else processor fallback chain with multiple attribute-based fallbacks.
 *
 * Verifies that when the primary value is empty and multiple `a:` (attribute) fallbacks
 * are provided, the processor returns the FIRST non-empty fallback — not the last one.
 *
 * This covers a bug where the loop inside AttOrElseProcessor checked the original `value`
 * instead of the newly resolved `newValue`, causing the chain to always return the last fallback.
 */
class OrElseMultiFallbackTest {

    @Test
    fun orElseReturnsFirstNonEmptyFallback() {

        val records = RecordsServiceFactory().recordsService
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord(
                    "rec1",
                    ObjectData.create(
                        """
                        {
                            "emptyField": "",
                            "firstFallback": "first-value",
                            "secondFallback": "second-value"
                        }
                        """.trimIndent()
                    )
                )
                .build()
        )

        val ref = EntityRef.create("test", "rec1")

        // emptyField is empty, so should fall through to firstFallback ("first-value"),
        // NOT to secondFallback ("second-value")
        val result = records.getAtt(ref, "emptyField|or('a:firstFallback','a:secondFallback')").asText()
        assertThat(result).isEqualTo("first-value")
    }

    @Test
    fun orElseReturnsSecondFallbackWhenFirstIsAlsoEmpty() {

        val records = RecordsServiceFactory().recordsService
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord(
                    "rec1",
                    ObjectData.create(
                        """
                        {
                            "emptyField": "",
                            "alsoEmpty": "",
                            "lastFallback": "last-value"
                        }
                        """.trimIndent()
                    )
                )
                .build()
        )

        val ref = EntityRef.create("test", "rec1")

        // both emptyField and alsoEmpty are empty, should return lastFallback
        val result = records.getAtt(ref, "emptyField|or('a:alsoEmpty','a:lastFallback')").asText()
        assertThat(result).isEqualTo("last-value")
    }

    @Test
    fun orElseReturnsConstantWhenAllAttrsEmpty() {

        val records = RecordsServiceFactory().recordsService
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("rec1", ObjectData.create("""{"emptyField": "", "alsoEmpty": ""}"""))
                .build()
        )

        val ref = EntityRef.create("test", "rec1")

        val result = records.getAtt(ref, "emptyField|or('a:alsoEmpty','fallback-constant')").asText()
        assertThat(result).isEqualTo("fallback-constant")
    }

    @Test
    fun orElseShortcircuitReturnsOriginalValueWhenNotEmpty() {

        val records = RecordsServiceFactory().recordsService
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("rec1", ObjectData.create("""{"field": "original"}"""))
                .build()
        )

        val ref = EntityRef.create("test", "rec1")

        val result = records.getAtt(ref, "field|or('a:other','constant')").asText()
        assertThat(result).isEqualTo("original")
    }

    @Test
    fun orElseWithExclamationSyntaxMultipleFallbacks() {

        val records = RecordsServiceFactory().recordsService
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord(
                    "rec1",
                    ObjectData.create(
                        """
                        {
                            "name": "",
                            "title": "the-title",
                            "label": "the-label"
                        }
                        """.trimIndent()
                    )
                )
                .build()
        )

        val ref = EntityRef.create("test", "rec1")

        // name is empty -> should return title ("the-title"), not label
        val result = records.getAtt(ref, "name!title!label").asText()
        assertThat(result).isEqualTo("the-title")
    }
}
