package com.github.intheclouddan.intellijpluginld

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.castSafelyTo
import com.launchdarkly.api.model.FeatureFlag
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths

class LDDocumentationProvider() : AbstractDocumentationProvider() {

    override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): List<String>? {
        return null
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) {
            return null
        }
        val getFlags = element.project.service<FlagStore>()
        val flag: FeatureFlag? = getFlags.flags.items.find { it.key == element.text.drop(1).dropLast(1) }

        if (flag != null) {
            val flagKey = element.text.drop(1).dropLast(1)

            val flag: FeatureFlag? = getFlags.flags.items.find { it.key == flagKey }

            val result = StringBuilder()
            result.append("<html>")
            result.append("<h2>${flag!!.name ?: flag!!.key}</h2>")
            result.append("<pre>")
            result.append("LaunchDarkly Feature Flag")
            result.append("</pre>")
            result.append("${flag!!.description}")
            result.append("</html>")

            return result.toString()
        }

        return null
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
//        try {
//            if (originalElement == null || !originalElement.text.startsWith("ngx")) {
//                return null
//            }
            return null
//            var text = originalElement.text
//            val openBrace = text.indexOf('(')
//            if (openBrace > -1) {
//                text = text.substring(0, openBrace)
//            }
//
//            if (!NgxLuaKeywords.isAKeyword(text)) {
//                return null
//            }
//            val path = javaClass.classLoader.getResource("/quickDocs/$text.txt").toURI()
//            if (!Files.exists(Paths.get(path))) {
//                return null
//            }
//            val lines = Files.readAllLines(Paths.get(path))
//            return lines.joinToString("\n")
//        } catch (e: IOException) {
//            LOG.error("IOException getting log information for element {} and originalElement {}", e, element, originalElement)
//        } catch (e: URISyntaxException) {
//            LOG.error("URISyntaxException getting log information for element {} and originalElement {}", e, element, originalElement)
//        }

        return null
    }

    override fun getCustomDocumentationElement(editor: Editor, file: PsiFile, contextElement: PsiElement?): PsiElement? {


//        return if (NgxLuaKeywords.isAKeyword(contextElement?.parent?.parent?.text ?: "")) {
//            contextElement?.parent?.parent
//        } else {
//            null
//        }
        return null
    }

//    companion object {
//        private val LOG = LoggerFactory.getLogger(NgxLuaDocumentationProvider::class.java)
//    }
}