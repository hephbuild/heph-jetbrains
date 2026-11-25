package io.github.hephbuild.heph

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.lang.LanguageBraceMatching
import com.intellij.lang.LanguageCommenters
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.LanguageQuoteHandling
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.colors.ColorSettingsPages
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.hephbuild.heph.filetype.HephFileTypeOverrider
import io.github.hephbuild.heph.frontend.settings.HephConfigurable
import io.github.hephbuild.heph.lang.HephFileType
import io.github.hephbuild.heph.lang.HephLanguage
import io.github.hephbuild.heph.lang.editor.HephBraceMatcher
import io.github.hephbuild.heph.lang.editor.HephClosingBracketHandler
import io.github.hephbuild.heph.lang.editor.HephCommenter
import io.github.hephbuild.heph.lang.editor.HephEnterHandler
import io.github.hephbuild.heph.lang.editor.HephQuoteHandler
import io.github.hephbuild.heph.lang.highlight.HephColorSettingsPage
import io.github.hephbuild.heph.lang.highlight.HephSyntaxHighlighterFactory
import io.github.hephbuild.heph.lang.parser.HephParserDefinition
import io.github.hephbuild.heph.lsp.HephLspIntegrationProvider

/** Every registration resolves in the test IDE, whichever content module declares it. */
class HephPluginModulesTest : BasePlatformTestCase() {
    fun testRegistrationsResolve() {
        val cases: Map<String, Pair<() -> Any?, Class<*>>> = mapOf(
            "file type" to ({ FileTypeManager.getInstance().findFileTypeByName("Heph") } to HephFileType::class.java),
            "parser definition" to ({ LanguageParserDefinitions.INSTANCE.forLanguage(HephLanguage) } to HephParserDefinition::class.java),
            "syntax highlighter factory" to ({ SyntaxHighlighterFactory.LANGUAGE_FACTORY.forLanguage(HephLanguage) } to HephSyntaxHighlighterFactory::class.java),
            "commenter" to ({ LanguageCommenters.INSTANCE.forLanguage(HephLanguage) } to HephCommenter::class.java),
            "brace matcher" to ({ LanguageBraceMatching.INSTANCE.forLanguage(HephLanguage) } to HephBraceMatcher::class.java),
            "quote handler" to ({ LanguageQuoteHandling.INSTANCE.forLanguage(HephLanguage) } to HephQuoteHandler::class.java),
            "enter handler" to ({ EnterHandlerDelegate.EP_NAME.extensionList.firstOrNull { it is HephEnterHandler } } to HephEnterHandler::class.java),
            "typed handler" to ({ TypedHandlerDelegate.EP_NAME.extensionList.firstOrNull { it is HephClosingBracketHandler } } to HephClosingBracketHandler::class.java),
            "colour settings page" to ({ ColorSettingsPages.getInstance().registeredPages.firstOrNull { it is HephColorSettingsPage } } to HephColorSettingsPage::class.java),
            "file type overrider" to ({ FileTypeOverrider.EP_NAME.extensionList.firstOrNull { it is HephFileTypeOverrider } } to HephFileTypeOverrider::class.java),
            "lsp integration provider" to ({ LspIntegrationProvider.EP_NAME.extensionList.firstOrNull { it is HephLspIntegrationProvider } } to HephLspIntegrationProvider::class.java),
        )
        for ((name, case) in cases) {
            val actual = case.first()
            assertNotNull("$name is not registered", actual)
            assertTrue("$name resolved to ${actual!!.javaClass}", case.second.isInstance(actual))
        }
    }

    fun testSettingsPageRegistered() {
        val ep = Configurable.APPLICATION_CONFIGURABLE.extensionList.firstOrNull { it.id == "io.github.hephbuild.heph.settings" }
        assertNotNull("settings page is not registered", ep)
        assertEquals(HephConfigurable::class.java.name, ep!!.instanceClass)
    }

    fun testRemoteApiProviderRegistered() {
        val providers = ExtensionPointName<Any>("com.intellij.platform.rpc.backend.remoteApiProvider").filterableLazySequence()
        assertTrue(providers.any { it.implementationClassName == "io.github.hephbuild.heph.rpc.HephRemoteApiProvider" })
    }

    fun testNotificationGroupRegistered() {
        assertNotNull(NotificationGroupManager.getInstance().getNotificationGroup("Heph"))
    }
}
