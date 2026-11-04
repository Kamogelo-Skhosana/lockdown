"""Validate and minimize incident text before a provider call."""

import re

MAX_INCIDENT_BYTES = 4096

_SECRET = re.compile(
    r"(?i)\b(password|passcode|otp|verification code|api key|recovery code)"
    r"\s*(?:is\s+|[:=]\s*|\s+)(?:\"[^\"\n]*\"|'[^'\n]*'|[^\s,;]+)"
)
_EMAIL = re.compile(r"(?i)\b[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b")
_URL = re.compile(r"(?i)\bhttps?://[^\s<>]+")
_NUMBER = re.compile(r"(?<!\w)\+?\d(?:[ .()-]*\d){5,}(?!\w)")


def prepare_incident(description: str, *, consent: bool) -> str:
    """Return minimized text, or reject with an error safe to display."""
    if consent is not True:
        raise ValueError("Explicit consent is required for online analysis.")
    if len(description.encode("utf-8")) > MAX_INCIDENT_BYTES:
        raise ValueError("Incident description is too long.")

    description = description.strip()
    if not description:
        raise ValueError("Incident description is required.")

    description = _SECRET.sub(lambda match: f"{match.group(1)} [redacted]", description)
    description = _URL.sub("[link redacted]", description)
    description = _EMAIL.sub("[email redacted]", description)
    return _NUMBER.sub("[number redacted]", description)
