import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';
import OnlineAssessment from './OnlineAssessment.jsx';

const plans = {
  account: ['Account takeover', [
    'On a trusted device, open the account provider’s official app or website.',
    'If you can sign in, change your password and remove unfamiliar sessions.',
    'If you’re locked out, use the provider’s official account recovery process.',
  ]],
  access: ['Secure your account', [
    'From a trusted device, open the provider’s official app or site and change your password.',
    'Review active sessions and sign out unfamiliar devices.',
    'Turn on two-step verification in the provider’s security settings.',
  ]],
  locked: ['Recover account access', [
    'From a trusted device, open the provider’s official account recovery page.',
    'Follow the provider’s recovery steps and secure the email used to recover the account.',
  ]],
  message: ['Suspicious message', [
    'Avoid opening links and attachments in the message.',
    'Verify the request through an official app, website, or number you already trust.',
  ]],
  device: ['Lost device', [
    'From a trusted device, use the provider’s official device-finding service to try to lock the device.',
    'Contact your mobile carrier to secure your SIM if the device can’t be located.',
  ]],
  fraud: ['Payment fraud', [
    'Contact your bank using its official app or the number on your card or statement.',
    'Report the unfamiliar transaction and ask how to secure the account.',
  ]],
  unknown: ['General protective steps', [
    'Pause before opening links, sending money, or sharing codes.',
    'Use a trusted device to check recent activity through the official provider.',
  ]],
};

const categories = [
  ['account', 'An unfamiliar sign-in or lost account access'],
  ['message', 'A link, attachment, or request you don’t trust'],
  ['device', 'A missing phone, tablet, or computer'],
  ['fraud', 'A card charge or transfer you don’t recognise'],
  ['unknown', 'Start with general protective steps'],
];

function App() {
  const [plan, setPlan] = useState('unknown');
  const [completed, setCompleted] = useState({});
  const [title, steps] = plans[plan];
  const checked = completed[plan] ?? [];
  const count = steps.filter((step) => checked.includes(step)).length;
  const isAccount = ['account', 'access', 'locked'].includes(plan);

  function markStep(step, done) {
    setCompleted((current) => {
      const next = new Set(current[plan]);
      if (done) next.add(step);
      else next.delete(step);
      return { ...current, [plan]: [...next] };
    });
  }

  return (
    <>
      <header><strong>Lockdown</strong><span>Digital incident response</span></header>
      <main>
        <div className="intro">
          <p className="eyebrow">Web recovery checklist</p>
          <h1>What happened?</h1>
          <p>Choose an incident to see protective steps you can take now.</p>
        </div>
        <div className="workspace">
          <fieldset className="categories">
            <legend>Choose an incident</legend>
            {categories.map(([id, hint]) => (
              <label className="category" key={id}>
                <input type="radio" name="incident" checked={id === 'account' ? isAccount : plan === id} onChange={() => setPlan(id)} />
                <span><strong>{id === 'unknown' ? 'I’m not sure' : plans[id][0]}</strong><small>{hint}</small></span>
              </label>
            ))}
          </fieldset>
          <section className="checklist" aria-labelledby="plan-title">
            <p className="eyebrow">Protective steps</p>
            <h2 id="plan-title">{title}</h2>
            {isAccount && <fieldset className="access">
              <legend>Can you still sign in?</legend>
              {[['access', 'Yes'], ['locked', 'No'], ['account', 'I’m not sure']].map(([id, label]) => (
                <label key={id}><input type="radio" name="access" checked={plan === id} onChange={() => setPlan(id)} /> {label}</label>
              ))}
            </fieldset>}
            <p className="progress" role="status">{count} of {steps.length} steps complete</p>
            <ol>{steps.map((step) => (
              <li key={step}>
                <label><input type="checkbox" checked={checked.includes(step)} onChange={(event) => markStep(step, event.target.checked)} /><span>{step}</span></label>
              </li>
            ))}</ol>
            <p className="note">Checklist progress stays on this page until you reload or close it.</p>
          </section>
        </div>
        <OnlineAssessment />
      </main>
    </>
  );
}

createRoot(document.getElementById('root')).render(<App />);

