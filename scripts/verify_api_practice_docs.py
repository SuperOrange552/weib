#!/usr/bin/env python3
"""Verify that the complete API practice manual covers every MVC endpoint."""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import asdict, dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CONTROLLER_ROOT = ROOT / "src/main/java/com/weib/controller"
DOC_PATH = ROOT / "docs/API_TESTING_COMPLETE.md"
EVIDENCE_PATH = ROOT / "docs/verification/api-docs-verification.json"

MAPPING_RE = re.compile(
    r"@(Get|Post|Put|Delete|Patch)Mapping(?:\s*\((.*?)\))?",
    re.DOTALL,
)
DOC_ENDPOINT_RE = re.compile(
    r"^###\s+`(GET|POST|PUT|DELETE|PATCH)\s+([^`]+)`",
    re.MULTILINE,
)
REQUIRED_TEXT = [
    "POST /login",
    "POST /api/admin/auth/login",
    "JSESSIONID",
    "_csrf",
    "X-Captcha-Expires-In",
    "Idempotency-Key",
    "Authorization: Bearer",
    "encodedId",
]


@dataclass(frozen=True, order=True)
class Endpoint:
    method: str
    path: str


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    return re.sub(r"//.*?$", "", text, flags=re.MULTILINE)


def normalize_path(path: str) -> str:
    path = re.sub(r"/+", "/", path.strip())
    if not path:
        return "/"
    if not path.startswith("/"):
        path = "/" + path
    return path[:-1] if len(path) > 1 and path.endswith("/") else path


def class_prefix(text: str) -> str:
    class_pos = text.find(" class ")
    if class_pos < 0:
        class_pos = text.find(" class\n")
    header = text[: class_pos if class_pos >= 0 else 0]
    matches = list(re.finditer(r'@RequestMapping\s*\((.*?)\)', header, re.DOTALL))
    if not matches:
        return ""
    values = re.findall(r'"([^"]+)"', matches[-1].group(1))
    return values[0] if values else ""


def is_page_handler(annotation_start: int, annotation_end: int, text: str, method: str) -> bool:
    if method != "GET" or "@RestController" in text[: text.find(" class ")]:
        return False
    nearby = text[max(0, annotation_start - 250) : annotation_end + 500]
    if "@ResponseBody" in nearby:
        return False
    signature = re.search(r"public\s+([\w<>?,. ]+)\s+\w+\s*\(", text[annotation_end : annotation_end + 700])
    if not signature:
        return False
    return signature.group(1).strip() == "String"


def extract_controller_endpoints(root: Path) -> tuple[set[Endpoint], set[Endpoint]]:
    business: set[Endpoint] = set()
    pages: set[Endpoint] = set()
    for source in root.rglob("*Controller.java"):
        text = strip_comments(source.read_text(encoding="utf-8"))
        prefix = class_prefix(text)
        for match in MAPPING_RE.finditer(text):
            method = match.group(1).upper()
            args = match.group(2) or ""
            values = re.findall(r'"([^"]+)"', args) or [""]
            target = pages if is_page_handler(match.start(), match.end(), text, method) else business
            for value in values:
                target.add(Endpoint(method, normalize_path(prefix + value)))
    return business, pages


def extract_documented_endpoints(text: str) -> tuple[set[Endpoint], set[Endpoint]]:
    marker = "## 13. 页面型路由附录"
    before, separator, after = text.partition(marker)
    if not separator:
        before, after = text, ""

    def parse(section: str) -> set[Endpoint]:
        return {
            Endpoint(method, normalize_path(path.split("?")[0]))
            for method, path in DOC_ENDPOINT_RE.findall(section)
        }

    return parse(before), parse(after)


def endpoint_strings(values: set[Endpoint]) -> list[str]:
    return [f"{item.method} {item.path}" for item in sorted(values)]


def write_evidence(payload: dict) -> None:
    EVIDENCE_PATH.parent.mkdir(parents=True, exist_ok=True)
    existing = {}
    if EVIDENCE_PATH.exists():
        try:
            existing = json.loads(EVIDENCE_PATH.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            existing = {}
    existing.update(payload)
    EVIDENCE_PATH.write_text(json.dumps(existing, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--allow-incomplete", action="store_true")
    args = parser.parse_args()

    business, pages = extract_controller_endpoints(CONTROLLER_ROOT)
    if not DOC_PATH.exists():
        payload = {
            "controllerEndpointCount": len(business) + len(pages),
            "controllerBusinessEndpointCount": len(business),
            "controllerPageEndpointCount": len(pages),
            "documentedMainEndpointCount": 0,
            "documentedAppendixEndpointCount": 0,
            "missingEndpoints": endpoint_strings(business | pages),
            "unknownDocumentedEndpoints": [],
            "requiredTextMissing": REQUIRED_TEXT,
            "status": "INCOMPLETE",
        }
        write_evidence(payload)
        print(f"ERROR: manual does not exist: {DOC_PATH}", file=sys.stderr)
        return 1

    text = DOC_PATH.read_text(encoding="utf-8")
    documented_business, documented_pages = extract_documented_endpoints(text)
    implemented = business | pages
    documented = documented_business | documented_pages
    missing = implemented - documented
    unknown = documented - implemented
    required_missing = [value for value in REQUIRED_TEXT if value not in text]
    misplaced_pages = (documented_pages - pages) | (pages & documented_business)

    passed = not missing and not unknown and not required_missing and not misplaced_pages
    payload = {
        "controllerEndpointCount": len(implemented),
        "controllerBusinessEndpointCount": len(business),
        "controllerPageEndpointCount": len(pages),
        "documentedMainEndpointCount": len(documented_business),
        "documentedAppendixEndpointCount": len(documented_pages),
        "missingEndpoints": endpoint_strings(missing),
        "unknownDocumentedEndpoints": endpoint_strings(unknown),
        "misplacedPageEndpoints": endpoint_strings(misplaced_pages),
        "requiredTextMissing": required_missing,
        "status": "PASS" if passed else "INCOMPLETE",
    }
    write_evidence(payload)
    print(json.dumps(payload, ensure_ascii=False, indent=2))
    if passed or args.allow_incomplete:
        return 0
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
