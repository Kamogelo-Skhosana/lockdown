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

private enum class Screen { Splash, Welcome, Privacy, Home, Incident, Consent, Checklist }

private enum class Incident(val label: String, val steps: List<String>) {
    Account("Account takeover", listOf(
        "On a trusted device, open the provider’s official app or website.",
        "If you can sign in, change your password and remove unfamiliar sessions.",
        "If you can’t sign in, use the provider’s official account recovery process.",
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
    var screen by rememberSaveable { mutableStateOf(Screen.Splash) }
    var incident by rememberSaveable { mutableStateOf(Incident.Unknown) }
    var checklistBack by rememberSaveable { mutableStateOf(Screen.Incident) }

    fun goBack() {
        screen = when (screen) {
            Screen.Splash -> Screen.Splash
            Screen.Welcome, Screen.Home -> Screen.Splash
            Screen.Privacy -> Screen.Welcome
            Screen.Incident -> Screen.Home
            Screen.Consent -> Screen.Incident
            Screen.Checklist -> checklistBack
        }
    }

    fun openChecklist(selection: Incident) {
        checklistBack = screen
        incident = selection
        completed = ArrayList(
            if (saveProgress) storage.getStringSet(selection.name, emptySet()).orEmpty()
            else emptySet(),
        )
        screen = Screen.Checklist
    }

    BackHandler(enabled = screen != Screen.Splash, onBack = ::goBack)

    val title = when (screen) {
        Screen.Splash -> "Lockdown"
        Screen.Welcome -> "Take back control"
        Screen.Privacy -> "You control your data"
        Screen.Home -> "What happened?"
        Screen.Incident -> "Choose an incident"
        Screen.Consent -> "Online analysis"
        Screen.Checklist -> "Secure what you can"
    }

    Scaffold { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp),
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
                    Screen.Home -> Text("Start with the incident. We’ll guide you through the first steps.")
                    Screen.Incident -> Incident.entries.forEach { option ->
                        OutlinedButton(
                            onClick = {
                                incident = option
                                screen = Screen.Consent
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) { Text(option.label) }
                    }
                    Screen.Consent -> {
                        Text("Online analysis is optional and is currently unavailable.")
                        Text("You can get protective steps on this device without sharing incident details.")
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
                            OutlinedButton(
                                onClick = {
                                    val updated = completed.toMutableSet()
                                    if (checked) updated.remove(id) else updated.add(id)
                                    completed = ArrayList(updated)
                                    if (saveProgress) {
                                        storage.edit().putStringSet(incident.name, updated).apply()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            ) {
                                val status = if (checked) "Complete" else "To do"
                                Text("${index + 1}. $step\n$status")
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
                    Screen.Consent -> {
                        Action("Continue offline") { openChecklist(incident) }
                        SecondaryAction("Choose another incident", ::goBack)
                    }
                    Screen.Checklist -> {
                        Action("Return to Home") { screen = Screen.Home }
                        if (checklistBack == Screen.Incident) {
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
