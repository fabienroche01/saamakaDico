package com.saamaka.dico.testeurs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

internal data class EmailAccountSession(
    val uid: String? = null,
    val email: String = "",
    val verified: Boolean = false
)

internal class EmailAccountController(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    var session by mutableStateOf(readSession())
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<AccountText?>(null)
        private set

    private val listener = FirebaseAuth.AuthStateListener { session = readSession() }

    private fun readSession(): EmailAccountSession {
        val user = auth.currentUser ?: return EmailAccountSession()
        return EmailAccountSession(user.uid, user.email.orEmpty(), user.isEmailVerified)
    }

    fun start() {
        auth.addAuthStateListener(listener)
        refresh()
    }

    fun stop() = auth.removeAuthStateListener(listener)

    fun setLanguage(language: UiLanguage) {
        auth.setLanguageCode(when (language) {
            UiLanguage.ENGLISH -> "en"
            UiLanguage.DUTCH -> "nl"
            else -> "fr"
        })
    }

    private fun report(error: Exception?) {
        message = when {
            error is FirebaseNetworkException -> AccountText.NETWORK_ERROR
            error is FirebaseTooManyRequestsException -> AccountText.TOO_MANY_REQUESTS
            error is FirebaseAuthException -> when (error.errorCode) {
                "ERROR_WEAK_PASSWORD" -> AccountText.WEAK_PASSWORD
                "ERROR_INVALID_EMAIL" -> AccountText.INVALID_EMAIL
                "ERROR_EMAIL_ALREADY_IN_USE" -> AccountText.EXISTING_ACCOUNT
                "ERROR_REQUIRES_RECENT_LOGIN" -> AccountText.REAUTH_REQUIRED
                "ERROR_OPERATION_NOT_ALLOWED", "ERROR_CONFIGURATION_NOT_FOUND" -> AccountText.UNAVAILABLE
                "ERROR_USER_DISABLED", "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD",
                "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" -> AccountText.INVALID_LOGIN
                else -> AccountText.GENERIC_ERROR
            }
            else -> AccountText.GENERIC_ERROR
        }
    }

    // SDK tasks deliver these callbacks on Android's main thread.
    private fun <T> perform(task: () -> Task<T>, success: () -> Unit = {}) {
        if (busy) return
        busy = true
        message = null
        try {
            task().addOnCompleteListener { result ->
                busy = false
                session = readSession()
                if (result.isSuccessful) success() else report(result.exception)
            }
        } catch (error: Exception) {
            busy = false
            report(error)
        }
    }

    fun register(email: String, password: String) {
        perform({ auth.createUserWithEmailAndPassword(email.trim(), password) }) {
            sendVerification()
        }
    }

    fun signIn(email: String, password: String) {
        perform({ auth.signInWithEmailAndPassword(email.trim(), password) }) {
            message = if (session.verified) AccountText.SIGNED_IN else AccountText.VERIFY_REQUIRED
        }
    }

    fun sendVerification() {
        val user = auth.currentUser ?: return
        perform({ user.sendEmailVerification() }) { message = AccountText.CHECK_EMAIL }
    }

    fun refresh() {
        val user = auth.currentUser ?: return
        perform({ user.reload() }) {
            message = if (session.verified) AccountText.VERIFIED else AccountText.VERIFY_REQUIRED
        }
    }

    fun resetPassword(email: String) {
        perform({ auth.sendPasswordResetEmail(email.trim()) }) {
            message = AccountText.RESET_SENT
        }
    }

    fun signOut() {
        if (busy) return
        auth.signOut()
        session = readSession()
        message = null
    }

    fun deleteAccount(password: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        perform({
            user.reauthenticate(EmailAuthProvider.getCredential(email, password))
                .continueWithTask { result ->
                    if (!result.isSuccessful) {
                        throw result.exception ?: IllegalStateException("Reauthentication failed")
                    }
                    user.delete()
                }
        }) {
            auth.signOut()
            session = readSession()
            message = AccountText.DELETED
        }
    }
}
