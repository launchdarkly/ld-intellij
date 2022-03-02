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
        if (contextElement == JavaTokenType.STRING_LITERAL) {
            return ThreeState.UNSURE
        }
        if (FLAG_KEY_BOOL.accepts(contextElement)) {
            return ThreeState.NO
        }
        if (FLAG_KEY_BOOL.accepts(contextElement)) {
            return ThreeState.NO
        }

        return ThreeState.UNSURE
    }
}