package com.saamaka.dico.testeurs

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun EmailAccountDialog(
    account: EmailAccountController,
    language: UiLanguage,
    onDismiss: () -> Unit,
    onManageSubscription: () -> Unit
) {
    fun copy(key: AccountText) = key.text(language)
    var email by remember { mutableStateOf(account.session.email) }
    // Passwords deliberately stay in memory only, never in saved state or preferences.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var registration by remember { mutableStateOf(true) }
    var deleting by remember { mutableStateOf(false) }
    val formScroll = rememberScrollState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    fun prepareSubmission() {
        keyboard?.hide()
        focusManager.clearFocus()
    }
    val signedIn = account.session.uid != null
    val validEmail = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    LaunchedEffect(account.session.uid) {
        password = ""
        confirmation = ""
        deleting = false
        if (signedIn) email = account.session.email
    }

    LaunchedEffect(account.session.uid, account.message) {
        if (account.message != null || signedIn) formScroll.scrollTo(0)
    }

    AlertDialog(
        onDismissRequest = { if (!account.busy) onDismiss() },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(copy(if (signedIn) AccountText.ACCOUNT else if (registration) AccountText.CREATE else AccountText.SIGN_IN))
                if (account.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(copy(AccountText.PROCESSING), style = MaterialTheme.typography.bodyMedium)
                }
                account.message?.let {
                    Text(
                        copy(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (account.messageIsError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(formScroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (signedIn) {
                    Text(account.session.email)
                    Text(copy(if (account.session.verified) AccountText.VERIFIED else AccountText.VERIFY_REQUIRED))
                    Text(copy(AccountText.LOCAL_DATA), style = MaterialTheme.typography.bodySmall)
                    if (!account.session.verified) {
                        if (account.verificationEmailFailed) {
                            Text(copy(AccountText.VERIFICATION_SEND_FAILED), color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = { account.refresh() },
                            enabled = !account.busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(copy(AccountText.CHECK_VERIFICATION)) }
                        OutlinedButton(
                            onClick = { account.sendVerification() },
                            enabled = !account.busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(copy(AccountText.RESEND)) }
                    }
                    OutlinedButton(
                        onClick = { account.signOut() },
                        enabled = !account.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(copy(AccountText.SIGN_OUT)) }
                    if (deleting) {
                        Text(copy(AccountText.DELETE_NOTICE))
                        TextButton(onClick = onManageSubscription, enabled = !account.busy) {
                            Text(copy(AccountText.SUBSCRIPTIONS))
                        }
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(copy(AccountText.CURRENT_PASSWORD)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            enabled = !account.busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { prepareSubmission(); account.deleteAccount(password) },
                            enabled = !account.busy && password.isNotEmpty()
                        ) { Text(copy(AccountText.CONFIRM_DELETE)) }
                        TextButton(
                            onClick = { deleting = false; password = "" },
                            enabled = !account.busy
                        ) { Text(copy(AccountText.CANCEL)) }
                    } else {
                        TextButton(onClick = { deleting = true }, enabled = !account.busy) {
                            Text(copy(AccountText.DELETE))
                        }
                    }
                } else {
                    Text(copy(AccountText.BENEFIT))
                    Text(copy(AccountText.LOCAL_DATA), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(copy(AccountText.EMAIL)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        enabled = !account.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(copy(AccountText.PASSWORD)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !account.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (registration) {
                        Text(copy(AccountText.PASSWORD_HINT), style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = confirmation,
                            onValueChange = { confirmation = it },
                            label = { Text(copy(AccountText.CONFIRM_PASSWORD)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            enabled = !account.busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(copy(AccountText.DATA_USE), style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = {
                            prepareSubmission()
                            if (registration) account.register(email, password)
                            else account.signIn(email, password)
                            // Keep the input on failure; the session effect clears passwords on success.
                        },
                        enabled = !account.busy && validEmail &&
                            (if (registration) password.length >= 6 && password == confirmation else password.isNotEmpty()),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(copy(if (registration) AccountText.CREATE else AccountText.SIGN_IN)) }
                    TextButton(
                        onClick = { registration = !registration; password = ""; confirmation = "" },
                        enabled = !account.busy
                    ) { Text(copy(if (registration) AccountText.HAVE_ACCOUNT else AccountText.CREATE)) }
                    TextButton(
                        onClick = { prepareSubmission(); account.resetPassword(email) },
                        enabled = !account.busy && validEmail
                    ) { Text(copy(AccountText.FORGOT)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !account.busy) {
                Text(copy(if (signedIn) AccountText.CLOSE else AccountText.LATER))
            }
        }
    )
}
