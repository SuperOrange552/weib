#!/usr/bin/env python3
"""Extract a deterministic inventory of Spring MVC routes from Weib controllers."""

from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path


MAPPING_RE = re.compile(r"@(Get|Post|Put|Delete|Patch)Mapping(?:\s*\((.*?)\))?", re.DOTALL)


@dataclass(frozen=True, order=True)
class Endpoint:
    method: str
    path: str
    controller: str
    handler: str
    page_route: bool

    @property
    def key(self) -> str:
        return f"{self.method} {self.path}"


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    return re.sub(r"//.*?$", "", text, flags=re.MULTILINE)


def normalize_path(value: str) -> str:
    value = re.sub(r"/+", "/", value.strip())
    if not value:
        return "/"
    if not value.startswith("/"):
        value = "/" + value
    return value[:-1] if len(value) > 1 and value.endswith("/") else value


def class_prefix(text: str) -> str:
    class_match = re.search(r"\bclass\s+\w+", text)
    header = text[: class_match.start()] if class_match else ""
    matches = list(re.finditer(r"@RequestMapping\s*\((.*?)\)", header, re.DOTALL))
    if not matches:
        return ""
    values = re.findall(r'"([^"]*)"', matches[-1].group(1))
    return values[0] if values else ""


def _handler_name(text: str, annotation_end: int) -> str:
    tail = text[annotation_end : annotation_end + 1800]
    match = re.search(
        r"(?:public|protected|private)\s+(?:static\s+)?[\w<>,.?\[\]\s]+?\s+(\w+)\s*\(",
        tail,
        re.DOTALL,
    )
    return match.group(1) if match else "unknown"


def _is_page_handler(text: str, match: re.Match[str], method: str) -> bool:
    class_match = re.search(r"\bclass\s+\w+", text)
    header = text[: class_match.start()] if class_match else text
    if method != "GET" or "@RestController" in header:
        return False
    nearby = text[max(0, match.start() - 300) : match.end() + 900]
    if "@ResponseBody" in nearby:
        return False
    signature = re.search(
        r"(?:public|protected)\s+([\w<>,.?\[\]\s]+?)\s+\w+\s*\(",
        text[match.end() : match.end() + 1200],
        re.DOTALL,
    )
    return bool(signature and signature.group(1).strip() == "String")


def extract_endpoints(controller_root: Path) -> list[Endpoint]:
    values: dict[tuple[str, str], Endpoint] = {}
    for source in sorted(controller_root.rglob("*Controller.java")):
        text = strip_comments(source.read_text(encoding="utf-8"))
        prefix = class_prefix(text)
        for match in MAPPING_RE.finditer(text):
            method = match.group(1).upper()
            args = match.group(2) or ""
            paths = re.findall(r'"([^"]*)"', args) or [""]
            handler = _handler_name(text, match.end())
            page_route = _is_page_handler(text, match, method)
            for suffix in paths:
                endpoint = Endpoint(
                    method=method,
                    path=normalize_path(prefix + suffix),
                    controller=source.stem,
                    handler=handler,
                    page_route=page_route,
                )
                key = (endpoint.method, endpoint.path)
                if key in values and values[key] != endpoint:
                    raise ValueError(f"duplicate MVC route: {endpoint.key}")
                values[key] = endpoint
    return sorted(values.values(), key=lambda item: (item.path, item.method))


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[1] / "src/main/java/com/weib/controller"
    endpoints = extract_endpoints(root)
    print(f"total={len(endpoints)} business={sum(not e.page_route for e in endpoints)} pages={sum(e.page_route for e in endpoints)}")
    for endpoint in endpoints:
        print(f"{endpoint.key}\t{endpoint.controller}.{endpoint.handler}\tpage={endpoint.page_route}")
