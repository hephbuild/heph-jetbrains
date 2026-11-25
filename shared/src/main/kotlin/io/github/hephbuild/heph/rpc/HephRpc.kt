package io.github.hephbuild.heph.rpc

import com.intellij.platform.project.ProjectId
import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

// Host and client plugin versions can differ in remote development: every field has a default so a payload from
// an older peer still decodes.
@Serializable
data class HephSettingsDto(
    val binaryPath: String = "",
    val serverArgs: String = "",
    val extraFilePatterns: String = "",
)

/**
 * A blank [HephSettingsDto.binaryPath] means auto-detect. Then [detected] is the executable that found and [searched]
 * lists where it looked when it found nothing; both are null for a configured path.
 */
@Serializable
data class HephSettingsPageDto(
    val settings: HephSettingsDto,
    val searched: String? = null,
    val detected: String? = null,
)

@Serializable
data class HephNotificationDto(
    val title: String,
    val content: String,
    val restartable: Boolean = false,
)

/** Backend-owned settings, read and written from the frontend settings page. */
@Rpc
interface HephSettingsApi : RemoteApi<Unit> {
    /** Returns the stored values and, for a blank binary path, what auto-detection finds. Nothing is persisted. */
    suspend fun load(): HephSettingsPageDto

    /** Persists [settings], schedules the side effects, and returns the stored values as [load] does. */
    suspend fun save(settings: HephSettingsDto): HephSettingsPageDto

    companion object {
        suspend fun getInstance(): HephSettingsApi =
            RemoteApiProviderService.resolve(remoteApiDescriptor<HephSettingsApi>())
    }
}

@Rpc
interface HephNotificationsApi : RemoteApi<Unit> {
    suspend fun notifications(projectId: ProjectId): Flow<HephNotificationDto>

    suspend fun restartServer(projectId: ProjectId)

    companion object {
        suspend fun getInstance(): HephNotificationsApi =
            RemoteApiProviderService.resolve(remoteApiDescriptor<HephNotificationsApi>())
    }
}
