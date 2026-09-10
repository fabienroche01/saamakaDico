package com.saamaka.dico.testeurs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saamaka.dico.testeurs.database.DictionaryDatabase
import com.saamaka.dico.testeurs.repository.CorrectionStore

class AudioAdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var accessGranted by remember { mutableStateOf(false) }
                    var code by remember { mutableStateOf("") }
                    var invalidCode by remember { mutableStateOf(false) }

                    if (!accessGranted) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Admin audio Saamaka",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Saisis le code testeur pour accéder à l'enregistrement en chaîne.")
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = code,
                                onValueChange = {
                                    code = it
                                    invalidCode = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Code") },
                                singleLine = true,
                                isError = invalidCode
                            )
                            if (invalidCode) {
                                Spacer(Modifier.height(6.dp))
                                Text("Code incorrect")
                            }
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    if (TesterAccess.isValid(code)) {
                                        accessGranted = true
                                    } else {
                                        invalidCode = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ouvrir le mode Admin audio")
                            }
                        }
                    } else {
                        val context = this@AudioAdminActivity
                        val database = remember { DictionaryDatabase(context) }
                        val validationStore = remember { ValidationStore(context) }
                        val correctionStore = remember { CorrectionStore(context) }
                        val strings = remember { stringsFor(UiLanguage.FRENCH) }
                        val audioStore = remember { AudioStore(context, strings) }
                        val entries = remember { database.allEntries() }
                        val locallyValidatedIds = remember { validationStore.ids() }
                        val testerName = remember {
                            correctionStore.testerName().ifBlank { "Admin" }
                        }

                        AudioAdminScreen(
                            entries = entries,
                            locallyValidatedIds = locallyValidatedIds,
                            audioStore = audioStore,
                            testerName = testerName,
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }
}
