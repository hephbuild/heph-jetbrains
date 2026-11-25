package io.github.hephbuild.heph.frontend.notify

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class HephNotificationsSubscriber : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Tests drain HephNotifier.events themselves; a collector would steal the events.
        if (ApplicationManager.getApplication().isUnitTestMode) return
        project.service<HephFrontendNotifier>().subscribe()
    }
}
