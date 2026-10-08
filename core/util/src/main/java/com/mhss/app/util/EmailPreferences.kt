package com.mhss.app.util

import android.content.Context

object EmailPreferences {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_RECIPIENT_EMAIL = "recipient_email"

    /** Hämtar sparad e-postadress, eller null om ingen sparad */
    fun getRecipientEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_RECIPIENT_EMAIL, null)
    }

    /** Sparar e-postadress så den finns kvar nästa gång */
    fun setRecipientEmail(context: Context, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_RECIPIENT_EMAIL, email)
            .apply()
    }
}

