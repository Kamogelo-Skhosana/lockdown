import json

from fastapi.testclient import TestClient

from lockdown_api import meta_assessment
from lockdown_api.main import MAX_REQUEST_BYTES, app

client = TestClient(app)


def test_requires_explicit_boolean_consent() -> None:
    for consent in (False, "true", 1):
        response = client.post("/assessment", json={"description": "Unexpected sign-in", "consent": consent})
        assert response.status_code == 403


def test_manual_fallback_does_not_echo_incident_text(monkeypatch) -> None:
    monkeypatch.setenv("META_MODEL_API_ENABLED", "false")
    response = client.post(
        "/assessment", json={"description": "Password: sample-secret", "consent": True}
    )
    assert response.status_code == 200
    assert response.json() == {"mode": "manual"}


def test_oversize_request_is_rejected_before_parsing() -> None:
    response = client.post("/assessment", content=b"x" * (MAX_REQUEST_BYTES + 1))
    assert response.status_code == 413


def test_invalid_json_does_not_echo_body() -> None:
    response = client.post("/assessment", content=b"{sample-secret")
    assert response.status_code == 400
    assert response.json() == {"detail": "Invalid JSON."}


def test_assessment_redacts_provider_request_and_hides_upstream_failure(monkeypatch, caplog) -> None:
    monkeypatch.setenv("META_MODEL_API_ENABLED", "true")
    monkeypatch.setenv("META_MODEL_API_KEY", "synthetic-key")
    monkeypatch.setenv("META_MODEL_ID", "test-model")
    monkeypatch.setattr(meta_assessment, "reserve_provider_request", lambda: True)
    captured = []

    def timeout(request, timeout):
        captured.append(json.loads(request.data)["input"])
        raise TimeoutError("upstream test marker")

    monkeypatch.setattr(meta_assessment, "urlopen", timeout)
    description = "Unexpected sign-in. Email synthetic@example.com, password: sample-secret."

    denied = client.post("/assessment", json={"description": description, "consent": False})
    assert denied.status_code == 403
    assert not captured

    result = client.post("/assessment", json={"description": description, "consent": True})
    assert result.status_code == 200
    assert result.json() == {"mode": "manual"}
    assert captured == ["Unexpected sign-in. Email [email redacted], password [redacted]"]
    for secret in ("sample-secret", "synthetic@example.com", "synthetic-key", "upstream test marker"):
        assert secret not in result.text
        assert secret not in caplog.text
