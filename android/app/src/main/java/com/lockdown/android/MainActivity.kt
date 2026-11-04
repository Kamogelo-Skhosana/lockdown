package com.lockdown.android

import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            val preferences = remember { getSharedPreferences("appearance", Context.MODE_PRIVATE) }
            var appearance by remember {
                mutableStateOf(Appearance.entries.find { it.name == preferences.getString("theme", null) } ?: Appearance.System)
            }
            val dark = when (appearance) {
                Appearance.System -> isSystemInDarkTheme()
                Appearance.Light -> false
                Appearance.Dark -> true
            }
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                EmergencyFlow(appearance) { selection ->
                    appearance = selection
                    preferences.edit().putString("theme", selection.name).apply()
                }
            }
        }
    }
}

private enum class Screen { Splash, Welcome, Privacy, Home, Incident, Description, Consent, Analyzing, AssessmentReview, AccountAccess, Checklist, History, Settings, Help }

private enum class Appearance { System, Light, Dark }

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
private fun EmergencyFlow(appearance: Appearance, onAppearanceChange: (Appearance) -> Unit) {
    val context = LocalContext.current
    val storage = remember(context) {
        context.getSharedPreferences("recovery", Context.MODE_PRIVATE)
    }
    var saveProgress by remember { mutableStateOf(storage.getBoolean("enabled", false)) }
    var completed by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var draft by remember { mutableStateOf("") }
    var assessment by remember { mutableStateOf<Assessment?>(null) }
    var assessmentFailed by remember { mutableStateOf(false) }
    var draftError by remember { mutableStateOf(false) }
    var savedPlans by remember { mutableStateOf(Incident.entries.filter { storage.contains(it.name) }) }
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
            Screen.Incident, Screen.History, Screen.Settings -> Screen.Home
            Screen.Help -> Screen.Settings
            Screen.Description -> Screen.Incident
            Screen.Consent -> Screen.Description
            Screen.Analyzing, Screen.AssessmentReview -> Screen.Consent
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
        Screen.Analyzing -> "Checking your report"
        Screen.AssessmentReview -> "Assessment review"
        Screen.AccountAccess -> "Can you still sign in?"
        Screen.Checklist -> "Secure what you can"
        Screen.History -> "Recovery history"
        Screen.Settings -> "Settings"
        Screen.Help -> "Help"
    }

    LaunchedEffect(screen) {
        if (screen == Screen.Analyzing) {
            val result = withContext(Dispatchers.IO) { requestAssessment(draft) }
            assessment = result
            assessmentFailed = result == null
            screen = if (result == null) Screen.Consent else Screen.AssessmentReview
        }
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
                    savedPlans = emptyList()
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
                        SecondaryAction("Settings") { screen = Screen.Settings }
                        SecondaryAction("Recovery history") {
                            savedPlans = Incident.entries.filter { storage.contains(it.name) }
                            screen = Screen.History
                        }
                    }
                    Screen.Settings -> {
                        Text("Appearance", Modifier.semantics { heading() })
                        Appearance.entries.forEach { option ->
                            FilterChip(
                                selected = appearance == option,
                                onClick = { onAppearanceChange(option) },
                                label = { Text(option.name) },
                            )
                        }
                        Text("Recovery privacy", Modifier.semantics { heading() })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = saveProgress,
                                onCheckedChange = { enabled ->
                                    saveProgress = enabled
                                    storage.edit().putBoolean("enabled", enabled).apply()
                                },
                            )
                            Text("Save recovery progress on this device")
                        }
                        Text("Turning off saving keeps existing plans in Recovery history.")
                        SecondaryAction("Manage recovery history") {
                            savedPlans = Incident.entries.filter { storage.contains(it.name) }
                            screen = Screen.History
                        }
                        SecondaryAction("Help") { screen = Screen.Help }
                    }
                    Screen.Help -> {
                        Text("Can’t access an account?", Modifier.semantics { heading() })
                        Text("Use the account provider’s official recovery page from a trusted device.")
                        Text("Using Lockdown offline", Modifier.semantics { heading() })
                        Text("Open the offline checklist from Home to see protective steps without internet.")
                        Text("What data is shared?", Modifier.semantics { heading() })
                        Text("Offline guidance stays on this device. Recovery progress is saved only if you enable saving.")
                    }
                    Screen.History -> {
                        if (savedPlans.isEmpty()) Text("No saved recovery plans.")
                        savedPlans.forEach { plan ->
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
                        if (BuildConfig.ASSESSMENT_URL.isBlank()) {
                            Text("Online analysis is unavailable in this build.")
                        } else {
                            Text("Send your description to Lockdown for analysis by Meta. Remove sensitive information before submitting.")
                            if (assessmentFailed) Text("Online analysis is unavailable. Try again or continue offline.")
                        }
                        Text("You can get protective steps on this device without sharing incident details.")
                    }
                    Screen.Analyzing -> CircularProgressIndicator()
                    Screen.AssessmentReview -> {
                        assessment?.let { result ->
                            Text("Provisional risk: ${result.risk}")
                            result.actions.forEach { Text(it) }
                        }
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
                    Screen.Incident, Screen.Settings -> SecondaryAction("Back to Home", ::goBack)
                    Screen.Help -> SecondaryAction("Back to Settings", ::goBack)
                    Screen.History -> {
                        if (savedPlans.isNotEmpty()) {
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
                        if (BuildConfig.ASSESSMENT_URL.isNotBlank() && draft.isNotBlank()) {
                            Action("Agree and analyse") { screen = Screen.Analyzing }
                            SecondaryAction("Continue offline", ::continueOffline)
                        } else {
                            Action("Continue offline", ::continueOffline)
                        }
                        SecondaryAction("Back to description", ::goBack)
                    }
                    Screen.Analyzing -> SecondaryAction("Cancel analysis", ::goBack)
                    Screen.AssessmentReview -> Action("Open recovery steps", ::continueOffline)
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
