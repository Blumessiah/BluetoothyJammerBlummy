package api

/**
 * Aggregated parameters for launching an attack. Mirrors the PortaPack Mayhem
 * "Jammer TX" controls: TX/Sleep duty cycle (with jitter applied by the
 * wrapper), payload pattern, rate and intensity.
 */
data class AttackParams(
    val threads: Int = 8,
    val rateDelayMs: Int = 0,
    val txSeconds: Int = 0,
    val sleepSeconds: Int = 0,
    val payloadPattern: PayloadPattern = PayloadPattern.FIXED
)
