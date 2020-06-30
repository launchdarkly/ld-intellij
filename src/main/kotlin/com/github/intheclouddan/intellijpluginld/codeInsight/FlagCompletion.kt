package com.github.intheclouddan.intellijpluginld.codeInsight

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.intellij.psi.JavaTokenType
import com.github.intheclouddan.intellijpluginld.FlagStore
import com.intellij.patterns.PsiJavaPatterns.psiLiteral
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.patterns.PsiJavaPatterns.psiMethod
import com.launchdarkly.api.model.FeatureFlags
import com.intellij.patterns.PsiJavaPatterns


@Service
class JavaCompletionContributor : CompletionContributor() {
    var flags: FeatureFlags? = null

    init {

        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(JavaTokenType.STRING_LITERAL).withParent(
                    psiElement().methodCallParameter(
                            0,
                    psiMethod()
                            .withName("boolVariation")
                            .definedInClass("com.launchdarkly.sdk.server.LDClient")
                            )),
                object : CompletionProvider<CompletionParameters?>() {
                    override fun addCompletions(parameters: CompletionParameters,
                                                context: ProcessingContext,
                                                resultSet: CompletionResultSet) {
                        val project = parameters.originalFile.project
                        val getFlags = project.service<FlagStore>()
                        for (flag in getFlags.flags.items) {
                                resultSet.addElement(LookupElementBuilder.create(flag.key))
                        }
                    }
                }
        )
    }
}