package com.launchdarkly.intellij.codeInsight

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState

/**
 * Enables automatic completion inside of matching functions `featureKey` parameter for string literals (which is disabled by default)
 */
class JavaCompletionConfidence : CompletionConfidence() {
    override fun shouldSkipAutopopup(
        contextElement: PsiElement,
        psiFile: PsiFile,
        offset: Int
    ): ThreeState {
        // Return early if it's not a string literal
        if (contextElement == JavaTokenType.STRING_LITERAL) {
            return ThreeState.UNSURE
        }

        when {
            FLAG_KEY_BOOL.accepts(contextElement) -> return ThreeState.NO
            FLAG_KEY_BOOL_DETAIL.accepts(contextElement) -> return ThreeState.NO
            FLAG_KEY_STRING.accepts(contextElement) -> return ThreeState.NO
            FLAG_KEY_STRING_DETAIL.accepts(contextElement) -> return ThreeState.NO
            FLAG_KEY_JSON.accepts(contextElement) -> return ThreeState.NO
            FLAG_KEY_JSON_DETAIL.accepts(contextElement) -> return ThreeState.NO
            FLAG_KEY_INT.accepts(contextElement) -> return ThreeState.NO
            FLAG_KEY_INT_DETAIL.accepts(contextElement) -> return ThreeState.NO
            FLAG_KEY_DOUBLE.accepts(contextElement) -> return ThreeState.NO
            FLAG_KEY_DOUBLE_DETAIL.accepts(contextElement) -> return ThreeState.NO
        }

        return ThreeState.UNSURE
    }
}
