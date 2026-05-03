package com.example.LyricBox

import android.content.ComponentName
import android.content.Context
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitPairFilter
import androidx.window.embedding.SplitPairRule
import androidx.window.embedding.SplitRule
import androidx.window.embedding.SplitController

object MusicPlayerEmbeddingRules {
    private const val PLAYER_PREVIEW_RULE_TAG = "music_player_preview_split_rule"

    @Volatile
    private var isRegistered = false

    fun isSplitAvailable(context: Context): Boolean {
        return runCatching {
            SplitController.getInstance(context.applicationContext).splitSupportStatus ==
                SplitController.SplitSupportStatus.SPLIT_AVAILABLE
        }.getOrDefault(false)
    }

    fun ensureRegistered(context: Context) {
        if (isRegistered) return

        synchronized(this) {
            if (isRegistered) return

            val appContext = context.applicationContext
            val ruleController = RuleController.getInstance(appContext)

            val splitFilter = SplitPairFilter(
                ComponentName(appContext, MusicPlayerActivity::class.java),
                ComponentName(appContext, LyricPreviewActivity::class.java),
                null
            )
            val splitAttributes = SplitAttributes.Builder()
                .setSplitType(SplitAttributes.SplitType.ratio(0.5f))
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .build()

            val splitRule = SplitPairRule.Builder(setOf(splitFilter))
                .setTag(PLAYER_PREVIEW_RULE_TAG)
                .setDefaultSplitAttributes(splitAttributes)
                .setMinWidthDp(840)
                .setMinSmallestWidthDp(600)
                .setFinishPrimaryWithSecondary(SplitRule.FinishBehavior.NEVER)
                .setFinishSecondaryWithPrimary(SplitRule.FinishBehavior.ALWAYS)
                .setClearTop(true)
                .build()

            ruleController.addRule(splitRule)

            isRegistered = true
        }
    }
}
