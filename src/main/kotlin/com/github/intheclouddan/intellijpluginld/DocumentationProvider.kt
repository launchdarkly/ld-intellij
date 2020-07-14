package com.github.intheclouddan.intellijpluginld

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.launchdarkly.api.model.FeatureFlag

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

            //val flag: FeatureFlag? = getFlags.flags.items.find { it.key == flagKey }

            val result = StringBuilder()
            result.append("<html>")
            result.append("<h2>${flag.name ?: flag.key}</h2>")
            result.append("<pre>")
            result.append("LaunchDarkly Feature Flag")
            result.append("</pre>")
            result.append("${flag.description}")
            result.append("</html>")

            return result.toString()
        }

        return null
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {

        return null
    }

    override fun getCustomDocumentationElement(editor: Editor, file: PsiFile, contextElement: PsiElement?): PsiElement? {

        return null
    }

}