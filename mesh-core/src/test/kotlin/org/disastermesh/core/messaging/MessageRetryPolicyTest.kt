package org.disastermesh.core.messaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MessageRetryPolicyTest {
    @Test
    fun retriesUseBoundedExponentialBackoffAndFinalAckWindow() {
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 16_000L),
            (1..MessageRetryPolicy.MAX_SEND_ATTEMPTS).map(MessageRetryPolicy::delayAfterAttempt))
    }

    @Test
    fun rejectsAttemptNumbersOutsideTheSendLifecycle() {
        assertFailsWith<IllegalArgumentException> { MessageRetryPolicy.delayAfterAttempt(0) }
        assertFailsWith<IllegalArgumentException> {
            MessageRetryPolicy.delayAfterAttempt(MessageRetryPolicy.MAX_SEND_ATTEMPTS + 1)
        }
    }
}
