package io.github.hephbuild.heph.rpc

import com.intellij.platform.rpc.backend.RemoteApiProvider
import fleet.rpc.remoteApiDescriptor

internal class HephRemoteApiProvider : RemoteApiProvider {
    override fun RemoteApiProvider.Sink.remoteApis() {
        remoteApi(remoteApiDescriptor<HephSettingsApi>()) { HephSettingsApiImpl() }
        remoteApi(remoteApiDescriptor<HephNotificationsApi>()) { HephNotificationsApiImpl() }
    }
}
