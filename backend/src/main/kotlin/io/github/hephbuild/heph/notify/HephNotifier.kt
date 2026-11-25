package io.github.hephbuild.heph.notify

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import io.github.hephbuild.heph.HephBundle
import io.github.hephbuild.heph.core.HephBinaryMessages
import io.github.hephbuild.heph.core.HephBinaryResult
import io.github.hephbuild.heph.rpc.HephNotificationDto
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.ConcurrentHashMap

/**
 * Decides and de-duplicates notifications per project per session; the frontend shows them.
 * Events buffer (newest 16) until a frontend subscribes and are consumed once.
 */
@Service(Service.Level.PROJECT)
class HephNotifier : Disposable {
    private val shown = ConcurrentHashMap.newKeySet<String>()
    private val channel = Channel<HephNotificationDto>(CAPACITY, BufferOverflow.DROP_OLDEST)

    val events: ReceiveChannel<HephNotificationDto> get() = channel

    fun binaryProblem(result: HephBinaryResult) {
        when (result) {
            is HephBinaryResult.Found -> Unit
            is HephBinaryResult.Failure -> post(
                HephBinaryMessages.key(result),
                HephBinaryMessages.title(result),
                HephBinaryMessages.content(result),
                restartable = false,
            )
        }
    }

    /** Re-arms every notification, e.g. after a restart or a healthy start. */
    fun reset() {
        shown.clear()
    }

    fun serverCrashed() {
        post(
            CRASHED_KEY,
            HephBundle.message("notification.server.crashed.title"),
            HephBundle.message("notification.server.crashed.content"),
            restartable = true,
        )
    }

    private fun post(key: String, title: String, content: String, restartable: Boolean) {
        if (!shown.add(key)) return
        LOG.info("$title: $content")
        channel.trySend(HephNotificationDto(title, content, restartable))
    }

    override fun dispose() {
        channel.close()
    }

    companion object {
        private const val CAPACITY = 16
        private const val CRASHED_KEY = "crashed"
        private val LOG = logger<HephNotifier>()
    }
}
