# Android route map

Route names below are implementation identifiers derived from the reviewed handoff. Individual Figma frame IDs still need linking. The prototype uses scripted sample data.

| From | User action | Destination | Data effect |
| --- | --- | --- | --- |
| Splash | Get started | Welcome → Privacy introduction → Home | Local navigation |
| Splash | Get emergency help | Emergency Home | Local navigation |
| Home | Start an incident | Incident category → Description | Create an in-memory draft |
| Description | Analyse securely | Analysis → optional account-access question → Assessment | Redact and submit only after explicit user action and consent |
| Description | Use manual checklist | Offline checklist | Read bundled guidance |
| Analysis | Cancel, retry or continue manually | Description, Analysis or Offline checklist | Keep the draft on cancel or error. Ignore results from cancelled requests. |
| Assessment | See urgent actions | Urgent actions | Select approved actions for the account-access branch |
| Urgent actions | Open recovery plan | Checklist | Build plan from approved actions |
| Checklist | Open task or mark complete | Task details or progress | Save completed task IDs when local history is enabled |
| Home | Open active recovery | Checklist | Read saved progress when available |
| Breach | Check for exposure | Loading → Results, No match, Offline or Provider error | Validate email before requesting. Cancel returns to the email form. |
| Results | View exposure details | Exposure detail | Read the existing lookup response |
| History | Open saved incident | Recovery checklist | Read local history |
| Settings | Appearance, Privacy controls or Help | Selected settings page | Store theme and privacy settings locally |
| Privacy controls | Turn off saving or clear history | Saving-off state or confirmation → cleared state | Turning off saving retains records. Confirmed clearing deletes records and progress. |

Use runtime system and keyboard insets. Keep primary actions outside the scrollable content. Verify both themes, TalkBack, font scaling, RTL and Android Back. System Back from a dirty draft prompts before discarding it.
