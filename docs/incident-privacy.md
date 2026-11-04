# Incident privacy boundary

| Threat | Control | Verification |
| --- | --- | --- |
| Draft shared before user choice | Keep draft on the device. Require consent for each online submission. Offline checklist remains reachable. | Decline consent and complete offline triage. |
| Incident text includes secrets | Show a warning before submit. Redact on-device before sending where practical, then run server-side minimization before model egress. Pattern matching is best-effort. Never solicit passwords, codes, or payment details. | Exercise synthetic emails, links, credentials and card-like numbers. |
| Abuse or surprise spend | Limit request bodies before parsing, check consent and the UTF-8 description limit, and apply rate limits and a budget cap before calling the provider. | Oversized, denied and rate-limited requests never call the model. |
| Prompt injection | Treat descriptions as data. Validate structured model output and resolve only approved action IDs. | Malicious text cannot introduce executable actions or arbitrary links. |
| Data in transit or logs | Require HTTPS in deployed environments. Record request IDs, timing and status without bodies, raw prompts, or raw model responses. Disable request-body collection at the proxy and observability layers. | Inspect outbound requests and logs with synthetic markers. |
| Cross-user data access | Assessment is stateless. Return the result only to the submitting request. Keep opt-in history on the device and avoid a server-side history endpoint. | Verify independent requests cannot retrieve previous incident data. |

Lockdown's API should retain no raw incident descriptions or responses. Keep the draft transient and persist only the minimum recovery data when local history is enabled. Turning saving off preserves prior records. Confirming clear history removes locally stored records and progress. Verify the model provider's own retention terms before using real incident text.

The consent, size and redaction helper is committed with unit tests. Transport, abuse controls, provider egress and mobile consent must be verified through the actual API and Android entry points before closing #11.
