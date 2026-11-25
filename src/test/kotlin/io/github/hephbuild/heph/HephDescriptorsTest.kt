package io.github.hephbuild.heph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

class HephDescriptorsTest {
    private val loader = HephDescriptorsTest::class.java.classLoader

    private fun parse(url: java.net.URL): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isExpandEntityReferences = false
        }
        return url.openStream().use { factory.newDocumentBuilder().parse(it) }
    }

    private fun module(name: String): Document {
        val url = loader.getResource("heph-jetbrains.$name.xml")
        assertNotNull("heph-jetbrains.$name.xml is not on the classpath", url)
        return parse(url!!)
    }

    private fun root(): Document {
        val doc = loader.getResources("META-INF/plugin.xml").asSequence()
            .filter { it.protocol == "file" && it.path.endsWith("/META-INF/plugin.xml") }
            .map(::parse)
            .firstOrNull { it.getElementsByTagName("id").item(0)?.textContent == "io.github.hephbuild.heph" }
        assertNotNull("no file-based META-INF/plugin.xml with id io.github.hephbuild.heph on the test classpath", doc)
        return doc!!
    }

    private fun NodeList.elements(): List<Element> = (0 until length).map { item(it) as Element }

    private fun Document.moduleDependencies(): List<String> =
        getElementsByTagName("dependencies").elements()
            .flatMap { it.getElementsByTagName("module").elements() }
            .map { it.getAttribute("name") }

    private fun Document.text(): String =
        javax.xml.transform.TransformerFactory.newInstance().newTransformer().let {
            val out = java.io.StringWriter()
            it.transform(javax.xml.transform.dom.DOMSource(this), javax.xml.transform.stream.StreamResult(out))
            out.toString()
        }

    @Test
    fun rootListsContentModulesWithSharedRequired() {
        val content = root().getElementsByTagName("content").elements().single()
        val modules = content.getElementsByTagName("module").elements()
            .associate { it.getAttribute("name") to it.getAttribute("loading") }
        assertEquals(
            mapOf(
                "heph-jetbrains.shared" to "required",
                "heph-jetbrains.frontend" to "",
                "heph-jetbrains.backend" to "",
            ),
            modules,
        )
    }

    @Test
    fun rootDoesNotDependOnLsp() {
        val depends = root().getElementsByTagName("depends").elements().map { it.textContent }
        assertEquals(listOf("com.intellij.modules.platform"), depends)
        val text = root().text()
        assertFalse(text.contains("modules.lsp") || text.contains("platform.lsp"))
    }

    @Test
    fun moduleDescriptorsUseDependenciesNotDepends() {
        for (name in listOf("shared", "frontend", "backend")) {
            assertEquals(name, 0, module(name).getElementsByTagName("depends").length)
        }
    }

    @Test
    fun sharedDependsOnNeitherFrontendNorBackend() {
        val dependencies = module("shared").moduleDependencies()
        assertTrue(dependencies.toString(), dependencies.none { it.endsWith(".frontend") || it.endsWith(".backend") })
    }

    @Test
    fun lspIsReferencedOnlyFromBackend() {
        val forbidden = listOf("platform.lsp", "intellij.platform.lsp", "intellij.platform.lsp.impl", "com.intellij.modules.lsp")
        for ((name, doc) in mapOf("root" to root(), "shared" to module("shared"), "frontend" to module("frontend"))) {
            val text = doc.text()
            for (needle in forbidden) {
                assertFalse("$name mentions $needle", text.contains(needle))
            }
        }
        val backend = module("backend").moduleDependencies()
        assertTrue(backend.toString(), "intellij.platform.lsp" in backend)
        assertTrue(backend.toString(), "intellij.platform.lsp.impl" in backend)
    }

    @Test
    fun modulesDependOnTheirPlatformSide() {
        assertTrue("intellij.platform.frontend" in module("frontend").moduleDependencies())
        assertTrue("intellij.platform.backend" in module("backend").moduleDependencies())
    }
}
