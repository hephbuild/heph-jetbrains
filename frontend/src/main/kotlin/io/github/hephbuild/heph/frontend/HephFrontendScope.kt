package io.github.hephbuild.heph.frontend

import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

/** Application-lifetime scope for frontend work that outlives a single dialog or project. */
@Service(Service.Level.APP)
class HephFrontendScope(val scope: CoroutineScope)
