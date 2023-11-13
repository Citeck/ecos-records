package ru.citeck.ecos.records3.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordRefTestV2 {

    @Test
    fun test() {

        val ref = EntityRef.create("app", "src", "local-id")
        assertThat(ref.withDefault()).isSameAs(ref)

        assertThat(ref.withDefault(appName = "app")).isSameAs(ref)
        assertThat(ref.withDefault(appName = "app", sourceId = "src")).isSameAs(ref)
        assertThat(ref.withDefault(appName = "app", sourceId = "src", localId = "local-id")).isSameAs(ref)

        assertThat(ref.withDefault(appName = "app2")).isSameAs(ref)
        assertThat(ref.withDefault(appName = "app2", sourceId = "src2")).isSameAs(ref)
        assertThat(ref.withDefault(appName = "app2", sourceId = "src2", localId = "local-id2")).isSameAs(ref)

        val emptyRef = EntityRef.create("", "", "")
        assertThat(emptyRef.withDefault(appName = "app").toString()).isEqualTo("app/@")
        assertThat(emptyRef.withDefault(appName = "app", sourceId = "src").toString()).isEqualTo("app/src@")
        assertThat(emptyRef.withDefault(appName = "app", sourceId = "src", localId = "local-id").toString()).isEqualTo("app/src@local-id")

        val withGlobalSourceId = EntityRef.EMPTY.withSourceId("app/source")
        assertThat(withGlobalSourceId.getAppName()).isEqualTo("app")
        assertThat(withGlobalSourceId.getSourceId()).isEqualTo("source")

        val withGlobalSourceId2 = EntityRef.EMPTY.withSourceId("app/")
        assertThat(withGlobalSourceId2.getAppName()).isEqualTo("app")
        assertThat(withGlobalSourceId2.getSourceId()).isEqualTo("")
    }
}
