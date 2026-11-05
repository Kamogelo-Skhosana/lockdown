const actions = new Map([
  ['official_account_recovery', 'Open the account provider’s official recovery page.'],
  ['review_account_sessions', 'Review active sessions in the provider’s security settings.'],
  ['verify_suspicious_message', 'Verify the message through an official channel.'],
  ['contact_bank', 'Contact your bank through its official app or the number on your card.'],
  ['lock_lost_device', 'Use the provider’s official device-finding service to lock the device.'],
]);

export function parseAssessment(result) {
  if (result?.mode !== 'meta' || !['low', 'medium', 'high'].includes(result.risk)) return null;
  const ids = result.action_ids;
  if (!Array.isArray(ids) || ids.length < 1 || ids.length > 3 ||
      new Set(ids).size !== ids.length || ids.some((id) => !actions.has(id))) return null;
  return { risk: result.risk, actions: ids.map((id) => actions.get(id)) };
}
