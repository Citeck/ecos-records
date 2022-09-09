package ru.citeck.ecos.records3.test.record.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext
import kotlin.test.assertTrue

class RequestTransactionTest {

    @Test
    fun test() {

        RecordsServiceFactory()

        val inCtxMessage = "inCtx"
        val afterCommitMessage = "after-commit"

        val msgs = mutableListOf<String>()

        RequestContext.doWithTxn {
            AuthContext.runAs("user") {
                repeat(5) {
                    RequestContext.doAfterCommit {
                        if (it == 3) {
                            error("expected error")
                        }
                        msgs.add(afterCommitMessage)
                        assertTrue("is run as system") { AuthContext.isRunAsSystem() }
                    }
                }
                msgs.add(inCtxMessage)
            }
        }

        assertThat(msgs).containsExactly(
            inCtxMessage,
            // one message completed with error, and we expect only 4 elements
            afterCommitMessage,
            afterCommitMessage,
            afterCommitMessage,
            afterCommitMessage
        )
    }
}
