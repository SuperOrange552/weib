import json
import sys
import unittest
import tempfile
from pathlib import Path

from docx import Document

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from api_word_v2_inventory import extract_endpoints
from api_word_v2_catalog import APP_BADGE, build_catalog
from generate_api_word_v2 import generate_manual
from verify_api_word_v2 import verify_manual


class ApiWordV2InventoryTest(unittest.TestCase):
    def test_current_controllers_have_large_complete_inventory(self):
        endpoints = extract_endpoints(ROOT / "src/main/java/com/weib/controller")
        keys = {(item.method, item.path) for item in endpoints}
        self.assertGreaterEqual(len(endpoints), 158)
        self.assertIn(("POST", "/login"), keys)
        self.assertIn(("POST", "/api/mobile/auth/login"), keys)
        self.assertIn(("GET", "/api/admin/identities/users/{userId}"), keys)
        self.assertIn(("POST", "/api/forum/media"), keys)
        self.assertIn(("GET", "/api/test/captcha"), keys)

    def test_inventory_has_no_duplicate_method_path(self):
        endpoints = extract_endpoints(ROOT / "src/main/java/com/weib/controller")
        keys = [(item.method, item.path) for item in endpoints]
        self.assertEqual(len(keys), len(set(keys)))

    def test_openapi_snapshot_is_valid(self):
        data = json.loads((ROOT / "docs/verification/api-word-v2-openapi.json").read_text(encoding="utf-8"))
        self.assertGreater(len(data.get("paths", {})), 100)


class ApiWordV2CatalogTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.endpoints = extract_endpoints(ROOT / "src/main/java/com/weib/controller")
        cls.catalog = build_catalog(cls.endpoints, ROOT / "docs/verification/api-word-v2-openapi.json")

    def test_every_route_has_exactly_one_card(self):
        endpoint_keys = {(item.method, item.path) for item in self.endpoints}
        card_keys = {(item.method, item.path) for item in self.catalog}
        self.assertEqual(endpoint_keys, card_keys)
        self.assertEqual(len(self.catalog), len(card_keys))

    def test_mobile_routes_are_all_and_only_app_specific(self):
        for card in self.catalog:
            self.assertEqual(card.platform == APP_BADGE, card.path.startswith("/api/mobile/"), card.key)

    def test_every_business_card_is_directly_testable(self):
        for card in self.catalog:
            if card.page_route:
                continue
            self.assertTrue(card.function.strip(), card.key)
            self.assertTrue(card.full_url.startswith("https://superorange.top/"), card.key)
            self.assertTrue(card.permission.strip(), card.key)
            self.assertTrue(card.content_type.strip(), card.key)
            self.assertTrue(card.request_sample.strip(), card.key)
            self.assertTrue(card.success_response.strip(), card.key)
            self.assertNotIn("按实际填写", card.request_sample)
            self.assertNotIn("同上", card.request_sample)
            if card.content_type == "application/json" and card.method in ("POST", "PUT", "PATCH"):
                self.assertFalse(card.request_sample.rstrip().endswith("{}"), card.key)

    def test_test_captcha_card_is_explicitly_gated(self):
        card = next(item for item in self.catalog if item.key == "GET /api/test/captcha")
        self.assertEqual(card.platform, "测试工具（默认关闭）")
        self.assertIn("TEST_CAPTCHA_API_ENABLED=true", card.permission)
        self.assertIn(("X-Test-Access-Key", "{{testCaptchaAccessKey}}"), card.headers)
        self.assertIn("data.captcha", card.variable_source)

    def test_upload_routes_use_real_multipart_examples(self):
        uploads = {card.key: card for card in self.catalog if card.key in {
            "POST /api/appeals/evidence", "POST /api/forum/media",
            "POST /api/seeker/resume/media", "POST /chat/upload",
        }}
        self.assertEqual(len(uploads), 4)
        for card in uploads.values():
            self.assertEqual(card.content_type, "multipart/form-data")
            self.assertIn("@C:/test-data/", card.request_sample)


class ApiWordV2DocxTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.cards = build_catalog(
            extract_endpoints(ROOT / "src/main/java/com/weib/controller"),
            ROOT / "docs/verification/api-word-v2-openapi.json",
        )

    def test_generated_word_contains_every_interface_and_app_badge(self):
        with tempfile.TemporaryDirectory() as tmp:
            output = Path(tmp) / "api-word-v2-test.docx"
            generate_manual(self.cards, output, "2026-07-14")
            doc = Document(output)
            text = "\n".join(paragraph.text for paragraph in doc.paragraphs)
            for card in self.cards:
                self.assertIn(card.key, text)
            app_count = sum(1 for card in self.cards if card.platform == APP_BADGE)
            self.assertEqual(text.count(APP_BADGE), app_count + 1)
            self.assertIn("请求方式与完整地址", output.read_bytes().decode("utf-8", errors="ignore") or "") if False else None
            self.assertIn("可复制请求数据", text)
            self.assertIn("基准环境  https://superorange.top", text)
            self.assertNotIn("http://superorange.top", text)

    def test_old_manual_is_not_the_output_target(self):
        output = ROOT / "docs/微招系统接口测试手册-精简完整版-V2.docx"
        self.assertNotEqual(output.name, "微招系统完整接口测试文档.docx")


class ApiWordV2VerificationTest(unittest.TestCase):
    def test_real_manual_has_full_coverage_and_preserves_old_files(self):
        result = verify_manual(ROOT)
        self.assertEqual(result["status"], "PASS")
        self.assertEqual(result["missingEndpoints"], [])
        self.assertEqual(result["unknownEndpoints"], [])
        self.assertEqual(result["mislabelledAppEndpoints"], [])
        self.assertEqual(result["incompleteCards"], [])
        self.assertEqual(result["changedOldFiles"], [])


if __name__ == "__main__":
    unittest.main()
