package io.github.hephbuild.heph.rpc

import com.intellij.platform.project.ProjectId
import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class HephSettingsDto(
    val binaryPath: String,
    val serverArgs: String,
    val extraFilePatterns: String,
)

/** [searched] is set only when [HephSettingsDto.binaryPath] is blank and auto-detection found nothing. */
@Serializable
data class HephSettingsPageDto(
    val settings: HephSettingsDto,
    val searched: String?,
)

@Serializable
data class HephNotificationDto(
    val title: String,
    val content: String,
    val restartable: Boolean,
)

/** Backend-owned settings, read and written from the frontend settings page. */
@Rpc
interface HephSettingsApi : RemoteApi<Unit> {
    /** Seeds an empty binary path from auto-detection, then returns the stored values. */
    suspend fun load(): HephSettingsPageDto

    /** Persists [settings], schedules the side effects, and returns the stored values without seeding. */
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
