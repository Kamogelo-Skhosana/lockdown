import io
import json
from urllib.error import HTTPError

import pytest

from lockdown_api import meta_assessment as meta


def response(risk, actions):
    return {
        "status": "completed",
        "output": [{"type": "message", "content": [{
            "type": "output_text",
            "text": json.dumps({"risk": risk, "action_ids": actions}),
        }]}],
    }


@pytest.mark.parametrize("action", ["contact_bank", "review_account_sessions"])
def test_valid_model_response(action):
    assert meta.parse_assessment(response("high", [action])) == {
        "mode": "meta", "risk": "high", "action_ids": [action]
    }


@pytest.mark.parametrize("actions", [["https://example.com"], ["contact_bank", "contact_bank"], [{}]])
def test_unsafe_actions_fall_back(actions):
    assert meta.parse_assessment(response("high", actions)) == {"mode": "manual"}


@pytest.mark.parametrize("error", [TimeoutError(), HTTPError(meta.ENDPOINT, 429, "", {}, None),
                                   HTTPError(meta.ENDPOINT, 503, "", {}, None)])
def test_provider_errors_fall_back(monkeypatch, error):
    monkeypatch.setenv("META_MODEL_API_KEY", "synthetic")
    monkeypatch.setenv("META_MODEL_ID", "test-model")

    def fail(request, timeout):
        raise error

    monkeypatch.setattr(meta, "urlopen", fail)
    assert meta.assess_with_meta("Synthetic incident") == {"mode": "manual"}


def test_request_is_bounded_and_stateless(monkeypatch):
    monkeypatch.setenv("META_MODEL_API_KEY", "synthetic")
    monkeypatch.setenv("META_MODEL_ID", "test-model")

    def send(request, timeout):
        payload = json.loads(request.data)
        assert timeout == 8
        assert payload["store"] is False
        assert payload["max_output_tokens"] == 512
        assert payload["input"] == "Synthetic incident"
        return io.BytesIO(json.dumps(response("medium", ["contact_bank"])).encode())

    monkeypatch.setattr(meta, "urlopen", send)
    assert meta.assess_with_meta("Synthetic incident")["mode"] == "meta"


def test_disabled_provider_does_not_make_network_request(monkeypatch):
    monkeypatch.setenv("META_MODEL_API_ENABLED", "false")
    monkeypatch.setenv("META_MODEL_API_KEY", "synthetic")
    monkeypatch.setenv("META_MODEL_ID", "test-model")

    def unexpected_request(*args, **kwargs):
        pytest.fail("Disabled provider made a network request")

    monkeypatch.setattr(meta, "urlopen", unexpected_request)
    assert meta.assess_with_meta("Synthetic incident") == {"mode": "manual"}
