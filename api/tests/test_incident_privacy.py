import pytest

from lockdown_api.incident_privacy import MAX_INCIDENT_BYTES, prepare_incident


def test_online_analysis_requires_explicit_consent() -> None:
    with pytest.raises(ValueError, match="Explicit consent"):
        prepare_incident("Unexpected sign-in", consent=False)


def test_rejects_oversize_unicode_input() -> None:
    with pytest.raises(ValueError, match="too long"):
        prepare_incident("é" * (MAX_INCIDENT_BYTES // 2 + 1), consent=True)


def test_rejects_blank_input() -> None:
    with pytest.raises(ValueError, match="required"):
        prepare_incident(" \n ", consent=True)


def test_redacts_common_identifiers_and_credentials() -> None:
    description = (
        "Unexpected sign-in. Email alex@example.com, password: 'secret phrase', "
        "OTP 123456, card 4111 1111 1111 1111, https://example.com/?token=abc"
    )
    assert prepare_incident(description, consent=True) == (
        "Unexpected sign-in. Email [email redacted], password [redacted], "
        "OTP [redacted], card [number redacted], [link redacted]"
    )


def test_preserves_useful_context() -> None:
    assert prepare_incident("  Unexpected sign-in at 9:41.  ", consent=True) == (
        "Unexpected sign-in at 9:41."
    )
