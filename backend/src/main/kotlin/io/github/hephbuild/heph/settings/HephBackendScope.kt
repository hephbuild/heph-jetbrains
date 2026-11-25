package io.github.hephbuild.heph.settings

import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

/** Application-lifetime scope for fire-and-forget backend work. */
@Service(Service.Level.APP)
class HephBackendScope(val scope: CoroutineScope)
