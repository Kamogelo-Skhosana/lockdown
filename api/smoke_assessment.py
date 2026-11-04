"""Smoke-test a configured HTTPS assessment endpoint with synthetic data."""

import argparse
import json
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

CASES = (
    ("card fraud", "I noticed a card payment I did not make.", "contact_bank"),
    ("account access", "I cannot sign in after an unfamiliar account login.", "official_account_recovery"),
)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("endpoint", help="Full HTTPS URL ending in /assessment")
    parser.add_argument("--expect-manual", action="store_true", help="Rehearse with Meta disabled")
    args = parser.parse_args()
    endpoint = urlsplit(args.endpoint)
    if (endpoint.scheme != "https" or not endpoint.hostname or endpoint.username is not None
            or endpoint.query or endpoint.fragment or not endpoint.path.endswith("/assessment")):
        parser.error("Provide a full HTTPS assessment URL without credentials or query parameters.")

    failed = False
    for name, description, expected_action in CASES:
        request = Request(
            args.endpoint,
            data=json.dumps({"description": description, "consent": True}).encode(),
            headers={"Content-Type": "application/json"},
        )
        try:
            with urlopen(request, timeout=20) as response:
                payload = json.load(response)
        except (OSError, ValueError):
            print(f"{name}: request failed")
            failed = True
            continue

        if args.expect_manual:
            passed = payload == {"mode": "manual"}
        else:
            passed = (
                isinstance(payload, dict)
                and payload.get("mode") == "meta"
                and payload.get("risk") in ("low", "medium", "high")
                and isinstance(payload.get("action_ids"), list)
                and expected_action in payload["action_ids"]
            )
        print(f"{name}: {'passed' if passed else 'unexpected assessment result'}")
        failed |= not passed
    raise SystemExit(int(failed))


if __name__ == "__main__":
    main()
