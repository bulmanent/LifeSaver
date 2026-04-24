package com.lifesaver.data.preferences

import android.content.Context

class AppPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val sheetsId: String?
        get() = prefs.getString(KEY_SHEETS_ID, null)?.trim()?.takeIf { it.isNotEmpty() }

    val rootFolderId: String?
        get() = prefs.getString(KEY_ROOT_FOLDER_ID, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun isBackendConfigured(): Boolean = !sheetsId.isNullOrBlank() && !rootFolderId.isNullOrBlank()

    fun saveBackendConfig(sheetsId: String, rootFolderId: String) {
        prefs.edit()
            .putString(KEY_SHEETS_ID, sheetsId.trim())
            .putString(KEY_ROOT_FOLDER_ID, rootFolderId.trim())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "lifesaver_prefs"
        private const val KEY_SHEETS_ID = "sheets_id"
        private const val KEY_ROOT_FOLDER_ID = "root_folder_id"
    }
}
