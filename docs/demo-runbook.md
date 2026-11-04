# Lockdown demo rehearsal

Use synthetic descriptions throughout. Record the APK commit and checksum, Android version and backend build before filming. Start with an APK configured for the test HTTPS backend and confirm the service is healthy. Use the signed release APK for the final rehearsal.

## Online assessment

1. Open **Get emergency help → Start an incident → Payment fraud**.
2. Enter “I noticed a card payment I did not make.” Open **Review online analysis** and choose **Agree and analyse**.
3. Show the provisional assessment and approved bank-contact action. Tap **Open recovery steps** to show the two-step fraud checklist. Record the actual risk and action returned. If the app offers manual guidance, record that as fallback and investigate before recording the online portion.

## Offline recovery

1. Turn on airplane mode. Open **Start an incident → Account takeover → Continue offline**.
2. Choose **Yes, I can sign in** and show the prioritized recovery steps.
3. Enable local progress saving, complete one step, and reopen the same plan from Recovery history. Show the checked step and progress count.
4. Clear recovery history and show the empty state if demonstrating privacy controls.

## Submission evidence

- [ ] Confirm required assets, video constraints and reviewer access in the authenticated event rules (#8).
- [ ] Verify the signed release APK, SHA-256 and clean install (#20).
- [ ] Rehearse both flows on the same release build and record actual results (#18).
- [ ] Record a demo and verify each link from a reviewer-equivalent account using approved access.
- [ ] Capture the submission receipt after authorized submission.

Existing evidence includes a successful synthetic card-fraud result through the live adapter (#13), successful debug-build offline phone tests (#12 and #17), and verified local deletion (#19). The owner verified the Android-to-Render-to-Meta card-fraud flow using [debug build #51](https://github.com/Kamogelo-Skhosana/lockdown/actions/runs/41925799661). The assessment displayed the approved bank-contact action. Opening recovery showed the bundled two-step fraud checklist. The deployed API also returned approved recovery actions for a synthetic account-lockout case. The account-lockout phone test and signed release rehearsal remain pending.
