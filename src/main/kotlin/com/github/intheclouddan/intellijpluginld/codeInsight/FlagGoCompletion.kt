package com.github.intheclouddan.intellijpluginld.codeInsight

import com.github.intheclouddan.intellijpluginld.FlagStore
import com.github.intheclouddan.intellijpluginld.LDIcons
import com.goide.GoTypes.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext


@Service
class GoCompletionContributor : CompletionContributor() {

//    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
//        val caretOffset: Int = parameters.editor.getCaretModel().getOffset()
//        val elem = parameters.originalFile.findElementAt(caretOffset)
//        println(elem)
//        println(elem!!.parent)
//        println(elem!!.parent.parent)
//        println(elem!!.parent.parent.parent)
//        println(elem!!.parent.parent.parent.parent)
//
//        super.fillCompletionVariants(parameters, result)
//    }
//
//    private fun parseValue(json: String): LDValue? {
//        val gson = Gson()
//        return gson.fromJson(json, LDValue::class.java)
//    }

    companion object {
        val FLAG_KEY_BOOL = psiElement()
                .withParent(psiElement(STRING_LITERAL)
                        .withParent(psiElement(ARGUMENT_LIST)
                        .withParent(psiElement(CALL_EXPR)
                                .withChild(psiElement(REFERENCE_EXPRESSION)))))
        val FLAG_KEY_CONST = psiElement()
                .withParent(psiElement(STRING_LITERAL)
                        .withParent(psiElement(CONST_SPEC)))
    }

    init {
        extend(CompletionType.BASIC,
                FLAG_KEY_BOOL,
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
                FLAG_KEY_CONST,
                object : CompletionProvider<CompletionParameters>() {
                    override fun addCompletions(parameters: CompletionParameters,
                                                context: ProcessingContext,
                                                resultSet: CompletionResultSet) {
                        val project = parameters.originalFile.project
                        val getFlags = project.service<FlagStore>()
                        for (flag in getFlags.flags.items) {
                                var builder: LookupElementBuilder = LookupElementBuilder.create(flag.key)
                                        .withTypeText(flag.description)
                                        .withIcon(LDIcons.FLAG)
                                resultSet.addElement(builder)
                        }
                    }
                }
        )
    }
}