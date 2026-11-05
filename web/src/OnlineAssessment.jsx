import React, { useEffect, useRef, useState } from 'react';
import { parseAssessment } from './assessment.js';

const enabled = import.meta.env.VITE_ENABLE_ASSESSMENT === 'true' ||
  (import.meta.env.DEV && typeof __LOCKDOWN_DEV_PROXY__ !== 'undefined' && __LOCKDOWN_DEV_PROXY__);

export default function OnlineAssessment() {
  const [description, setDescription] = useState('');
  const [consent, setConsent] = useState(false);
  const [status, setStatus] = useState('');
  const [result, setResult] = useState(null);
  const [pending, setPending] = useState(false);
  const request = useRef(null);

  useEffect(() => () => request.current?.abort(), []);

  async function submit(event) {
    event.preventDefault();
    if (request.current || !consent) return;
    const draft = description.trim();
    setResult(null);
    if (!draft || new TextEncoder().encode(draft).length > 4096) {
      setStatus('Enter a description of up to 4,096 bytes.');
      return;
    }

    const controller = new AbortController();
    request.current = controller;
    setPending(true);
    setStatus('Checking your report…');
    const timeout = setTimeout(() => controller.abort(), 15000);
    try {
      const response = await fetch('/api/assessment', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ description: draft, consent: true }),
        signal: controller.signal,
        redirect: 'error',
      });
      const assessment = response.ok ? parseAssessment(await response.json()) : null;
      if (request.current !== controller) return;
      setResult(assessment);
      setStatus(assessment ? 'Assessment ready.' : 'Online analysis is unavailable. Use the checklist above.');
    } catch {
      if (request.current === controller) {
        setStatus('Online analysis is unavailable. Use the checklist above or try again.');
      }
    } finally {
      clearTimeout(timeout);
      if (request.current === controller) {
        request.current = null;
        setPending(false);
      }
    }
  }

  function cancel() {
    const controller = request.current;
    request.current = null;
    controller?.abort();
    setPending(false);
    setStatus('Analysis cancelled. Your draft is still here.');
  }

  return (
    <section className="checklist online" aria-labelledby="online-title">
      <h2 id="online-title">Optional online analysis</h2>
      {enabled ? <form onSubmit={submit}>
        <p>Remove passwords, verification codes and payment details before submitting.</p>
        <label htmlFor="incident-description">Incident description</label>
        <textarea id="incident-description" rows="4" value={description} disabled={pending}
          onChange={(event) => { setDescription(event.target.value); setResult(null); setStatus(''); }} />
        <label className="consent">
          <input type="checkbox" checked={consent} disabled={pending}
            onChange={(event) => setConsent(event.target.checked)} />
          <span>I agree to send this description to Lockdown for analysis by Meta.</span>
        </label>
        {pending ? <button type="button" onClick={cancel}>Cancel analysis</button> :
          <button type="submit" disabled={!consent}>Analyse incident</button>}
        <p role="status">{status}</p>
        {result && <section aria-label="Assessment result">
          <h3>Provisional risk: {result.risk}</h3>
          <ul>{result.actions.map((action) => <li key={action}>{action}</li>)}</ul>
        </section>}
      </form> : <p>Online analysis is unavailable in this build. You can use the checklist above.</p>}
    </section>
  );
}
