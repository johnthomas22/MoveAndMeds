package com.jaytt.moveandmeds.util

import android.content.Context

object StepCountHelper {
    private const val PREFS = "step_prefs"
    private const val KEY_BASELINE = "step_baseline"

    fun saveBaseline(context: Context, steps: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_BASELINE, steps).apply()
    }

    fun getBaseline(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_BASELINE, -1L)
}
