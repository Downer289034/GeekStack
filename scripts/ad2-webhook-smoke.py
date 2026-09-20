"""AD2 smoke: POST one signed event to AGENT_WEBHOOK_URL. Loads secret from env only.

HMAC key = STEWARD_WEBHOOK_SECRET 的 UTF-8 字节（与 Java HmacSigner 一致，勿 hex 解码）。
Body = 契约 snake_case（content_id / event_time …）。
"""
import hashlib
import hmac
import json
import os
import sys
import time
import urllib.request


def main():
    secret = os.environ.get("STEWARD_WEBHOOK_SECRET")
    url = os.environ.get("AGENT_WEBHOOK_URL")
    if not secret or not url:
        print("missing AGENT_WEBHOOK_URL or STEWARD_WEBHOOK_SECRET in env", file=sys.stderr)
        sys.exit(2)
    body = json.dumps(
        {
            "content_id": "post_ad2script",
            "content_type": "post",
            "content_text": "ad2 script probe",
            "author_id": "user_0",
            "metadata": {
                "source": "jikezhan",
                "action": "created",
                "event_time": "2026-09-20T16:00:00+08:00",
                "truncated": False,
            },
        },
        separators=(",", ":"),
        ensure_ascii=False,
    ).encode("utf-8")
    ts = str(int(time.time()))
    mode = sys.argv[1] if len(sys.argv) > 1 else "good"
    if mode == "good":
        # 与 Java HmacSigner 一致：密钥为 secret 字符串 UTF-8，非 hex decode
        sig = hmac.new(
            secret.encode("utf-8"),
            f"{ts}.".encode("utf-8") + body,
            hashlib.sha256,
        ).hexdigest()
    else:
        sig = "ab" * 32
    req = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "X-Steward-Signature": f"sha256={sig}",
            "X-Steward-Timestamp": ts,
            "X-Trace-Id": f"wf-ad2-{mode}",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            raw = r.read().decode(errors="replace")
            data = json.loads(raw) if raw else {}
            print(
                json.dumps(
                    {
                        "mode": mode,
                        "status": r.status,
                        "trace_id": data.get("trace_id"),
                        "body_redacted": raw[:200],
                    },
                    ensure_ascii=False,
                )
            )
    except Exception as e:
        code = getattr(e, "code", None)
        raw = ""
        if hasattr(e, "read"):
            raw = e.read().decode(errors="replace")
        print(
            json.dumps(
                {
                    "mode": mode,
                    "status": code,
                    "body_redacted": raw[:200],
                    "error": type(e).__name__,
                },
                ensure_ascii=False,
            )
        )


if __name__ == "__main__":
    main()
