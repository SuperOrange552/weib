from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
compose = (ROOT / "src/main/resources/templates/forum-compose.html").read_text(encoding="utf-8")
script = (ROOT / "src/main/resources/static/js/forum-compose.js").read_text(encoding="utf-8")
css = (ROOT / "src/main/resources/static/css/forum.css").read_text(encoding="utf-8")
shell = (ROOT / "src/main/resources/static/css/app-shell.css").read_text(encoding="utf-8")

checks = {
    "fileInput": 'type="file"' in compose and 'accept="image/*"' in compose,
    "dropzone": 'forum-upload' in compose and 'dragover' in script and 'drop' in script,
    "uploadRequest": "/api/forum/media" in script and "FormData" in script,
    "darkBodyText": "--app-text:#0f172a" in shell and "color:var(--app-text)" in shell,
    "contrastControls": "--app-control-text:#ffffff" in shell and "--app-control-bg:#0f766e" in shell,
    "forumUploadStyle": ".forum-upload" in css and ".forum-upload.is-dragover" in css,
}
failed = [name for name, passed in checks.items() if not passed]
print({"checks": checks, "status": "PASS" if not failed else "FAIL", "failed": failed})
raise SystemExit(1 if failed else 0)
