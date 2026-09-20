"""AD2 smoke: POST one signed event to AGENT_WEBHOOK_URL. Loads secret from env only."""
import hmac, hashlib, os, time, urllib.request, json, sys

def main():
    secret = os.environ.get("STEWARD_WEBHOOK_SECRET")
    url = os.environ.get("AGENT_WEBHOOK_URL")
    if not secret or not url:
        print("missing AGENT_WEBHOOK_URL or STEWARD_WEBHOOK_SECRET in env", file=sys.stderr)
        sys.exit(2)
    body = json.dumps({
        "contentId": "post_ad2script",
        "contentType": "post",
        "contentText": "ad2 script probe",
        "authorId": "user_0",
        "metadata": {
            "source": "jikezhan",
            "action": "created",
            "eventTime": "2026-09-20T16:00:00+08:00",
            "truncated": False,
        },
    }, separators=(",", ":")).encode()
    ts = str(int(time.time()))
    mode = sys.argv[1] if len(sys.argv) > 1 else "good"
    if mode == "good":
        sig = hmac.new(bytes.fromhex(secret), f"{ts}.".encode() + body, hashlib.sha256).hexdigest()
    else:
        sig = "ab" * 32
    req = urllib.request.Request(url, data=body, method="POST", headers={
        "Content-Type": "application/json",
        "X-Steward-Signature": f"sha256={sig}",
        "X-Steward-Timestamp": ts,
        "X-Trace-Id": f"wf-ad2-{mode}",
    })
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            raw = r.read().decode(errors="replace")
            print(json.dumps({"mode": mode, "status": r.status, "body_redacted": raw[:200]}, ensure_ascii=False))
    except Exception as e:
        code = getattr(e, "code", None)
        raw = ""
        if hasattr(e, "read"):
            raw = e.read().decode(errors="replace")
        print(json.dumps({"mode": mode, "status": code, "body_redacted": raw[:200], "error": type(e).__name__}, ensure_ascii=False))

if __name__ == "__main__":
    main()
