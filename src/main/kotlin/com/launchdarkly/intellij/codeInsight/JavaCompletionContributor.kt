package com.launchdarkly.intellij.codeInsight

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.PsiJavaPatterns.psiLiteral
import com.intellij.patterns.PsiJavaPatterns.psiMethod
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.launchdarkly.intellij.FlagStore
import com.launchdarkly.intellij.LDIcons
import org.json.simple.JSONObject

val FLAG_KEY_BOOL = LDPsiCaptureFactory("boolVariation", "com.launchdarkly.sdk.server.LDClient")
val FLAG_KEY_BOOL_DETAIL = LDPsiCaptureFactory("boolVariationDetail", "com.launchdarkly.sdk.server.LDClient")
val FLAG_KEY_STRING = LDPsiCaptureFactory("stringVariation", "com.launchdarkly.sdk.server.LDClient")
val FLAG_KEY_STRING_DETAIL = LDPsiCaptureFactory("stringVariationDetail", "com.launchdarkly.sdk.server.LDClient")
val FLAG_KEY_JSON = LDPsiCaptureFactory("jsonValueVariation", "com.launchdarkly.sdk.server.LDClient")
val FLAG_KEY_JSON_DETAIL = LDPsiCaptureFactory("jsonValueVariationDetail", "com.launchdarkly.sdk.server.LDClient")
val FLAG_KEY_INT = LDPsiCaptureFactory("intVariation", "com.launchdarkly.sdk.server.LDClient")
val FLAG_KEY_INT_DETAIL = LDPsiCaptureFactory("intVariationDetail", "com.launchdarkly.sdk.server.LDClient")
val FLAG_KEY_DOUBLE = LDPsiCaptureFactory("doubleVariation", "com.launchdarkly.sdk.server.LDClient")
val FLAG_KEY_DOUBLE_DETAIL = LDPsiCaptureFactory("doubleVariationDetail", "com.launchdarkly.sdk.server.LDClient")

fun LDPsiCaptureFactory(methodName: String, className: String): PsiElementPattern.Capture<PsiElement> {
    return psiElement(JavaTokenType.STRING_LITERAL).withParent(
        psiLiteral().methodCallParameter(
            0,
            psiMethod()
                .withName(methodName)
                .definedInClass(className)
        )
    )
}

@Service
class JavaCompletionContributor : CompletionContributor() {

//    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
//        val caretOffset: Int = parameters.editor.getCaretModel().getOffset()
//        val elem = parameters.originalFile.findElementAt(caretOffset)
//        println(elem)
//        super.fillCompletionVariants(parameters, result)
//    }

    init {
        extend(
            CompletionType.BASIC,
            psiElement().andOr(FLAG_KEY_BOOL, FLAG_KEY_BOOL_DETAIL),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet
                ) {
                    val project = parameters.originalFile.project
                    val getFlags = project.service<FlagStore>()
                    for (flag in getFlags.flags.items) {
                        if (flag.kind == "boolean") {
                            var builder: LookupElementBuilder = LookupElementBuilder.create(flag.key)
                                .withTypeText(flag.description)
                                .withIcon(LDIcons.LOGO)
                            resultSet.addElement(builder)
                        }
                    }
                }
            }
        )
        extend(
            CompletionType.BASIC,
            psiElement().andOr(FLAG_KEY_STRING, FLAG_KEY_STRING_DETAIL),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet
                ) {
                    val project = parameters.originalFile.project
                    val getFlags = project.service<FlagStore>()
                    for (flag in getFlags.flags.items) {
                        if (flag.kind == "multivariate" && flag.variations[0].value is String) {
                            var builder: LookupElementBuilder = LookupElementBuilder.create(flag.key)
                                .withTypeText(flag.description)
                                .withIcon(LDIcons.LOGO)
                            resultSet.addElement(builder)
                        }
                    }
                }
            }
        )
        extend(
            CompletionType.BASIC,
            psiElement().andOr(FLAG_KEY_JSON, FLAG_KEY_JSON_DETAIL),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet
                ) {
                    val project = parameters.originalFile.project
                    val getFlags = project.service<FlagStore>()
                    for (flag in getFlags.flags.items) {
                        if (flag.kind == "multivariate" && flag.variations[0].value is JSONObject) {
                            var builder: LookupElementBuilder = LookupElementBuilder.create(flag.key)
                                .withTypeText(flag.description)
                                .withIcon(LDIcons.LOGO)
                            resultSet.addElement(builder)
                        }
                    }
                }
            }
        )
        extend(
            CompletionType.BASIC,
            psiElement().andOr(FLAG_KEY_INT, FLAG_KEY_INT_DETAIL, FLAG_KEY_DOUBLE, FLAG_KEY_DOUBLE_DETAIL),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet
                ) {
                    val project = parameters.originalFile.project
                    val getFlags = project.service<FlagStore>()
                    for (flag in getFlags.flags.items) {
                        if (flag.kind == "multivariate" && flag.variations[0].value is Number) {
                            var builder: LookupElementBuilder = LookupElementBuilder.create(flag.key)
                                .withTypeText(flag.description)
                                .withIcon(LDIcons.LOGO)
                            resultSet.addElement(builder)
                        }
                    }
                }
            }
        )
    }
}
