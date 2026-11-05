# Lockdown web

First browser slice for issue #23. Choose an incident and work through a bundled checklist. Progress lasts for the current page session. The browser does not collect incident descriptions or call Meta in this slice.

Use Node.js 22.12 or later. From this directory, run `npm install`, then `npm run dev`. Run `npm run build` for a production bundle.

To verify the flow, select Account takeover, choose whether you can sign in, and complete a step. Open the other account path and confirm it has separate progress. Verify every control can be reached with Tab and operated with Enter or Space. Once the page has loaded, disconnect and confirm the checklist still works. A fresh offline page load requires a future service worker.
