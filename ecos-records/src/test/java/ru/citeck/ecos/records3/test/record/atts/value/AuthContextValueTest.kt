package ru.citeck.ecos.records3.test.record.atts.value

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsServiceFactory

/**
 * Tests for AuthContextValue — the $auth context attribute.
 *
 * Covers a bug where AuthContextValue.getAtt() had a `when` block without `return`,
 * causing "full" and "runAs" branches to be silently discarded. Both `$auth.full.user`
 * and `$auth.runAs.user` would return null instead of the correct auth data.
 */
class AuthContextValueTest {

    @Test
    fun authFullReturnsFullAuthUser() {

        val records = RecordsServiceFactory().recordsService

        val result = AuthContext.runAsFull("fullUser", listOf("GROUP_A")) {
            AuthContext.runAs("runAsUser") {
                records.getAtt(null, "\$auth.full.user?str").asText()
            }
        }

        assertThat(result).isEqualTo("fullUser")
    }

    @Test
    fun authRunAsReturnsRunAsUser() {

        val records = RecordsServiceFactory().recordsService

        val result = AuthContext.runAsFull("fullUser", listOf("GROUP_A")) {
            AuthContext.runAs("runAsUser") {
                records.getAtt(null, "\$auth.runAs.user?str").asText()
            }
        }

        assertThat(result).isEqualTo("runAsUser")
    }

    @Test
    fun authRunAsDiffersFromFull() {

        val records = RecordsServiceFactory().recordsService

        val result = AuthContext.runAsFull("fullUser", listOf("ROLE_ADMIN")) {
            AuthContext.runAs("runAsUser", listOf("ROLE_USER")) {
                val fullUser = records.getAtt(null, "\$auth.full.user?str").asText()
                val runAsUser = records.getAtt(null, "\$auth.runAs.user?str").asText()
                val fullIsAdmin = records.getAtt(null, "\$auth.full.isAdmin?bool").asBoolean()
                val runAsIsAdmin = records.getAtt(null, "\$auth.runAs.isAdmin?bool").asBoolean()
                "$fullUser/$runAsUser/$fullIsAdmin/$runAsIsAdmin"
            }
        }

        assertThat(result).isEqualTo("fullUser/runAsUser/true/false")
    }

    @Test
    fun authDirectAttsFallBackToFullAuth() {

        val records = RecordsServiceFactory().recordsService

        val result = AuthContext.runAsFull("theUser", listOf("ROLE_ADMIN")) {
            records.getAtt(null, "\$auth.user?str").asText()
        }

        assertThat(result).isEqualTo("theUser")
    }
}
