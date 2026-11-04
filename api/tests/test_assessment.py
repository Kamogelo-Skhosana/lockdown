from fastapi.testclient import TestClient

from lockdown_api.main import MAX_REQUEST_BYTES, app

client = TestClient(app)


def test_requires_explicit_boolean_consent() -> None:
    for consent in (False, "true", 1):
        response = client.post("/assessment", json={"description": "Unexpected sign-in", "consent": consent})
        assert response.status_code == 403


def test_manual_fallback_does_not_echo_incident_text() -> None:
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
