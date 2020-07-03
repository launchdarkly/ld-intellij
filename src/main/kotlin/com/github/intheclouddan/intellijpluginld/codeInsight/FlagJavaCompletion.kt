package com.github.intheclouddan.intellijpluginld.codeInsight

import com.github.intheclouddan.intellijpluginld.FlagStore
import com.github.intheclouddan.intellijpluginld.LDIcons
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyConfig
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.PsiJavaPatterns.psiLiteral
import com.intellij.patterns.PsiJavaPatterns.psiMethod
import com.intellij.psi.JavaTokenType
import com.intellij.util.ProcessingContext

@Service
class JavaCompletionContributor : CompletionContributor() {

//    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
//        val caretOffset: Int = parameters.editor.getCaretModel().getOffset()
//        val elem = parameters.originalFile.findElementAt(caretOffset)
//        println(elem)
//        super.fillCompletionVariants(parameters, result)
//    }
//
//    private fun parseValue(json: String): LDValue? {
//        val gson = Gson()
//        return gson.fromJson(json, LDValue::class.java)
//    }

    companion object {
        val FLAG_KEY_BOOL = psiElement(JavaTokenType.STRING_LITERAL).withParent(
                psiLiteral().methodCallParameter(
                        0,
                        psiMethod()
                                .withName("boolVariation")
                                .definedInClass("com.launchdarkly.sdk.server.LDClient")))
        val FLAG_KEY_BOOL_DETAILS = psiElement(JavaTokenType.STRING_LITERAL).withParent(
                psiLiteral().methodCallParameter(
                        0,
                        psiMethod()
                                .withName("boolVariationDetail")
                                .definedInClass("com.launchdarkly.sdk.server.LDClient")))
        val FLAG_KEY_STRING = psiElement(JavaTokenType.STRING_LITERAL).withParent(
                psiLiteral().methodCallParameter(
                        0,
                        psiMethod()
                                .withName("stringVariation")
                                .definedInClass("com.launchdarkly.sdk.server.LDClient")))
        val FLAG_KEY_STRING_DETAILS = psiElement(JavaTokenType.STRING_LITERAL).withParent(
                psiLiteral().methodCallParameter(
                        0,
                        psiMethod()
                                .withName("stringVariationDetail")
                                .definedInClass("com.launchdarkly.sdk.server.LDClient")))
        val FLAG_KEY_JSON = psiElement(JavaTokenType.STRING_LITERAL).withParent(
                psiLiteral().methodCallParameter(
                        0,
                        psiMethod()
                                .withName("jsonValueVariation")
                                .definedInClass("com.launchdarkly.sdk.server.LDClient")))
        val FLAG_KEY_JSON_DETAILS = psiElement(JavaTokenType.STRING_LITERAL).withParent(
                psiLiteral().methodCallParameter(
                        0,
                        psiMethod()
                                .withName("jsonValueVariationDetail")
                                .definedInClass("com.launchdarkly.sdk.server.LDClient")))
        val FLAG_DEFAULT = psiElement().withParent(
                PsiJavaPatterns.psiElement().methodCallParameter(
                        2,
                        psiMethod()
                                .withName("boolVariation")
                                .definedInClass("com.launchdarkly.sdk.server.LDClient")))
    }

    init {
        extend(CompletionType.BASIC,
                psiElement().andOr(FLAG_KEY_BOOL,FLAG_KEY_BOOL_DETAILS),
                object : CompletionProvider<CompletionParameters>() {
                    override fun addCompletions(parameters: CompletionParameters,
                                                context: ProcessingContext,
                                                resultSet: CompletionResultSet) {
                        val project = parameters.originalFile.project
                        val getFlags = project.service<FlagStore>()
                        for (flag in getFlags.flags.items) {
                            if (flag.kind == "boolean") {
                                var builder: LookupElementBuilder = LookupElementBuilder.create(flag.key)
                                        .withTypeText(flag.description)
                                        .withIcon(LDIcons.FLAG)
                                resultSet.addElement(builder)
                            }
                        }
                    }
                }
        )
        extend(CompletionType.BASIC,
                psiElement().andOr(FLAG_KEY_STRING, FLAG_KEY_STRING_DETAILS),
                object : CompletionProvider<CompletionParameters>() {
                    override fun addCompletions(parameters: CompletionParameters,
                                                context: ProcessingContext,
                                                resultSet: CompletionResultSet) {
                        val project = parameters.originalFile.project
                        val getFlags = project.service<FlagStore>()
                        for (flag in getFlags.flags.items) {
                            if (flag.kind == "multivariate" && flag.variations[0].value is String) {
                                var builder: LookupElementBuilder = LookupElementBuilder.create(flag.key)
                                        .withTypeText(flag.description)
                                        .withIcon(LDIcons.FLAG)
                                resultSet.addElement(builder)
                            }
                        }
                    }
                }
        )
//        extend(CompletionType.BASIC,
//                psiElement().andOr(FLAG_KEY_JSON, FLAG_KEY_JSON_DETAILS),
//                object : CompletionProvider<CompletionParameters>() {
//                    override fun addCompletions(parameters: CompletionParameters,
//                                                context: ProcessingContext,
//                                                resultSet: CompletionResultSet) {
//                        val project = parameters.originalFile.project
//                        val getFlags = project.service<FlagStore>()
//                        for (flag in getFlags.flags.items) {
//                            try {
////                                println(parseValue(flag.variations[0].value as String))
//
//                                if (flag.kind == "multivariate" && flag.variations[0].value is String) {
//                                    val gson = Gson()
//                                    val blah = gson.fromJson(flag.variations[0].value as String, String)
//                                    var builder: LookupElementBuilder = LookupElementBuilder.create(flag.key)
//                                            .withTypeText(flag.description)
//                                            .withIcon(LDIcons.FLAG)
//                                    resultSet.addElement(builder)
//                                }
//                            } catch(err: Error) {
//                                continue
//                            }
//                        }
//                    }
//                }
//        )
        extend(CompletionType.BASIC,
                FLAG_DEFAULT,
                object : CompletionProvider<CompletionParameters>() {
                    override fun addCompletions(parameters: CompletionParameters,
                                                context: ProcessingContext,
                                                resultSet: CompletionResultSet) {
                        val project = parameters.originalFile.project
                        val settings = LaunchDarklyConfig.getInstance(project)
                        val getFlags = project.service<FlagStore>()
                        val caretOffset: Int = parameters.editor.getCaretModel().getOffset()
                        val elem = parameters.originalFile.findElementAt(caretOffset)
                        val parent = elem?.parent
                        parent?.let {
                            println(it)
                        }
                        val filteredFlag = getFlags.flags.items.filter { it.key == "navbar" }
                        val flag = filteredFlag[0]
                        resultSet.addElement(LookupElementBuilder.create("true").withIcon(LDIcons.OFF_VARIATION))
                    }

                }
        )
    }
}