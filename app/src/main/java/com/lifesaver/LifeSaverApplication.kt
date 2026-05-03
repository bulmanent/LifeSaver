package com.lifesaver

import android.app.Application
import com.lifesaver.auth.GoogleAuthManager
import com.lifesaver.data.preferences.AppPreferences
import com.lifesaver.data.remote.DriveImageRegistry
import com.lifesaver.data.remote.GmailImportService
import com.lifesaver.data.remote.GoogleSheetsDriveService
import com.lifesaver.data.repository.DocumentRepository

class LifeSaverApplication : Application() {

    lateinit var preferences: AppPreferences
        private set

    lateinit var authManager: GoogleAuthManager
        private set

    lateinit var repository: DocumentRepository
        private set

    override fun onCreate() {
        super.onCreate()

        preferences = AppPreferences(this)
        authManager = GoogleAuthManager(this)

        val service = GoogleSheetsDriveService(
            context = this,
            authManager = authManager,
            preferences = preferences
        )
        val gmailService = GmailImportService(
            context = this,
            authManager = authManager
        )

        repository = DocumentRepository(
            authManager = authManager,
            preferences = preferences,
            remoteService = service,
            gmailService = gmailService
        )

        DriveImageRegistry.register(this, service)
    }
}
