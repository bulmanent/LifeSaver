package com.lifesaver

import android.app.Application
import com.lifesaver.data.database.AppDatabase
import com.lifesaver.data.repository.DocumentRepository

class LifeSaverApplication : Application() {

    val database by lazy { AppDatabase.getInstance(this) }

    val repository by lazy {
        DocumentRepository(
            database.documentGroupDao(),
            database.documentPageDao()
        )
    }
}
