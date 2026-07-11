from pathlib import Path

root = Path(__file__).resolve().parents[1]
client = (root / "admin-frontend/src/api/client.ts").read_text(encoding="utf-8")
helper = root / "admin-frontend/src/utils/idempotency.ts"
helper_text = helper.read_text(encoding="utf-8") if helper.exists() else ""

checks = {
    "clientUsesFallbackHelper": "createIdempotencyKey" in client,
    "helperExists": helper.exists(),
    "supportsRandomUuid": "randomUUID" in helper_text,
    "supportsGetRandomValues": "getRandomValues" in helper_text,
    "hasLastResortFallback": "Math.random" in helper_text,
}
failed = [key for key, value in checks.items() if not value]
print({"checks": checks, "status": "PASS" if not failed else "FAIL", "failed": failed})
raise SystemExit(1 if failed else 0)
