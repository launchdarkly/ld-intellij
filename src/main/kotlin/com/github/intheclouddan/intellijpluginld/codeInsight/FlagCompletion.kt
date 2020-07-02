package com.github.intheclouddan.intellijpluginld.codeInsight

import com.github.intheclouddan.intellijpluginld.FlagStore
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiJavaPatterns.*
import com.intellij.psi.JavaTokenType

import com.intellij.util.ProcessingContext
import com.launchdarkly.api.model.FeatureFlags


@Service
class JavaCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val caretOffset: Int = parameters.editor.getCaretModel().getOffset()
        val elem = parameters.originalFile.findElementAt(caretOffset)
        println(elem)
    }

    companion object {
        val FLAG_KEY = psiElement()
    }

    init {
        extend(CompletionType.BASIC,
                FLAG_KEY,
                object : CompletionProvider<CompletionParameters>() {
                    override fun addCompletions(parameters: CompletionParameters,
                                                context: ProcessingContext,
                                                resultSet: CompletionResultSet) {
                        val project = parameters.originalFile.project
                        val getFlags = project.service<FlagStore>()
                        resultSet.addElement(LookupElementBuilder.create("my-flag-key"))
//                        for (flag in getFlags.flags.items) {
//                                resultSet.addElement(LookupElementBuilder.create(flag.key))
//                        }
                    }

                }
        )
    }
}