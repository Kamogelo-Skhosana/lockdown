package com.lockdown.android

import android.content.Context
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colors) { EmergencyFlow() }
        }
    }
}

private enum class Screen { Splash, Welcome, Privacy, Home, Incident, Description, Consent, AccountAccess, Checklist, History }

private enum class Incident(val label: String, val steps: List<String>) {
    Account("Account takeover", listOf(
        "On a trusted device, open the provider’s official app or website.",
        "If you can sign in, change your password and remove unfamiliar sessions.",
        "If you can’t sign in, use the provider’s official account recovery process.",
    )),
    AccountWithAccess("Secure your account", listOf(
        "From a trusted device, open the provider’s official app or site and change your password.",
        "Review active sessions and sign out unfamiliar devices.",
        "Turn on two-step verification in the provider’s security settings.",
    )),
    AccountLockedOut("Recover account access", listOf(
        "From a trusted device, open the provider’s official account recovery page.",
        "Follow the provider’s recovery steps and secure the email used to recover the account.",
    )),
    Message("Suspicious message", listOf(
        "Leave the message open only if needed to report it. Avoid its links and attachments.",
        "Verify the request through an official app, website or a number you already trust.",
    )),
    Device("Lost device", listOf(
        "From a trusted device, use your provider’s official device-finding service to try to lock the device.",
        "If the device can’t be located, contact your mobile carrier to secure your SIM.",
    )),
    Fraud("Payment fraud", listOf(
        "Contact your bank using its official app or the number on your card or statement.",
        "Report the unfamiliar transaction and ask how to secure the account.",
    )),
    Unknown("I’m not sure", listOf(
        "Pause before opening links, sending money or sharing codes.",
        "Use a trusted device to check recent activity through the official provider.",
    )),
}

@Composable
private fun EmergencyFlow() {
    val context = LocalContext.current
    val storage = remember(context) {
        context.getSharedPreferences("recovery", Context.MODE_PRIVATE)
    }
    var saveProgress by remember { mutableStateOf(storage.getBoolean("enabled", false)) }
    var completed by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var draft by remember { mutableStateOf("") }
    var draftError by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var screen by rememberSaveable { mutableStateOf(Screen.Splash) }
    var incident by rememberSaveable { mutableStateOf(Incident.Unknown) }
    var checklistBack by rememberSaveable { mutableStateOf(Screen.Incident) }

    var accountAccessBack by rememberSaveable { mutableStateOf(Screen.Description) }

    fun goBack() {
        if (screen == Screen.Description && draft.isNotBlank()) {
            confirmDiscard = true
            return
        }
        screen = when (screen) {
            Screen.Splash -> Screen.Splash
            Screen.Welcome, Screen.Home -> Screen.Splash
            Screen.Privacy -> Screen.Welcome
            Screen.Incident, Screen.History -> Screen.Home
            Screen.Description -> Screen.Incident
            Screen.Consent -> Screen.Description
            Screen.AccountAccess -> {
                incident = Incident.Account
                accountAccessBack
            }
            Screen.Checklist -> {
                if (checklistBack == Screen.AccountAccess) incident = Incident.Account
                checklistBack
            }
        }
    }

    fun openChecklist(selection: Incident, resume: Boolean = false) {
        checklistBack = screen
        incident = selection
        completed = ArrayList(
            if (resume || saveProgress) storage.getStringSet(selection.name, emptySet()).orEmpty()
            else emptySet(),
        )
        screen = Screen.Checklist
    }

    fun continueOffline() {
        if (incident == Incident.Account) {
            accountAccessBack = screen
            screen = Screen.AccountAccess
        } else {
            openChecklist(incident)
        }
    }

    BackHandler(enabled = screen != Screen.Splash, onBack = ::goBack)

    val title = when (screen) {
        Screen.Splash -> "Lockdown"
        Screen.Welcome -> "Take back control"
        Screen.Privacy -> "You control your data"
        Screen.Home -> "What happened?"
        Screen.Incident -> "Choose an incident"
        Screen.Description -> "Describe what happened"
        Screen.Consent -> "Online analysis"
        Screen.AccountAccess -> "Can you still sign in?"
        Screen.Checklist -> "Secure what you can"
        Screen.History -> "Recovery history"
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear recovery history?") },
            text = { Text("This removes all saved recovery progress from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    val editor = storage.edit()
                    Incident.entries.forEach { editor.remove(it.name) }
                    editor.apply()
                    completed = arrayListOf()
                    confirmClearHistory = false
                }) { Text("Clear history") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearHistory = false }) { Text("Cancel") }
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this draft?") },
            text = { Text("Your description will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    draft = ""
                    draftError = false
                    confirmDiscard = false
                    screen = Screen.Incident
                }) { Text("Discard draft") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") }
            },
        )
    }

    Scaffold { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).imePadding().padding(horizontal = 20.dp),
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
                when (screen) {
                    Screen.Splash -> Text("Digital incident response")
                    Screen.Welcome -> Text("Identify what happened and take the first protective step.")
                    Screen.Privacy -> Text("The offline checklist works on this device without an account.")
                    Screen.Home -> {
                        Text("Start with the incident. We’ll guide you through the first steps.")
                        SecondaryAction("Recovery history") { screen = Screen.History }
                    }
                    Screen.History -> {
                        val saved = Incident.entries.filter { storage.contains(it.name) }
                        if (saved.isEmpty()) Text("No saved recovery plans.")
                        saved.forEach { plan ->
                            val steps = storage.getStringSet(plan.name, emptySet()).orEmpty()
                            val count = plan.steps.indices.count { it.toString() in steps }
                            SecondaryAction("${plan.label} · $count of ${plan.steps.size} complete") {
                                openChecklist(plan, resume = true)
                            }
                        }
                    }
                    Screen.Incident -> Incident.entries.filter {
                        it != Incident.AccountWithAccess && it != Incident.AccountLockedOut
                    }.forEach { option ->
                        OutlinedButton(
                            onClick = {
                                incident = option
                                screen = Screen.Description
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) { Text(option.label) }
                    }
                    Screen.Description -> {
                        Text("Tell us what you noticed and when it started.")
                        Text("Leave out passwords, verification codes and payment details.")
                        OutlinedTextField(
                            value = draft,
                            onValueChange = {
                                draft = it
                                draftError = false
                            },
                            label = { Text("Incident description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            isError = draftError,
                            supportingText = {
                                if (draftError) Text("Enter a description of up to 4,096 bytes.")
                            },
                        )
                    }
                    Screen.Consent -> {
                        Text("Online analysis is optional and is currently unavailable.")
                        Text("You can get protective steps on this device without sharing incident details.")
                    }
                    Screen.AccountAccess -> {
                        Text("Choose the option that matches what you can do from a trusted device.")
                        SecondaryAction("Yes, I can sign in") { openChecklist(Incident.AccountWithAccess) }
                        SecondaryAction("No, I’m locked out") { openChecklist(Incident.AccountLockedOut) }
                    }
                    Screen.Checklist -> {
                        Text(incident.label, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        val count = incident.steps.indices.count { it.toString() in completed }
                        Text("$count of ${incident.steps.size} steps complete")
                        if (count == incident.steps.size) {
                            Text("Recovery steps complete", Modifier.semantics { heading() })
                        }
                        incident.steps.forEachIndexed { index, step ->
                            val id = index.toString()
                            val checked = id in completed
                            OutlinedCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                        .toggleable(
                                            value = checked,
                                            role = Role.Checkbox,
                                            onValueChange = { isComplete ->
                                                val updated = completed.toMutableSet()
                                                if (isComplete) updated.add(id) else updated.remove(id)
                                                completed = ArrayList(updated)
                                                if (saveProgress) {
                                                    storage.edit().putStringSet(incident.name, updated).apply()
                                                }
                                            },
                                        ).padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = null)
                                    Column(Modifier.weight(1f)) {
                                        Text(if (checked) "Completed" else "To do", style = MaterialTheme.typography.labelLarge)
                                        Text("${index + 1}. $step")
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = saveProgress,
                                onCheckedChange = { enabled ->
                                    saveProgress = enabled
                                    val editor = storage.edit().putBoolean("enabled", enabled)
                                    if (enabled) editor.putStringSet(incident.name, completed.toSet())
                                    editor.apply()
                                },
                            )
                            Text("Save recovery progress on this device")
                        }
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (screen) {
                    Screen.Splash -> {
                        Action("Get started") { screen = Screen.Welcome }
                        SecondaryAction("Get emergency help") { screen = Screen.Home }
                    }
                    Screen.Welcome -> {
                        Action("Continue") { screen = Screen.Privacy }
                        SecondaryAction("Skip to Home") { screen = Screen.Home }
                    }
                    Screen.Privacy -> Action("Go to Home") { screen = Screen.Home }
                    Screen.Home -> {
                        Action("Start an incident") { screen = Screen.Incident }
                        SecondaryAction("Open offline checklist") { openChecklist(Incident.Unknown) }
                    }
                    Screen.Incident -> SecondaryAction("Back to Home", ::goBack)
                    Screen.History -> {
                        if (Incident.entries.any { storage.contains(it.name) }) {
                            Action("Clear history") { confirmClearHistory = true }
                        }
                        SecondaryAction("Back to Home", ::goBack)
                    }
                    Screen.Description -> {
                        Action("Review online analysis") {
                            if (draft.isBlank() || draft.encodeToByteArray().size > 4096) {
                                draftError = true
                            } else {
                                screen = Screen.Consent
                            }
                        }
                        SecondaryAction("Continue offline", ::continueOffline)
                    }
                    Screen.Consent -> {
                        Action("Continue offline", ::continueOffline)
                        SecondaryAction("Back to description", ::goBack)
                    }
                    Screen.AccountAccess -> {
                        Action("I’m not sure") { openChecklist(Incident.Account) }
                        SecondaryAction("Back", ::goBack)
                    }
                    Screen.Checklist -> {
                        Action("Return to Home") {
                            draft = ""
                            draftError = false
                            screen = Screen.Home
                        }
                        if (checklistBack == Screen.AccountAccess) {
                            SecondaryAction("Change account access answer", ::goBack)
                        } else if (checklistBack == Screen.Incident) {
                            SecondaryAction("Choose another incident", ::goBack)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Action(label: String, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(label) }
}

@Composable
private fun SecondaryAction(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(label) }
}
