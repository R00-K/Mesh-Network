package org.disastermesh.core.messaging

/** Retry schedule shared by the Android sender and JVM tests. */
object MessageRetryPolicy {
    const val MAX_SEND_ATTEMPTS = 6 // First write plus five retransmissions.
    const val FINAL_ACK_TIMEOUT_MS = 16_000L
    val retryDelaysMs: List<Long> = listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L)

    /** Returns the wait after [completedAttempts], or the final acknowledgement window. */
    fun delayAfterAttempt(completedAttempts: Int): Long {
        require(completedAttempts in 1..MAX_SEND_ATTEMPTS)
        return retryDelaysMs.getOrNull(completedAttempts - 1) ?: FINAL_ACK_TIMEOUT_MS
    }
}
