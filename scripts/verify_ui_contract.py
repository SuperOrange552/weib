from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
templates = list((ROOT / "src/main/resources/templates").glob("*.html"))
shell_css = (ROOT / "src/main/resources/static/css/app-shell.css").read_text(encoding="utf-8")
shell_js = (ROOT / "src/main/resources/static/js/app-shell.js").read_text(encoding="utf-8") if (ROOT / "src/main/resources/static/js/app-shell.js").exists() else ""

checks = {
    "shellCssHasDarkText": "body *{color:#0f172a !important}" in shell_css,
    "shellCssHasLightHeader": "background:#ffffff !important" in shell_css and "border:1px solid #e2e8f0 !important" in shell_css,
    "shellJsAddsForum": "/forum" in shell_js and "论坛" in shell_js,
    "allShellTemplatesLoadScript": all("/css/app-shell.css" not in t.read_text(encoding="utf-8") or "/js/app-shell.js" in t.read_text(encoding="utf-8") for t in templates),
}
failed = [name for name, passed in checks.items() if not passed]
print({"templateCount": len(templates), "checks": checks, "status": "PASS" if not failed else "FAIL", "failed": failed})
raise SystemExit(1 if failed else 0)
