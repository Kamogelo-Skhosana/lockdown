import assert from 'node:assert/strict';
import test from 'node:test';
import { parseAssessment } from './assessment.js';

test('accepts the assessment shape returned by FastAPI', () => {
  const result = parseAssessment({ mode: 'meta', risk: 'high', action_ids: ['contact_bank'] });
  assert.deepEqual(result, {
    risk: 'high',
    actions: ['Contact your bank through its official app or the number on your card.'],
  });
});

test('rejects unavailable, malformed and unapproved results', () => {
  const invalid = [
    null,
    { mode: 'manual' },
    { mode: 'meta', risk: 'unknown', action_ids: ['contact_bank'] },
    { mode: 'meta', risk: 'high', action_ids: [] },
    { mode: 'meta', risk: 'high', action_ids: ['https://example.com'] },
    { mode: 'meta', risk: 'high', action_ids: ['contact_bank', 'contact_bank'] },
  ];
  for (const payload of invalid) assert.equal(parseAssessment(payload), null);
});
