from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
common = (ROOT / "src/main/resources/application.yml").read_text(encoding="utf-8")
local_path = ROOT / "src/main/resources/application-local.yml"
prod_path = ROOT / "src/main/resources/application-prod.yml"
local = local_path.read_text(encoding="utf-8") if local_path.exists() else ""
prod = prod_path.read_text(encoding="utf-8") if prod_path.exists() else ""
redirect = (ROOT / "src/main/java/com/weib/config/HttpsRedirectConfig.java").read_text(encoding="utf-8")

checks = {
    "commonHasNoHardcodedSsl": "key-store:" not in common and "port: 8443" not in common,
    "localProfile": "on-profile: local" in local and "port: 8443" in local and "enabled: true" in local and "secure: true" in local,
    "prodProfile": "on-profile: prod" in prod and "SERVER_PORT:8888" in prod and "enabled: false" in prod and "secure: true" in prod,
    "prodHttpsOrigins": "https://superorange.top,https://www.superorange.top" in prod,
    "prodForwardHeaders": "forward-headers-strategy: framework" in prod,
    "redirectOnlyLocal": '@Profile("local")' in redirect,
    "websocketOriginsPerProfile": "https://localhost:8443" in local and "https://superorange.top" in prod,
}
failed = [name for name, passed in checks.items() if not passed]
print({"checks": checks, "status": "PASS" if not failed else "FAIL", "failed": failed})
raise SystemExit(1 if failed else 0)
