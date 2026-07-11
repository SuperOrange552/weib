from pathlib import Path

templates=sorted(Path("src/main/resources/templates").glob("*.html"))
missing=[p.name for p in templates if "/css/app-shell.css" not in p.read_text(encoding="utf-8")]
print({"templateCount":len(templates),"missingShellCss":missing,"status":"PASS" if not missing else "INCOMPLETE"})
raise SystemExit(0 if not missing else 1)
