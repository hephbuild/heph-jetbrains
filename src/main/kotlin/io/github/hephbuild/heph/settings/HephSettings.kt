package io.github.hephbuild.heph.settings

import io.github.hephbuild.heph.core.HephCommandLine
import io.github.hephbuild.heph.core.HephFileMatcher
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "HephSettings", storages = [Storage("heph.xml")])
class HephSettings : SimplePersistentStateComponent<HephSettings.State>(State()) {
    class State : BaseState() {
        var binaryPath by string("")
        var serverArgs by property(HephCommandLine.DEFAULT_ARGS) { it == HephCommandLine.DEFAULT_ARGS }
        var extraFilePatterns by string("")
    }

    /** Empty means auto-detect. */
    var binaryPath: String
        get() = state.binaryPath.orEmpty()
        set(value) {
            state.binaryPath = value
        }

    var serverArgs: String
        get() = state.serverArgs
        set(value) {
            state.serverArgs = value
        }

    var extraFilePatterns: String
        get() = state.extraFilePatterns.orEmpty()
        set(value) {
            state.extraFilePatterns = value
        }

    @Volatile
    private var matcherCache: Pair<String, HephFileMatcher>? = null

    /** Default names plus [extraFilePatterns]; rebuilt only when the patterns change. */
    val fileMatcher: HephFileMatcher
        get() {
            val patterns = extraFilePatterns
            matcherCache?.let { if (it.first == patterns) return it.second }
            return HephFileMatcher(patterns).also { matcherCache = patterns to it }
        }

    companion object {
        fun getInstance(): HephSettings = service()
    }
}
