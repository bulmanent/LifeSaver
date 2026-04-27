package com.lifesaver.auth

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential

class GoogleAuthManager(context: Context) {

    private val appContext = context.applicationContext

    private val requestedScopes = arrayOf(
        Scope(SPREADSHEETS_SCOPE),
        Scope(DRIVE_FILE_SCOPE)
    )

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(requestedScopes.first(), *requestedScopes.drop(1).toTypedArray())
        .build()

    fun signInIntent(): Intent = GoogleSignIn.getClient(appContext, signInOptions).signInIntent

    fun handleSignInResult(data: Intent?): GoogleSignInAccount {
        return GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
    }

    fun describeSignInFailure(error: Throwable): String {
        return if (error is ApiException) {
            val codeName = CommonStatusCodes.getStatusCodeString(error.statusCode)
            "Google sign-in failed: $codeName (${error.statusCode})"
        } else {
            error.message ?: "Google sign-in failed"
        }
    }

    fun currentAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(appContext)

    fun currentEmail(): String? = currentAccount()?.email

    fun isSignedInWithRequiredScopes(): Boolean {
        val account = currentAccount() ?: return false
        return GoogleSignIn.hasPermissions(account, *requestedScopes)
    }

    fun signOut() {
        GoogleSignIn.getClient(appContext, signInOptions).signOut()
    }

    fun requireGoogleAccount(): Account {
        val googleAccount = currentAccount()
            ?: throw IllegalStateException("Google account not connected")
        return googleAccount.account
            ?: throw IllegalStateException("Google account details unavailable")
    }

    fun requireCredential(): GoogleAccountCredential {
        return GoogleAccountCredential.usingOAuth2(
            appContext,
            listOf(SPREADSHEETS_SCOPE, DRIVE_FILE_SCOPE)
        ).apply {
            selectedAccountName = requireGoogleAccount().name
        }
    }

    companion object {
        const val SPREADSHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}
