# Lockdown web

First browser slice for issue #23. Choose an incident and work through a bundled checklist. Progress lasts for the current page session. Online analysis is optional and available only when configured.

Use Node.js 22.12 or later. From this directory, run `npm install`, then `npm run dev`. Run `npm run build` for a production bundle.

To verify the flow, select Account takeover, choose whether you can sign in, and complete a step. Open the other account path and confirm it has separate progress. Verify every control can be reached with Tab and operated with Enter or Space. Once the page has loaded, disconnect and confirm the checklist still works. A fresh offline page load requires a future service worker.

## Optional online assessment

For local development, set `LOCKDOWN_API_ORIGIN=https://lockdown-s17y.onrender.com` in the shell or in an untracked `.env.local` before running `npm run dev`. Vite forwards the browser's same-origin `POST /api/assessment` to FastAPI's `POST /assessment`.

For deployment, configure the host to proxy `POST /api/assessment` to the HTTPS FastAPI endpoint, then build with `VITE_ENABLE_ASSESSMENT=true`. Without that flag, production builds offer the checklist only. The Meta key stays in the backend environment. Use synthetic incident descriptions for smoke tests.

