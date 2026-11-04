import json

from fastapi import FastAPI, HTTPException, Request

from lockdown_api.incident_privacy import MAX_INCIDENT_BYTES, prepare_incident

app = FastAPI(title="Lockdown API")

# A Unicode character may occupy up to twelve bytes in escaped JSON.
MAX_REQUEST_BYTES = MAX_INCIDENT_BYTES * 12 + 1024


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/assessment")
async def assess(request: Request) -> dict[str, str]:
    body = bytearray()
    async for chunk in request.stream():
        if len(body) + len(chunk) > MAX_REQUEST_BYTES:
            raise HTTPException(413, "Request body is too large.")
        body.extend(chunk)

    try:
        payload = json.loads(body)
    except (ValueError, RecursionError):
        raise HTTPException(400, "Invalid JSON.") from None

    if not isinstance(payload, dict) or not isinstance(payload.get("description"), str):
        raise HTTPException(422, "Incident description is required.")
    if payload.get("consent") is not True:
        raise HTTPException(403, "Explicit consent is required for online analysis.")

    try:
        prepare_incident(payload["description"], consent=True)
    except (ValueError, UnicodeError):
        raise HTTPException(422, "Enter a valid incident description within the size limit.") from None

    # Until the provider is configured, the client can use bundled guidance.
    return {"mode": "manual"}
