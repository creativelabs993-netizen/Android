package site.coinforge.mobile

import android.os.SystemClock
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local SHA-256 proof-of-work benchmark.
 *
 * This intentionally does NOT create CoinForge blocks or coins: it has no
 * consensus-valid block template, network target, or block-submission path.
 */
class CpuPowEngine(
    private val onUpdate: (Snapshot) -> Unit,
    private val onFinished: (Snapshot) -> Unit
) {
    data class Snapshot(
        val running: Boolean,
        val hashes: Long,
        val solutions: Long,
        val hashesPerSecond: Long,
        val elapsedSeconds: Long,
        val lastHash: String,
        val message: String
    )

    private val stopFlag = AtomicBoolean(false)
    @Volatile private var running = false
    @Volatile private var hashes = 0L
    @Volatile private var solutions = 0L
    @Volatile private var rate = 0L
    @Volatile private var elapsed = 0L
    @Volatile private var lastHash = "—"
    @Volatile private var message = "Ready. Local benchmark only."
    private var worker: Thread? = null

    @Synchronized fun start() {
        if (running) return
        stopFlag.set(false)
        running = true
        hashes = 0L
        solutions = 0L
        rate = 0L
        elapsed = 0L
        lastHash = "—"
        message = "Running local SHA-256 benchmark; no coins are being mined."
        worker = Thread({ runWorker() }, "coinforge-pow-benchmark").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        stopFlag.set(true)
    }

    fun snapshot(): Snapshot = Snapshot(
        running, hashes, solutions, rate, elapsed, lastHash, message
    )

    private fun runWorker() {
        val digest = MessageDigest.getInstance("SHA-256")
        val prefix = "COINFORGE-LOCAL-BENCHMARK-NOT-A-BLOCK:".toByteArray(Charsets.UTF_8)
        val nonceBytes = ByteArray(8)
        val nonceBuffer = ByteBuffer.wrap(nonceBytes)
        var nonce = 0L
        val started = SystemClock.elapsedRealtime()
        var lastReport = started
        var activeWindowStart = started
        var hashesInWindow = 0L

        try {
            while (!stopFlag.get()) {
                // Limit each burst to about 100 ms, then yield the CPU for 100 ms.
                val burstStart = SystemClock.elapsedRealtime()
                while (!stopFlag.get() &&
                    SystemClock.elapsedRealtime() - burstStart < 100L) {
                    nonceBuffer.clear()
                    nonceBuffer.putLong(nonce++)
                    digest.update(prefix)
                    val hash = digest.digest(nonceBytes)
                    hashes++
                    hashesInWindow++
                    // Benchmark target: 20 leading zero bits. This is not a network target.
                    if (hash[0] == 0.toByte() && hash[1] == 0.toByte() &&
                        (hash[2].toInt() and 0xF0) == 0) {
                        solutions++
                        lastHash = hash.joinToString("") { "%02x".format(it.toInt() and 0xff) }
                    }
                }
                try { Thread.sleep(100L) } catch (_: InterruptedException) { break }

                val now = SystemClock.elapsedRealtime()
                elapsed = (now - started) / 1000L
                if (now - lastReport >= 1000L) {
                    rate = hashesInWindow * 1000L / (now - activeWindowStart).coerceAtLeast(1L)
                    hashesInWindow = 0L
                    activeWindowStart = now
                    lastReport = now
                    message = "Local benchmark only — results are not CoinForge blocks."
                    onUpdate(snapshot())
                }
                // Hard stop after five minutes to reduce accidental prolonged heating.
                if (now - started >= 5 * 60 * 1000L) {
                    message = "Five-minute safety limit reached. Let the phone cool before another test."
                    break
                }
            }
        } catch (e: Exception) {
            message = "Benchmark stopped: ${e.javaClass.simpleName}"
        } finally {
            running = false
            elapsed = (SystemClock.elapsedRealtime() - started) / 1000L
            if (message.startsWith("Running")) message = "Stopped. No coins or blocks were created."
            onFinished(snapshot())
        }
    }
}
