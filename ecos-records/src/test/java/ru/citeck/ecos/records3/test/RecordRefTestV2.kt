package ru.citeck.ecos.records3.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef

class RecordRefTestV2 {

    @Test
    fun test() {

        val ref = RecordRef.create("app", "src", "local-id")
        assertThat(ref.withDefault()).isSameAs(ref)

        assertThat(ref.withDefault(appName = "app")).isSameAs(ref)
        assertThat(ref.withDefault(appName = "app", sourceId = "src")).isSameAs(ref)
        assertThat(ref.withDefault(appName = "app", sourceId = "src", localId = "local-id")).isSameAs(ref)

        assertThat(ref.withDefault(appName = "app2")).isSameAs(ref)
        assertThat(ref.withDefault(appName = "app2", sourceId = "src2")).isSameAs(ref)
        assertThat(ref.withDefault(appName = "app2", sourceId = "src2", localId = "local-id2")).isSameAs(ref)

        val emptyRef = RecordRef.create("", "", "")
        assertThat(emptyRef.withDefault(appName = "app").toString()).isEqualTo("app/@")
        assertThat(emptyRef.withDefault(appName = "app", sourceId = "src").toString()).isEqualTo("app/src@")
        assertThat(emptyRef.withDefault(appName = "app", sourceId = "src", localId = "local-id").toString()).isEqualTo("app/src@local-id")
    }
}
