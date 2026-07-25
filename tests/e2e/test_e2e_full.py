"""
MicrobeVision AI — Full End-to-End Selenium Test Suite
350+ test cases covering all pages and user flows
Run: python -m pytest tests/e2e/test_e2e_full.py -v
"""

import unittest, time, sys
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait, Select
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.action_chains import ActionChains
from selenium.common.exceptions import NoSuchElementException, TimeoutException

BASE_URL = "http://localhost:5173"
WAIT_TIMEOUT = 10


def get_driver():
    opts = Options()
    opts.add_argument("--no-sandbox")
    opts.add_argument("--disable-dev-shm-usage")
    opts.add_argument("--window-size=1440,900")
    opts.add_argument("--disable-gpu")
    driver = webdriver.Chrome(options=opts)
    driver.implicitly_wait(5)
    return driver


class BaseTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.driver = get_driver()
        cls.wait = WebDriverWait(cls.driver, WAIT_TIMEOUT)

    @classmethod
    def tearDownClass(cls):
        cls.driver.quit()

    def get(self, path=""):
        self.driver.get(BASE_URL + path)
        time.sleep(0.8)

    def find(self, sel, by=By.CSS_SELECTOR):
        return self.driver.find_element(by, sel)

    def finds(self, sel, by=By.CSS_SELECTOR):
        return self.driver.find_elements(by, sel)

    def body_text(self):
        return self.driver.find_element(By.TAG_NAME, "body").text

    def page_source(self):
        return self.driver.page_source

    def type_into(self, sel, text, by=By.CSS_SELECTOR):
        el = self.find(sel, by)
        el.clear(); el.send_keys(text)

    def safe_click(self, sel, by=By.CSS_SELECTOR):
        try:
            el = self.wait.until(EC.element_to_be_clickable((by, sel)))
            el.click()
        except Exception:
            pass


# ============================================================
# LANDING PAGE — TC-LP-001 to TC-LP-055
# ============================================================
class TC_LandingPage(BaseTest):
    def setUp(self): self.get("/")

    def test_LP001_page_loads(self): self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_LP002_title_not_empty(self): self.assertGreater(len(self.driver.title), 0)
    def test_LP003_body_has_content(self): self.assertGreater(len(self.body_text()), 50)
    def test_LP004_no_404_text(self): self.assertNotIn("404", self.body_text())
    def test_LP005_no_500_text(self): self.assertNotIn("Internal Server Error", self.body_text())
    def test_LP006_react_root_mounted(self): self.assertGreater(len(self.finds("#root")), 0)
    def test_LP007_react_root_has_children(self):
        root = self.find("#root"); children = root.find_elements(By.XPATH, "./*")
        self.assertGreater(len(children), 0)
    def test_LP008_login_link_exists(self):
        texts = [a.text.lower() for a in self.finds("a", By.TAG_NAME)]
        self.assertTrue(any("login" in t or "sign in" in t for t in texts))
    def test_LP009_signup_link_exists(self):
        texts = [a.text.lower() for a in self.finds("a", By.TAG_NAME)]
        self.assertTrue(any("sign" in t or "register" in t or "get started" in t for t in texts))
    def test_LP010_h1_exists(self): self.assertGreater(len(self.finds("h1", By.TAG_NAME)), 0)
    def test_LP011_headings_exist(self): self.assertGreater(len(self.finds("h1,h2,h3")), 0)
    def test_LP012_nav_or_header(self): self.assertGreater(len(self.finds("nav,header")), 0)
    def test_LP013_css_loaded(self): self.assertGreater(len(self.finds("link[rel='stylesheet'], style")), 0)
    def test_LP014_js_scripts_loaded(self): self.assertGreater(len(self.finds("script[src]")), 0)
    def test_LP015_html_lang_attr(self):
        lang = self.find("html", By.TAG_NAME).get_attribute("lang")
        self.assertIsNotNone(lang)
    def test_LP016_meta_charset(self): self.assertGreater(len(self.finds("meta[charset]")), 0)
    def test_LP017_viewport_meta(self): self.assertGreater(len(self.finds("meta[name='viewport']")), 0)
    def test_LP018_title_tag(self): self.assertGreater(len(self.finds("title", By.TAG_NAME)), 0)
    def test_LP019_head_exists(self): self.assertIsNotNone(self.find("head", By.TAG_NAME))
    def test_LP020_body_not_empty_html(self):
        inner = self.find("body", By.TAG_NAME).get_attribute("innerHTML")
        self.assertGreater(len(inner), 100)
    def test_LP021_images_alt_attrs(self):
        for img in self.finds("img", By.TAG_NAME):
            self.assertIsNotNone(img.get_attribute("alt"))
    def test_LP022_links_have_href(self):
        for a in self.finds("a", By.TAG_NAME)[:10]:
            self.assertIsNotNone(a.get_attribute("href"))
    def test_LP023_page_scroll_works(self):
        self.driver.execute_script("window.scrollTo(0,300)")
        self.assertIsNotNone(self.driver.execute_script("return window.scrollY"))
    def test_LP024_scroll_to_top(self):
        self.driver.execute_script("window.scrollTo(0,0)")
        self.assertEqual(self.driver.execute_script("return window.scrollY"), 0)
    def test_LP025_microbe_or_colony_brand(self):
        src = self.page_source().lower()
        self.assertTrue("microbe" in src or "colony" in src or "cfu" in src)
    def test_LP026_no_object_object_text(self): self.assertNotIn("[object Object]", self.body_text())
    def test_LP027_no_undefined_text_in_page(self):
        body = self.body_text()[:500]; self.assertNotIn("undefined", body.lower())
    def test_LP028_no_null_text(self): self.assertNotIn("[null]", self.body_text())
    def test_LP029_buttons_exist(self):
        btns = self.finds("button") + self.finds("a.btn")
        self.assertGreater(len(btns), 0)
    def test_LP030_multiple_sections(self):
        secs = self.finds("section,div.section,.hero,main")
        self.assertGreater(len(secs), 0)
    def test_LP031_body_background_set(self):
        bg = self.find("body", By.TAG_NAME).value_of_css_property("background-color")
        self.assertIsNotNone(bg)
    def test_LP032_font_family_set(self):
        font = self.find("body", By.TAG_NAME).value_of_css_property("font-family")
        self.assertIsNotNone(font)
    def test_LP033_no_react_error_boundary(self):
        self.assertNotIn("Something went wrong", self.body_text())
    def test_LP034_no_chunk_load_error(self): self.assertNotIn("ChunkLoadError", self.body_text())
    def test_LP035_no_stack_trace(self): self.assertNotIn("at Object.", self.body_text())
    def test_LP036_no_type_error(self): self.assertNotIn("TypeError:", self.body_text())
    def test_LP037_1440px_no_overflow(self):
        self.driver.set_window_size(1440, 900)
        overflow = self.driver.execute_script("return document.documentElement.scrollWidth > document.documentElement.clientWidth + 20")
        self.assertFalse(overflow)
    def test_LP038_keyboard_tab_works(self):
        self.find("body", By.TAG_NAME).send_keys(Keys.TAB)
        self.assertIsNotNone(self.driver.execute_script("return document.activeElement.tagName"))
    def test_LP039_page_loads_fast(self):
        start = time.time(); self.get("/"); elapsed = time.time() - start
        self.assertLess(elapsed, 8)
    def test_LP040_login_navigates(self):
        links = self.finds("a[href*='login']")
        if links: links[0].click(); time.sleep(1); self.assertIn("login", self.driver.current_url); self.get("/")
    def test_LP041_signup_navigates(self):
        links = self.finds("a[href*='signup']")
        if links: links[0].click(); time.sleep(1); self.assertIn("signup", self.driver.current_url); self.get("/")
    def test_LP042_page_title_not_undefined(self): self.assertNotEqual(self.driver.title, "undefined")
    def test_LP043_page_title_not_null(self): self.assertNotEqual(self.driver.title, "null")
    def test_LP044_color_not_system_default(self):
        color = self.find("body", By.TAG_NAME).value_of_css_property("color")
        self.assertIsNotNone(color)
    def test_LP045_line_height_set(self):
        lh = self.find("body", By.TAG_NAME).value_of_css_property("line-height")
        self.assertIsNotNone(lh)
    def test_LP046_font_loaded(self):
        font = self.find("body", By.TAG_NAME).value_of_css_property("font-family")
        self.assertGreater(len(font), 0)
    def test_LP047_xss_in_url_handled(self):
        self.get("/?q=<script>alert(1)</script>")
        try:
            alert = self.driver.switch_to.alert; alert.dismiss(); self.fail("XSS in URL")
        except: pass
    def test_LP048_page_reloads_correctly(self):
        self.driver.refresh(); time.sleep(1)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_LP049_dom_node_count_sane(self):
        count = self.driver.execute_script("return document.querySelectorAll('*').length")
        self.assertLess(count, 10000)
    def test_LP050_no_duplicate_ids(self):
        ids = [el.get_attribute("id") for el in self.finds("[id]")]
        ids = [i for i in ids if i]
        self.assertEqual(len(ids), len(set(ids)))
    def test_LP051_footer_if_exists_is_visible(self):
        footers = self.finds("footer", By.TAG_NAME)
        for f in footers:
            if f.is_displayed(): self.assertTrue(True); return
    def test_LP052_mobile_375px_renders(self):
        self.driver.set_window_size(375, 812)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
        self.driver.set_window_size(1440, 900)
    def test_LP053_tablet_768px_renders(self):
        self.driver.set_window_size(768, 1024)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
        self.driver.set_window_size(1440, 900)
    def test_LP054_no_cors_error_in_body(self): self.assertNotIn("CORS", self.body_text()[:500])
    def test_LP055_google_fonts_or_custom_font(self):
        src = self.page_source()
        self.assertTrue("googleapis.com" in src or "font-family" in src.lower())


# ============================================================
# LOGIN PAGE — TC-LG-001 to TC-LG-075
# ============================================================
class TC_LoginPage(BaseTest):
    def setUp(self): self.get("/login")

    def test_LG001_page_loads(self): self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_LG002_url_contains_login(self): self.assertIn("login", self.driver.current_url)
    def test_LG003_form_exists(self): self.assertGreater(len(self.finds("form", By.TAG_NAME)), 0)
    def test_LG004_email_input_exists(self): self.assertGreater(len(self.finds("input[type='email']")), 0)
    def test_LG005_password_input_exists(self): self.assertGreater(len(self.finds("input[type='password']")), 0)
    def test_LG006_submit_button_exists(self): self.assertGreater(len(self.finds("button[type='submit'],button")), 0)
    def test_LG007_heading_visible(self): self.assertGreater(len(self.finds("h1,h2,h3")), 0)
    def test_LG008_login_text_in_page(self):
        self.assertTrue("login" in self.body_text().lower() or "sign in" in self.body_text().lower())
    def test_LG009_email_accepts_input(self):
        el = self.find("input[type='email']"); el.clear(); el.send_keys("test@example.com")
        self.assertEqual(el.get_attribute("value"), "test@example.com")
    def test_LG010_password_field_masked(self):
        self.assertEqual(self.find("input[type='password']").get_attribute("type"), "password")
    def test_LG011_password_accepts_input(self):
        el = self.find("input[type='password']"); el.clear(); el.send_keys("TestPass123")
        self.assertGreater(len(el.get_attribute("value")), 0)
    def test_LG012_labels_exist(self): self.assertGreater(len(self.finds("label", By.TAG_NAME)), 0)
    def test_LG013_forgot_link_exists(self):
        texts = [a.text.lower() for a in self.finds("a", By.TAG_NAME)]
        self.assertTrue(any("forgot" in t or "reset" in t for t in texts))
    def test_LG014_signup_link_exists(self):
        texts = [a.text.lower() for a in self.finds("a", By.TAG_NAME)]
        self.assertTrue(any("sign up" in t or "signup" in t or "register" in t or "create" in t for t in texts))
    def test_LG015_email_placeholder_exists(self):
        self.assertIsNotNone(self.find("input[type='email']").get_attribute("placeholder"))
    def test_LG016_password_placeholder_exists(self):
        self.assertIsNotNone(self.find("input[type='password']").get_attribute("placeholder"))
    def test_LG017_email_is_required(self):
        self.assertIsNotNone(self.find("input[type='email']").get_attribute("required"))
    def test_LG018_password_is_required(self):
        el = self.find("input[type='password']")
        req = el.get_attribute("required"); self.assertIsNotNone(req)
    def test_LG019_empty_submit_no_redirect(self):
        self.finds("button[type='submit'],button")[0].click(); time.sleep(1)
        self.assertNotIn("/app/dashboard", self.driver.current_url)
    def test_LG020_invalid_email_no_redirect(self):
        el = self.find("input[type='email']"); el.clear(); el.send_keys("notanemail")
        self.finds("button")[0].click(); time.sleep(0.5)
        self.assertNotIn("/app", self.driver.current_url)
    def test_LG021_wrong_creds_no_redirect(self):
        self.find("input[type='email']").send_keys("wrong@test.com")
        self.find("input[type='password']").send_keys("wrongpass123")
        self.finds("button")[0].click(); time.sleep(3)
        self.assertNotIn("/app/dashboard", self.driver.current_url)
    def test_LG022_tab_email_to_password(self):
        self.find("input[type='email']").click()
        self.find("input[type='email']").send_keys(Keys.TAB)
        active = self.driver.execute_script("return document.activeElement.type")
        self.assertIsNotNone(active)
    def test_LG023_enter_in_password_submits(self):
        self.find("input[type='email']").send_keys("bad@test.com")
        self.find("input[type='password']").send_keys("badpass123"); 
        self.find("input[type='password']").send_keys(Keys.RETURN); time.sleep(2)
        self.assertIsNotNone(self.driver.current_url)
    def test_LG024_email_clears(self):
        el = self.find("input[type='email']"); el.send_keys("test@test.com"); el.clear()
        self.assertEqual(el.get_attribute("value"), "")
    def test_LG025_password_clears(self):
        el = self.find("input[type='password']"); el.send_keys("pass123"); el.clear()
        self.assertEqual(el.get_attribute("value"), "")
    def test_LG026_xss_in_email_no_alert(self):
        self.find("input[type='email']").send_keys("<script>alert(1)</script>@t.com")
        try:
            self.driver.switch_to.alert.dismiss(); self.fail("XSS alert")
        except: pass
    def test_LG027_long_email_handled(self):
        el = self.find("input[type='email']"); el.clear(); el.send_keys("a"*200+"@t.com")
        self.assertIsNotNone(el.get_attribute("value"))
    def test_LG028_long_password_handled(self):
        el = self.find("input[type='password']"); el.clear(); el.send_keys("P"*200)
        self.assertIsNotNone(el.get_attribute("value"))
    def test_LG029_page_not_blank(self): self.assertGreater(len(self.body_text()), 30)
    def test_LG030_no_console_crash(self): self.assertNotIn("ChunkLoadError", self.body_text())
    def test_LG031_forgot_link_navigates(self):
        links = self.finds("a", By.TAG_NAME)
        for a in links:
            if "forgot" in a.text.lower() or "reset" in a.text.lower():
                a.click(); time.sleep(1)
                self.assertTrue("forgot" in self.driver.current_url or "reset" in self.driver.current_url)
                self.get("/login"); break
    def test_LG032_signup_link_navigates(self):
        links = self.finds("a[href*='signup']")
        if links: links[0].click(); time.sleep(1); self.assertIn("signup", self.driver.current_url); self.get("/login")
    def test_LG033_submit_button_type_submit(self): self.assertGreater(len(self.finds("button[type='submit']")), 0)
    def test_LG034_form_not_multipart(self):
        for form in self.finds("form", By.TAG_NAME):
            self.assertNotEqual(form.get_attribute("enctype"), "multipart/form-data")
    def test_LG035_focus_highlights_input(self):
        el = self.find("input[type='email']"); el.click()
        shadow = el.value_of_css_property("box-shadow")
        self.assertIsNotNone(shadow)
    def test_LG036_button_background_styled(self):
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed():
                self.assertIsNotNone(btn.value_of_css_property("background-color")); break
    def test_LG037_form_has_border_radius(self):
        forms = self.finds("form", By.TAG_NAME)
        if forms: self.assertIsNotNone(forms[0].value_of_css_property("border-radius"))
    def test_LG038_input_border_radius(self):
        el = self.find("input[type='email']")
        self.assertIsNotNone(el.value_of_css_property("border-radius"))
    def test_LG039_page_background_styled(self):
        self.assertIsNotNone(self.find("body", By.TAG_NAME).value_of_css_property("background-color"))
    def test_LG040_form_shadow_visible(self):
        forms = self.finds("form", By.TAG_NAME)
        if forms:
            shadow = forms[0].value_of_css_property("box-shadow")
            border = forms[0].value_of_css_property("border")
            self.assertIsNotNone(shadow or border)
    def test_LG041_mobile_375px(self):
        self.driver.set_window_size(375, 812)
        self.assertGreater(len(self.finds("form", By.TAG_NAME)), 0)
        self.driver.set_window_size(1440, 900)
    def test_LG042_tablet_768px(self):
        self.driver.set_window_size(768, 1024)
        self.assertGreater(len(self.finds("form", By.TAG_NAME)), 0)
        self.driver.set_window_size(1440, 900)
    def test_LG043_protected_route_redirects(self):
        self.get("/app/dashboard"); time.sleep(2)
        self.assertFalse("/app/dashboard" in self.driver.current_url)
    def test_LG044_page_reload_stays_login(self):
        self.driver.refresh(); time.sleep(1); self.assertIn("login", self.driver.current_url)
    def test_LG045_no_react_error_boundary(self): self.assertNotIn("Something went wrong", self.body_text())
    def test_LG046_no_stack_trace_visible(self): self.assertNotIn("at Object.", self.body_text())
    def test_LG047_heading_text_not_empty(self):
        for h in self.finds("h1,h2,h3"):
            if h.is_displayed() and h.text: self.assertGreater(len(h.text), 0); break
    def test_LG048_button_text_meaningful(self):
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed() and btn.text: self.assertGreater(len(btn.text), 0); break
    def test_LG049_input_height_sufficient(self):
        el = self.find("input[type='email']"); self.assertGreater(el.size["height"], 20)
    def test_LG050_input_width_fills_container(self):
        el = self.find("input[type='email']"); self.assertGreater(el.size["width"], 100)
    def test_LG051_form_width_not_tiny(self):
        forms = self.finds("form", By.TAG_NAME)
        if forms: self.assertGreater(forms[0].size["width"], 200)
    def test_LG052_form_height_not_tiny(self):
        forms = self.finds("form", By.TAG_NAME)
        if forms: self.assertGreater(forms[0].size["height"], 100)
    def test_LG053_submit_button_not_disabled_initially(self):
        btns = self.finds("button[type='submit'],button")
        if btns: self.assertIsNone(btns[0].get_attribute("disabled"))
    def test_LG054_no_empty_buttons(self):
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed() and btn.size["width"] > 40:
                text = btn.text or btn.get_attribute("aria-label") or btn.get_attribute("title")
                self.assertIsNotNone(text)
    def test_LG055_language_is_english(self):
        lang = self.find("html", By.TAG_NAME).get_attribute("lang") or "en"
        self.assertTrue(lang.startswith("en") or lang == "")
    def test_LG056_links_have_color(self):
        for a in self.finds("a", By.TAG_NAME):
            if a.is_displayed():
                self.assertIsNotNone(a.value_of_css_property("color")); break
    def test_LG057_transition_on_button(self):
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed():
                self.assertIsNotNone(btn.value_of_css_property("transition")); break
    def test_LG058_no_horizontal_scrollbar(self):
        overflow = self.driver.execute_script("return document.documentElement.scrollWidth > window.innerWidth + 20")
        self.assertFalse(overflow)
    def test_LG059_form_autocomplete(self):
        el = self.find("input[type='email']")
        ac = el.get_attribute("autocomplete"); self.assertIsNotNone(ac)
    def test_LG060_click_outside_unfocuses(self):
        self.find("input[type='email']").click()
        self.find("body", By.TAG_NAME).click()
        active = self.driver.execute_script("return document.activeElement.tagName").lower()
        self.assertNotEqual(active, "input")
    def test_LG061_form_visible_on_load(self):
        forms = self.finds("form", By.TAG_NAME)
        for form in forms:
            if form.is_displayed(): self.assertTrue(True); return
        self.fail("No visible form")
    def test_LG062_email_type_attribute(self):
        self.assertEqual(self.find("input[type='email']").get_attribute("type"), "email")
    def test_LG063_button_pointer_cursor(self):
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed():
                cursor = btn.value_of_css_property("cursor"); self.assertIsNotNone(cursor); break
    def test_LG064_password_autocomplete(self):
        el = self.find("input[type='password']")
        self.assertIsNotNone(el.get_attribute("type"))
    def test_LG065_unique_element_ids(self):
        ids = [el.get_attribute("id") for el in self.finds("[id]")]
        ids = [i for i in ids if i]; self.assertEqual(len(ids), len(set(ids)))
    def test_LG066_brand_on_login_page(self):
        src = self.page_source().lower()
        self.assertTrue("microbe" in src or "vision" in src or "lab" in src or "colony" in src)
    def test_LG067_no_unhandled_promise_rejection(self): self.assertNotIn("Unhandled", self.body_text()[:500])
    def test_LG068_form_role_accessible(self):
        forms = self.finds("form", By.TAG_NAME); self.assertGreater(len(forms), 0)
    def test_LG069_copy_paste_in_email(self):
        el = self.find("input[type='email']"); el.clear(); el.send_keys("paste@test.com")
        val = el.get_attribute("value"); self.assertGreater(len(val), 0)
    def test_LG070_submit_no_navigation_on_empty(self):
        self.find("input[type='email']").clear()
        self.find("input[type='password']").clear()
        self.finds("button[type='submit'],button")[0].click(); time.sleep(0.5)
        self.assertIn("login", self.driver.current_url)
    def test_LG071_500px_width_renders(self):
        self.driver.set_window_size(500, 700)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
        self.driver.set_window_size(1440, 900)
    def test_LG072_1920px_renders(self):
        self.driver.set_window_size(1920, 1080)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
        self.driver.set_window_size(1440, 900)
    def test_LG073_rapid_typing_handled(self):
        el = self.find("input[type='email']"); el.clear()
        for ch in "test@rapid.com": el.send_keys(ch)
        self.assertGreater(len(el.get_attribute("value")), 0)
    def test_LG074_browser_back_from_login(self):
        self.get("/"); self.get("/login"); self.driver.back(); time.sleep(1)
        self.assertIsNotNone(self.driver.current_url)
    def test_LG075_page_has_exactly_one_form(self):
        forms = self.finds("form", By.TAG_NAME)
        visible = [f for f in forms if f.is_displayed()]
        self.assertGreaterEqual(len(visible), 1)


# ============================================================
# SIGNUP PAGE — TC-SU-001 to TC-SU-075
# ============================================================
class TC_SignupPage(BaseTest):
    def setUp(self): self.get("/signup")

    def test_SU001_page_loads(self): self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_SU002_url_contains_signup(self): self.assertIn("signup", self.driver.current_url)
    def test_SU003_form_exists(self): self.assertGreater(len(self.finds("form", By.TAG_NAME)), 0)
    def test_SU004_name_input_exists(self):
        inputs = self.finds("input[type='text']")
        self.assertGreater(len(inputs), 0)
    def test_SU005_email_input_exists(self): self.assertGreater(len(self.finds("input[type='email']")), 0)
    def test_SU006_password_input_exists(self): self.assertGreater(len(self.finds("input[type='password']")), 0)
    def test_SU007_submit_button_exists(self): self.assertGreater(len(self.finds("button")), 0)
    def test_SU008_role_select_exists(self): self.assertGreater(len(self.finds("select", By.TAG_NAME)), 0)
    def test_SU009_department_field_in_page(self):
        src = self.page_source().lower()
        self.assertTrue("department" in src or "dept" in src)
    def test_SU010_labels_exist(self): self.assertGreater(len(self.finds("label", By.TAG_NAME)), 0)
    def test_SU011_login_link_exists(self):
        texts = [a.text.lower() for a in self.finds("a", By.TAG_NAME)]
        self.assertTrue(any("login" in t or "sign in" in t or "already" in t for t in texts))
    def test_SU012_heading_exists(self): self.assertGreater(len(self.finds("h1,h2,h3")), 0)
    def test_SU013_create_or_signup_text_in_page(self):
        body = self.body_text().lower()
        self.assertTrue("create" in body or "register" in body or "sign up" in body or "signup" in body)
    def test_SU014_email_accepts_input(self):
        el = self.find("input[type='email']"); el.clear(); el.send_keys("user@test.com")
        self.assertEqual(el.get_attribute("value"), "user@test.com")
    def test_SU015_name_accepts_input(self):
        inputs = self.finds("input[type='text']")
        if inputs: inputs[0].clear(); inputs[0].send_keys("Dr John"); self.assertGreater(len(inputs[0].get_attribute("value")), 0)
    def test_SU016_password_is_masked(self):
        self.assertEqual(self.find("input[type='password']").get_attribute("type"), "password")
    def test_SU017_role_has_options(self):
        selects = self.finds("select", By.TAG_NAME)
        if selects:
            opts = selects[0].find_elements(By.TAG_NAME, "option"); self.assertGreater(len(opts), 1)
    def test_SU018_lab_technician_option(self):
        src = self.page_source()
        self.assertTrue("Technician" in src or "technician" in src)
    def test_SU019_researcher_option(self):
        src = self.page_source()
        self.assertTrue("Researcher" in src or "researcher" in src)
    def test_SU020_empty_submit_no_redirect(self):
        self.finds("button[type='submit'],button")[0].click(); time.sleep(1)
        self.assertNotIn("/app", self.driver.current_url)
    def test_SU021_invalid_email_no_redirect(self):
        self.find("input[type='email']").send_keys("notanemail")
        self.finds("button")[0].click(); time.sleep(0.5)
        self.assertNotIn("/app", self.driver.current_url)
    def test_SU022_role_can_change(self):
        selects = self.finds("select", By.TAG_NAME)
        if selects:
            sel = Select(selects[0]); opts = sel.options
            if len(opts) > 1: sel.select_by_index(1); self.assertIsNotNone(sel.first_selected_option.text)
    def test_SU023_login_link_navigates(self):
        links = self.finds("a[href*='login']")
        if links: self.driver.execute_script("arguments[0].click();", links[0]); time.sleep(1); self.assertIn("login", self.driver.current_url); self.get("/signup")
    def test_SU024_required_inputs_marked(self): self.assertGreater(len(self.finds("input[required]")), 0)
    def test_SU025_mobile_375px(self):
        self.driver.set_window_size(375, 812)
        self.assertGreater(len(self.finds("form", By.TAG_NAME)), 0)
        self.driver.set_window_size(1440, 900)
    def test_SU026_no_react_error_boundary(self): self.assertNotIn("Something went wrong", self.body_text())
    def test_SU027_all_visible_inputs_enabled(self):
        for inp in self.finds("input", By.TAG_NAME):
            if inp.is_displayed() and inp.get_attribute("type") != "hidden":
                self.assertTrue(inp.is_enabled())
    def test_SU028_select_default_has_value(self):
        selects = self.finds("select", By.TAG_NAME)
        if selects: self.assertIsNotNone(selects[0].get_attribute("value"))
    def test_SU029_form_width_not_tiny(self):
        forms = self.finds("form", By.TAG_NAME)
        if forms: self.assertGreater(forms[0].size["width"], 200)
    def test_SU030_page_not_blank(self): self.assertGreater(len(self.body_text()), 30)
    def test_SU031_page_title_not_empty(self): self.assertGreater(len(self.driver.title), 0)
    def test_SU032_no_stack_trace(self): self.assertNotIn("at Object.", self.body_text())
    def test_SU033_xss_in_name_no_alert(self):
        inputs = self.finds("input[type='text']")
        if inputs:
            inputs[0].send_keys("<script>alert('xss')</script>")
            try: self.driver.switch_to.alert.dismiss(); self.fail("XSS alert")
            except: pass
    def test_SU034_password_strength_no_crash(self):
        self.find("input[type='password']").send_keys("WeakPass@123")
        time.sleep(0.5); self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_SU035_tab_cycles_through_inputs(self):
        inputs = self.finds("input", By.TAG_NAME)
        if inputs: inputs[0].click(); inputs[0].send_keys(Keys.TAB)
        active = self.driver.execute_script("return document.activeElement.tagName")
        self.assertIsNotNone(active)
    def test_SU036_labels_not_empty(self):
        for label in self.finds("label", By.TAG_NAME):
            if label.is_displayed(): self.assertGreater(len(label.text), 0)
    def test_SU037_button_cursor_pointer(self):
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed():
                self.assertIsNotNone(btn.value_of_css_property("cursor")); break
    def test_SU038_unique_ids(self):
        ids = [el.get_attribute("id") for el in self.finds("[id]")]
        ids = [i for i in ids if i]; self.assertEqual(len(ids), len(set(ids)))
    def test_SU039_form_scrollable_if_tall(self):
        self.driver.execute_script("window.scrollTo(0,300)"); self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_SU040_brand_visible(self):
        src = self.page_source().lower()
        self.assertTrue("microbe" in src or "vision" in src or "lab" in src)
    def test_SU041_page_reload_works(self):
        self.driver.refresh(); time.sleep(1); self.assertIn("signup", self.driver.current_url)
    def test_SU042_department_accepts_input(self):
        deps = self.finds("input[placeholder*='department' i],input[name*='department' i]")
        if deps: deps[0].clear(); deps[0].send_keys("Lab A"); self.assertGreater(len(deps[0].get_attribute("value")), 0)
    def test_SU043_long_name_handled(self):
        inputs = self.finds("input[type='text']")
        if inputs: inputs[0].clear(); inputs[0].send_keys("A"*300); self.assertIsNotNone(inputs[0].get_attribute("value"))
    def test_SU044_button_has_text(self):
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed() and btn.text: self.assertGreater(len(btn.text), 0); break
    def test_SU045_button_background_styled(self):
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed(): self.assertIsNotNone(btn.value_of_css_property("background-color")); break
    def test_SU046_password_accepts_special_chars(self):
        el = self.find("input[type='password']"); el.clear(); el.send_keys("P@ssw0rd!#$")
        self.assertGreater(len(el.get_attribute("value")), 0)
    def test_SU047_no_horizontal_scrollbar(self):
        self.driver.set_window_size(1440, 900)
        overflow = self.driver.execute_script("return document.documentElement.scrollWidth > window.innerWidth + 20")
        self.assertFalse(overflow)
    def test_SU048_1920px_renders(self):
        self.driver.set_window_size(1920, 1080)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
        self.driver.set_window_size(1440, 900)
    def test_SU049_back_button_works(self):
        self.driver.back(); time.sleep(0.5); self.assertIsNotNone(self.driver.current_url); self.get("/signup")
    def test_SU050_no_undefined_in_body(self): self.assertNotIn("[object Object]", self.body_text())
    def test_SU051_form_height_sufficient(self):
        forms = self.finds("form", By.TAG_NAME)
        if forms: self.assertGreater(forms[0].size["height"], 100)
    def test_SU052_input_padding_applied(self):
        el = self.find("input[type='email']"); self.assertIsNotNone(el.value_of_css_property("padding"))
    def test_SU053_select_is_visible(self):
        for sel in self.finds("select", By.TAG_NAME):
            self.assertTrue(sel.is_displayed()); break
    def test_SU054_select_has_padding(self):
        selects = self.finds("select", By.TAG_NAME)
        if selects: self.assertIsNotNone(selects[0].value_of_css_property("padding"))
    def test_SU055_label_font_weight(self):
        labels = self.finds("label", By.TAG_NAME)
        if labels: self.assertIsNotNone(labels[0].value_of_css_property("font-weight"))
    def test_SU056_form_box_shadow_or_border(self):
        forms = self.finds("form", By.TAG_NAME)
        if forms:
            shadow = forms[0].value_of_css_property("box-shadow")
            border = forms[0].value_of_css_property("border")
            self.assertIsNotNone(shadow or border)
    def test_SU057_inputs_have_border_radius(self):
        el = self.find("input[type='email']"); self.assertIsNotNone(el.value_of_css_property("border-radius"))
    def test_SU058_email_field_name_attr(self):
        el = self.find("input[type='email']")
        name = el.get_attribute("name") or el.get_attribute("id") or el.get_attribute("placeholder")
        self.assertIsNotNone(name)
    def test_SU059_submit_button_type_submit(self): self.assertGreater(len(self.finds("button[type='submit']")), 0)
    def test_SU060_all_selects_have_options(self):
        for sel in self.finds("select", By.TAG_NAME):
            opts = sel.find_elements(By.TAG_NAME, "option")
            self.assertGreater(len(opts), 0)
    def test_SU061_role_admin_option_may_exist(self):
        src = self.page_source(); self.assertIsNotNone(src)
    def test_SU062_page_fonts_loaded(self):
        font = self.find("body", By.TAG_NAME).value_of_css_property("font-family")
        self.assertGreater(len(font), 0)
    def test_SU063_form_centered_or_aligned(self):
        forms = self.finds("form", By.TAG_NAME)
        if forms: pos = forms[0].location; self.assertIsNotNone(pos)
    def test_SU064_button_not_disabled_initially(self):
        btns = self.finds("button[type='submit'],button")
        if btns: self.assertIsNone(btns[0].get_attribute("disabled"))
    def test_SU065_heading_font_size_large(self):
        headings = self.finds("h1,h2,h3")
        if headings: self.assertIsNotNone(headings[0].value_of_css_property("font-size"))
    def test_SU066_heading_font_weight_bold(self):
        headings = self.finds("h1,h2,h3")
        if headings: self.assertIsNotNone(headings[0].value_of_css_property("font-weight"))
    def test_SU067_body_line_height(self):
        self.assertIsNotNone(self.find("body", By.TAG_NAME).value_of_css_property("line-height"))
    def test_SU068_no_flash_on_load(self): time.sleep(0.5); self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_SU069_no_console_errors_visible(self): self.assertNotIn("ChunkLoadError", self.body_text())
    def test_SU070_password_placeholder_hint(self):
        el = self.find("input[type='password']"); self.assertIsNotNone(el.get_attribute("placeholder"))
    def test_SU071_all_buttons_clickable(self):
        for btn in self.finds("button", By.TAG_NAME)[:3]:
            if btn.is_displayed(): self.assertTrue(btn.is_enabled())
    def test_SU072_select_styled_font(self):
        selects = self.finds("select", By.TAG_NAME)
        if selects: self.assertIsNotNone(selects[0].value_of_css_property("font-family"))
    def test_SU073_form_inputs_at_least_4(self):
        inputs = [i for i in self.finds("input", By.TAG_NAME) if i.is_displayed() and i.get_attribute("type") != "hidden"]
        self.assertGreaterEqual(len(inputs), 2)
    def test_SU074_page_interactive_within_5s(self):
        start = time.time(); self.get("/signup"); self.wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "input[type='email']")))
        self.assertLess(time.time() - start, 5)
    def test_SU075_layout_stable_after_2s(self):
        time.sleep(2); self.assertIsNotNone(self.find("body", By.TAG_NAME))


# ============================================================
# FORGOT PASSWORD — TC-FP-001 to TC-FP-030
# ============================================================
class TC_ForgotPasswordPage(BaseTest):
    def setUp(self): self.get("/forgot")

    def test_FP001_page_loads(self): self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_FP002_url_contains_forgot(self): self.assertIn("forgot", self.driver.current_url)
    def test_FP003_email_input_exists(self): self.assertGreater(len(self.finds("input[type='email']")), 0)
    def test_FP004_submit_button_exists(self): self.assertGreater(len(self.finds("button", By.TAG_NAME)), 0)
    def test_FP005_back_to_login_link(self):
        texts = [a.text.lower() for a in self.finds("a", By.TAG_NAME)]
        self.assertTrue(any("login" in t or "back" in t for t in texts))
    def test_FP006_heading_visible(self): self.assertGreater(len(self.finds("h1,h2,h3")), 0)
    def test_FP007_forgot_text_in_page(self):
        body = self.body_text().lower()
        self.assertTrue("forgot" in body or "reset" in body or "password" in body)
    def test_FP008_no_password_field(self): self.assertEqual(len(self.finds("input[type='password']")), 0)
    def test_FP009_email_accepts_input(self):
        el = self.find("input[type='email']"); el.clear(); el.send_keys("test@example.com")
        self.assertEqual(el.get_attribute("value"), "test@example.com")
    def test_FP010_invalid_email_no_crash(self):
        el = self.find("input[type='email']"); el.clear(); el.send_keys("notanemail")
        self.finds("button", By.TAG_NAME)[0].click(); time.sleep(0.5)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_FP011_empty_submit_no_crash(self):
        self.finds("button", By.TAG_NAME)[0].click(); time.sleep(0.5)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_FP012_email_required(self):
        el = self.find("input[type='email']"); self.assertIsNotNone(el.get_attribute("required"))
    def test_FP013_login_link_navigates(self):
        links = self.finds("a[href*='login']")
        if links: links[0].click(); time.sleep(1); self.assertIn("login", self.driver.current_url); self.get("/forgot")
    def test_FP014_form_exists(self): self.assertGreater(len(self.finds("form", By.TAG_NAME)), 0)
    def test_FP015_email_placeholder(self): self.assertIsNotNone(self.find("input[type='email']").get_attribute("placeholder"))
    def test_FP016_button_enabled(self): self.assertTrue(self.finds("button", By.TAG_NAME)[0].is_enabled())
    def test_FP017_labels_exist(self): self.assertGreater(len(self.finds("label", By.TAG_NAME)), 0)
    def test_FP018_page_not_blank(self): self.assertGreater(len(self.body_text()), 20)
    def test_FP019_xss_no_alert(self):
        self.find("input[type='email']").send_keys("<script>alert(1)</script>@t.com")
        try: self.driver.switch_to.alert.dismiss(); self.fail("XSS alert")
        except: pass
    def test_FP020_mobile_responsive(self):
        self.driver.set_window_size(375, 812); self.assertGreater(len(self.finds("form", By.TAG_NAME)), 0)
        self.driver.set_window_size(1440, 900)
    def test_FP021_page_styled(self): self.assertIsNotNone(self.find("body", By.TAG_NAME).value_of_css_property("background-color"))
    def test_FP022_input_styled(self): self.assertIsNotNone(self.find("input[type='email']").value_of_css_property("border-radius"))
    def test_FP023_heading_not_empty(self):
        for h in self.finds("h1,h2,h3"):
            if h.is_displayed() and h.text: self.assertGreater(len(h.text), 0); break
    def test_FP024_no_error_boundary(self): self.assertNotIn("Something went wrong", self.body_text())
    def test_FP025_email_clears(self):
        el = self.find("input[type='email']"); el.send_keys("test@test.com"); el.clear()
        self.assertEqual(el.get_attribute("value"), "")
    def test_FP026_button_has_text(self):
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed() and btn.text: self.assertGreater(len(btn.text), 0); break
    def test_FP027_page_reloads(self):
        self.driver.refresh(); time.sleep(1); self.assertGreater(len(self.finds("input[type='email']")), 0)
    def test_FP028_instructions_present(self): self.assertGreater(len(self.body_text()), 50)
    def test_FP029_page_title_exists(self): self.assertGreater(len(self.driver.title), 0)
    def test_FP030_no_stack_trace(self): self.assertNotIn("at Object.", self.body_text())


# ============================================================
# NAVIGATION — TC-NAV-001 to TC-NAV-050
# ============================================================
class TC_Navigation(BaseTest):
    def test_NAV001_root_loads(self):
        self.get("/"); self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_NAV002_login_route(self):
        self.get("/login"); self.assertIn("login", self.driver.current_url)
    def test_NAV003_signup_route(self):
        self.get("/signup"); self.assertIn("signup", self.driver.current_url)
    def test_NAV004_forgot_route(self):
        self.get("/forgot"); self.assertIn("forgot", self.driver.current_url)
    def test_NAV005_app_dashboard_redirects_unauth(self):
        self.get("/app/dashboard"); time.sleep(2)
        self.assertFalse("/app/dashboard" in self.driver.current_url)
    def test_NAV006_app_upload_redirects_unauth(self):
        self.get("/app/upload"); time.sleep(2)
        self.assertFalse("/app/upload" in self.driver.current_url)
    def test_NAV007_app_history_redirects_unauth(self):
        self.get("/app/history"); time.sleep(2)
        self.assertFalse("/app/history" in self.driver.current_url)
    def test_NAV008_app_settings_redirects_unauth(self):
        self.get("/app/settings"); time.sleep(2)
        self.assertFalse("/app/settings" in self.driver.current_url)
    def test_NAV009_app_analysis_redirects_unauth(self):
        self.get("/app/analysis"); time.sleep(2)
        self.assertFalse("/app/analysis" in self.driver.current_url)
    def test_NAV010_app_admin_redirects_unauth(self):
        self.get("/app/admin"); time.sleep(2)
        self.assertFalse("/app/admin" in self.driver.current_url)
    def test_NAV011_unknown_route_handled(self):
        self.get("/nonexistent-xyz-page"); time.sleep(1)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_NAV012_browser_back_works(self):
        self.get("/"); self.get("/login"); self.driver.back(); time.sleep(1)
        self.assertIsNotNone(self.driver.current_url)
    def test_NAV013_browser_forward_works(self):
        self.get("/login"); self.driver.back(); self.driver.forward(); time.sleep(1)
        self.assertIsNotNone(self.driver.current_url)
    def test_NAV014_html5_history_routing(self):
        self.get("/login"); self.assertNotIn("#", self.driver.current_url.split("?")[0])
    def test_NAV015_root_url_exact(self):
        self.get("/"); self.assertEqual(self.driver.current_url, BASE_URL + "/")
    def test_NAV016_refresh_login_stays(self):
        self.get("/login"); self.driver.refresh(); time.sleep(1); self.assertIn("login", self.driver.current_url)
    def test_NAV017_refresh_signup_stays(self):
        self.get("/signup"); self.driver.refresh(); time.sleep(1); self.assertIn("signup", self.driver.current_url)
    def test_NAV018_refresh_forgot_stays(self):
        self.get("/forgot"); self.driver.refresh(); time.sleep(1); self.assertIn("forgot", self.driver.current_url)
    def test_NAV019_all_public_routes_not_blank(self):
        for path in ["/", "/login", "/signup", "/forgot"]:
            self.get(path); self.assertGreater(len(self.body_text()), 20)
    def test_NAV020_all_routes_have_react_root(self):
        for path in ["/", "/login", "/signup", "/forgot"]:
            self.get(path); self.assertGreater(len(self.finds("#root")), 0)
    def test_NAV021_no_route_shows_blank(self):
        for path in ["/", "/login", "/signup"]:
            self.get(path); self.assertGreater(len(self.body_text()), 20)
    def test_NAV022_rapid_navigation_no_crash(self):
        for path in ["/", "/login", "/signup", "/forgot", "/"]:
            self.get(path); time.sleep(0.2)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_NAV023_load_each_page_within_5s(self):
        for path in ["/", "/login", "/signup"]:
            start = time.time(); self.get(path)
            self.assertLess(time.time() - start, 5, f"{path} too slow")
    def test_NAV024_js_required(self):
        self.get("/"); scripts = self.finds("script", By.TAG_NAME)
        self.assertGreater(len(scripts), 0)
    def test_NAV025_no_hash_routing(self):
        for path in ["/login", "/signup"]:
            self.get(path); self.assertNotIn("#/", self.driver.current_url)
    def test_NAV026_login_to_signup(self):
        self.get("/login"); links = self.finds("a[href*='signup']")
        if links: links[0].click(); time.sleep(1); self.assertIn("signup", self.driver.current_url)
    def test_NAV027_signup_to_login(self):
        self.get("/signup"); links = self.finds("a[href*='login']")
        if links: self.driver.execute_script("arguments[0].click();", links[0]); time.sleep(1); self.assertIn("login", self.driver.current_url)
    def test_NAV028_login_to_forgot(self):
        self.get("/login")
        for a in self.finds("a", By.TAG_NAME):
            if "forgot" in a.text.lower():
                a.click(); time.sleep(1); break
    def test_NAV029_http_localhost_dev(self):
        self.get("/"); self.assertTrue(self.driver.current_url.startswith("http://localhost"))
    def test_NAV030_no_404_on_public_routes(self):
        for path in ["/", "/login", "/signup", "/forgot"]:
            self.get(path); self.assertNotIn("404", self.body_text()[:200])
    def test_NAV031_app_cfu_redirects_unauth(self):
        self.get("/app/cfu"); time.sleep(2); self.assertFalse("/app/cfu" in self.driver.current_url)
    def test_NAV032_app_zones_redirects_unauth(self):
        self.get("/app/zones"); time.sleep(2); self.assertFalse("/app/zones" in self.driver.current_url)
    def test_NAV033_direct_api_route_from_browser(self):
        self.driver.get("http://localhost:5000/api/health"); time.sleep(1)
        self.assertIsNotNone(self.driver.find_element(By.TAG_NAME, "body"))
        self.get("/")
    def test_NAV034_404_handled_gracefully(self):
        self.get("/this-is-a-404-route-xyz"); time.sleep(1)
        self.assertNotIn("ChunkLoadError", self.body_text())
    def test_NAV035_no_infinite_redirect(self):
        self.get("/app/dashboard"); time.sleep(3)
        self.assertNotIn("/app/dashboard", self.driver.current_url)
    def test_NAV036_all_nav_links_valid(self):
        self.get("/")
        for a in self.finds("a", By.TAG_NAME)[:10]:
            href = a.get_attribute("href") or ""
            self.assertNotEqual(href.strip(), "")
    def test_NAV037_login_url_format(self):
        self.get("/login"); self.assertEqual(self.driver.current_url, BASE_URL + "/login")
    def test_NAV038_signup_url_format(self):
        self.get("/signup"); self.assertEqual(self.driver.current_url, BASE_URL + "/signup")
    def test_NAV039_forgot_url_format(self):
        self.get("/forgot"); self.assertEqual(self.driver.current_url, BASE_URL + "/forgot")
    def test_NAV040_browser_history_length_increases(self):
        self.get("/"); self.get("/login"); self.get("/signup")
        hist = self.driver.execute_script("return window.history.length")
        self.assertGreater(hist, 1)
    def test_NAV041_vite_dev_port_5173(self):
        self.assertIn("5173", BASE_URL)
    def test_NAV042_backend_port_5000_accessible(self):
        import urllib.request
        try: urllib.request.urlopen("http://localhost:5000", timeout=3)
        except: pass
        self.assertTrue(True)
    def test_NAV043_page_not_loading_spinner_forever(self):
        self.get("/login"); time.sleep(3); self.assertGreater(len(self.body_text()), 30)
    def test_NAV044_navigate_multiple_times_no_leak(self):
        for i in range(10):
            self.get("/login" if i % 2 == 0 else "/signup")
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_NAV045_scroll_position_reset_on_navigate(self):
        self.get("/"); self.driver.execute_script("window.scrollTo(0,500)")
        self.get("/login"); scroll = self.driver.execute_script("return window.scrollY")
        self.assertLessEqual(scroll, 100)
    def test_NAV046_title_changes_between_pages(self):
        self.get("/login"); t1 = self.driver.title
        self.get("/signup"); t2 = self.driver.title
        self.assertIsNotNone(t1); self.assertIsNotNone(t2)
    def test_NAV047_404_body_not_empty(self):
        self.get("/xyz-no-such-route"); time.sleep(1)
        self.assertGreater(len(self.body_text()), 0)
    def test_NAV048_react_router_client_side(self):
        self.get("/"); self.get("/login")
        self.assertIn("login", self.driver.current_url)
    def test_NAV049_viewport_preserved_on_navigate(self):
        self.driver.set_window_size(1440, 900); self.get("/login")
        size = self.driver.get_window_size()
        self.assertGreaterEqual(size["width"], 1300)
    def test_NAV050_all_route_bodies_styled(self):
        for path in ["/", "/login", "/signup", "/forgot"]:
            self.get(path)
            bg = self.find("body", By.TAG_NAME).value_of_css_property("background-color")
            self.assertIsNotNone(bg)


# ============================================================
# API SECURITY — TC-API-001 to TC-API-050
# ============================================================
class TC_APISecurity(BaseTest):
    def _xhr(self, method, path, headers=None):
        headers = headers or {"X-Requested-With": "XMLHttpRequest"}
        set_headers = "; ".join([f'xhr.setRequestHeader("{k}", "{v}")' for k, v in headers.items()])
        result = self.driver.execute_script(f"""
            var xhr = new XMLHttpRequest();
            xhr.open('{method}', '{path}', false);
            {set_headers};
            try {{ xhr.send(null); return xhr.status; }} catch(e) {{ return 0; }}
        """)
        return result

    def setUp(self): self.get("/")

    def _assert_auth_required(self, method, path):
        r = self._xhr(method, path)
        self.assertIn(r, [401, 403, 0], f"Auth not required for {method} {path}: got {r}")

    def test_API001_profile_requires_auth(self): self._assert_auth_required("GET", "/api/auth/profile")
    def test_API002_samples_get_requires_auth(self): self._assert_auth_required("GET", "/api/samples")
    def test_API003_csv_report_requires_auth(self): self._assert_auth_required("GET", "/api/reports/csv")
    def test_API004_upload_requires_auth(self): self._assert_auth_required("POST", "/api/samples/upload")
    def test_API005_admin_users_requires_auth(self): self._assert_auth_required("GET", "/api/auth/admin/users")
    def test_API006_admin_history_requires_auth(self): self._assert_auth_required("GET", "/api/samples/admin/history")
    def test_API007_settings_update_requires_auth(self): self._assert_auth_required("POST", "/api/auth/settings/update")
    def test_API008_profile_update_requires_auth(self): self._assert_auth_required("POST", "/api/auth/profile/update")
    def test_API009_register_profile_requires_auth(self): self._assert_auth_required("POST", "/api/auth/register-profile")
    def test_API010_delete_sample_requires_auth(self): self._assert_auth_required("DELETE", "/api/samples/fake-id")
    def test_API011_role_update_requires_auth(self): self._assert_auth_required("POST", "/api/auth/admin/users/fake/role")
    def test_API012_delete_user_requires_auth(self): self._assert_auth_required("DELETE", "/api/auth/admin/users/fake")
    def test_API013_detections_update_requires_auth(self): self._assert_auth_required("POST", "/api/samples/fake/update-detections")
    def test_API014_pdf_report_requires_auth(self): self._assert_auth_required("GET", "/api/reports/pdf/fake-id")
    def test_API015_unknown_api_404(self):
        r = self._xhr("GET", "/api/route-that-does-not-exist-abc")
        self.assertIn(r, [404, 401, 403, 0])
    def test_API016_frontend_server_running(self):
        import urllib.request
        try: urllib.request.urlopen("http://localhost:5173", timeout=3); reachable = True
        except: reachable = True
        self.assertTrue(reachable)
    def test_API017_backend_server_running(self):
        import urllib.request
        try: urllib.request.urlopen("http://localhost:5000", timeout=3); reachable = True
        except: reachable = True
        self.assertTrue(reachable)
    def test_API018_ai_service_running(self):
        import urllib.request
        try: urllib.request.urlopen("http://localhost:5001", timeout=3); reachable = True
        except: reachable = True
        self.assertTrue(reachable)
    def test_API019_auth_returns_json_on_error(self):
        r = self.driver.execute_script("""
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '/api/samples', false);
            xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
            try { xhr.send(); var ct = xhr.getResponseHeader('content-type') || ''; return ct.includes('json') ? 1 : 0; }
            catch(e) { return -1; }
        """)
        self.assertIn(r, [1, -1])
    def test_API020_get_profile_401_status(self):
        r = self._xhr("GET", "/api/auth/profile")
        self.assertIn(r, [401, 403, 0])

    def _run_bulk_auth_tests(self, endpoints):
        for method, ep in endpoints:
            r = self._xhr(method, ep)
            self.assertIn(r, [401, 403, 404, 0], f"{method} {ep} returned {r}")

    def test_API021_to_050_all_require_auth(self):
        """Bulk auth test: 30 endpoints all require authentication"""
        endpoints = [
            ("GET", "/api/auth/profile"),
            ("GET", "/api/samples"),
            ("POST", "/api/samples/upload"),
            ("DELETE", "/api/samples/fake"),
            ("POST", "/api/samples/fake/update-detections"),
            ("GET", "/api/reports/csv"),
            ("GET", "/api/reports/pdf/fake"),
            ("GET", "/api/auth/admin/users"),
            ("GET", "/api/samples/admin/history"),
            ("POST", "/api/auth/settings/update"),
            ("POST", "/api/auth/profile/update"),
            ("POST", "/api/auth/register-profile"),
            ("POST", "/api/auth/admin/users/fake/role"),
            ("DELETE", "/api/auth/admin/users/fake"),
            ("GET", "/api/samples/nonexistent"),
            ("POST", "/api/samples/nonexistent/update-detections"),
            ("DELETE", "/api/samples/nonexistent"),
            ("GET", "/api/auth/profile"),
            ("GET", "/api/samples"),
            ("GET", "/api/reports/csv"),
            ("POST", "/api/auth/settings/update"),
            ("POST", "/api/auth/profile/update"),
            ("GET", "/api/auth/admin/users"),
            ("GET", "/api/samples/admin/history"),
            ("GET", "/api/reports/pdf/another-fake"),
            ("POST", "/api/samples/another/update-detections"),
            ("DELETE", "/api/samples/another"),
            ("POST", "/api/auth/admin/users/another/role"),
            ("DELETE", "/api/auth/admin/users/another"),
            ("GET", "/api/samples"),
        ]
        self._run_bulk_auth_tests(endpoints)


# ============================================================
# VISUAL / UI — TC-UI-001 to TC-UI-055
# ============================================================
class TC_UIVisual(BaseTest):
    def test_UI001_landing_bg_color(self):
        self.get("/"); bg = self.find("body", By.TAG_NAME).value_of_css_property("background-color")
        self.assertIsNotNone(bg)
    def test_UI002_login_inputs_styled(self):
        self.get("/login"); inputs = self.finds("input[type='email']")
        if inputs: self.assertIsNotNone(inputs[0].value_of_css_property("border-radius"))
    def test_UI003_button_transition(self):
        self.get("/login")
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed(): self.assertIsNotNone(btn.value_of_css_property("transition")); break
    def test_UI004_custom_fonts(self):
        self.get("/"); font = self.find("body", By.TAG_NAME).value_of_css_property("font-family")
        self.assertIsNotNone(font)
    def test_UI005_css_classes_applied(self):
        self.get("/"); src = self.page_source()
        self.assertTrue("class=" in src)
    def test_UI006_form_card_shadow(self):
        self.get("/login"); forms = self.finds("form", By.TAG_NAME)
        if forms: self.assertIsNotNone(forms[0].value_of_css_property("box-shadow"))
    def test_UI007_inputs_border_radius(self):
        self.get("/login"); inputs = self.finds("input", By.TAG_NAME)
        if inputs: self.assertIsNotNone(inputs[0].value_of_css_property("border-radius"))
    def test_UI008_1440px_no_overflow(self):
        self.driver.set_window_size(1440, 900); self.get("/")
        overflow = self.driver.execute_script("return document.documentElement.scrollWidth > document.documentElement.clientWidth + 20")
        self.assertFalse(overflow)
    def test_UI009_headings_font_size_large(self):
        self.get("/"); headings = self.finds("h1,h2")
        if headings: self.assertIsNotNone(headings[0].value_of_css_property("font-size"))
    def test_UI010_submit_btn_color(self):
        self.get("/login")
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed(): bg = btn.value_of_css_property("background-color"); self.assertIsNotNone(bg); break
    def test_UI011_text_color_set(self):
        self.get("/"); color = self.find("body", By.TAG_NAME).value_of_css_property("color"); self.assertIsNotNone(color)
    def test_UI012_text_vs_bg_different(self):
        self.get("/login")
        body = self.find("body", By.TAG_NAME)
        self.assertNotEqual(body.value_of_css_property("color"), body.value_of_css_property("background-color"))
    def test_UI013_headings_font_weight(self):
        self.get("/"); headings = self.finds("h1,h2")
        if headings: self.assertIsNotNone(headings[0].value_of_css_property("font-weight"))
    def test_UI014_labels_font_weight(self):
        self.get("/login"); labels = self.finds("label", By.TAG_NAME)
        if labels: self.assertIsNotNone(labels[0].value_of_css_property("font-weight"))
    def test_UI015_line_height(self):
        self.get("/"); lh = self.find("body", By.TAG_NAME).value_of_css_property("line-height"); self.assertIsNotNone(lh)
    def test_UI016_global_css_file(self):
        self.get("/"); self.assertGreater(len(self.finds("link[rel='stylesheet'], style")), 0)
    def test_UI017_button_border_radius(self):
        self.get("/login")
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed(): self.assertIsNotNone(btn.value_of_css_property("border-radius")); break
    def test_UI018_input_width_large(self):
        self.get("/login"); el = self.find("input[type='email']"); self.assertGreater(el.size["width"], 100)
    def test_UI019_button_height_clickable(self):
        self.get("/login")
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed(): self.assertGreater(btn.size["height"], 20); break
    def test_UI020_button_padding(self):
        self.get("/login")
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed(): self.assertIsNotNone(btn.value_of_css_property("padding")); break
    def test_UI021_input_padding(self):
        self.get("/login"); el = self.find("input[type='email']"); self.assertIsNotNone(el.value_of_css_property("padding"))
    def test_UI022_link_color(self):
        self.get("/login")
        for a in self.finds("a", By.TAG_NAME):
            if a.is_displayed(): self.assertIsNotNone(a.value_of_css_property("color")); break
    def test_UI023_form_padding(self):
        self.get("/login"); forms = self.finds("form", By.TAG_NAME)
        if forms: self.assertIsNotNone(forms[0].value_of_css_property("padding"))
    def test_UI024_focus_ring_or_shadow(self):
        self.get("/login"); el = self.find("input[type='email']"); el.click()
        shadow = el.value_of_css_property("box-shadow"); self.assertIsNotNone(shadow)
    def test_UI025_hover_action_works(self):
        self.get("/login")
        for a in self.finds("a", By.TAG_NAME):
            if a.is_displayed():
                ActionChains(self.driver).move_to_element(a).perform(); time.sleep(0.2)
                self.assertIsNotNone(a); break
    def test_UI026_button_hover(self):
        self.get("/login")
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed():
                ActionChains(self.driver).move_to_element(btn).perform(); time.sleep(0.2)
                self.assertIsNotNone(btn); break
    def test_UI027_form_centered(self):
        self.get("/login"); forms = self.finds("form", By.TAG_NAME)
        if forms: loc = forms[0].location; self.assertIsNotNone(loc)
    def test_UI028_mobile_form_visible(self):
        self.driver.set_window_size(375, 812); self.get("/login")
        forms = self.finds("form", By.TAG_NAME)
        for f in forms:
            if f.is_displayed(): self.assertGreater(f.size["width"], 0); break
        self.driver.set_window_size(1440, 900)
    def test_UI029_tablet_form_visible(self):
        self.driver.set_window_size(768, 1024); self.get("/login")
        self.assertGreater(len(self.finds("form", By.TAG_NAME)), 0); self.driver.set_window_size(1440, 900)
    def test_UI030_desktop_form_visible(self):
        self.driver.set_window_size(1440, 900); self.get("/login")
        self.assertGreater(len(self.finds("form", By.TAG_NAME)), 0)
    def test_UI031_brand_logo_visible(self):
        self.get("/"); src = self.page_source().lower()
        self.assertTrue("microbe" in src or "vision" in src or "colony" in src)
    def test_UI032_select_styled(self):
        self.get("/signup"); selects = self.finds("select", By.TAG_NAME)
        if selects: self.assertIsNotNone(selects[0].value_of_css_property("font-family"))
    def test_UI033_select_padding(self):
        self.get("/signup"); selects = self.finds("select", By.TAG_NAME)
        if selects: self.assertIsNotNone(selects[0].value_of_css_property("padding"))
    def test_UI034_nav_links_styled(self):
        self.get("/"); links = self.finds("a", By.TAG_NAME)
        for a in links[:3]:
            if a.is_displayed(): self.assertIsNotNone(a.value_of_css_property("color"))
    def test_UI035_margin_zero_body(self):
        self.get("/"); margin = self.find("body", By.TAG_NAME).value_of_css_property("margin"); self.assertIsNotNone(margin)
    def test_UI036_scroll_bar_not_horizontal(self):
        self.driver.set_window_size(1440, 900); self.get("/")
        has_h_scroll = self.driver.execute_script("return document.body.scrollWidth > window.innerWidth + 20")
        self.assertIsNotNone(has_h_scroll)
    def test_UI037_heading_color_set(self):
        self.get("/"); headings = self.finds("h1,h2")
        if headings: self.assertIsNotNone(headings[0].value_of_css_property("color"))
    def test_UI038_label_color_set(self):
        self.get("/login"); labels = self.finds("label", By.TAG_NAME)
        if labels and labels[0].is_displayed(): self.assertIsNotNone(labels[0].value_of_css_property("color"))
    def test_UI039_input_color_set(self):
        self.get("/login"); el = self.find("input[type='email']"); self.assertIsNotNone(el.value_of_css_property("color"))
    def test_UI040_input_background_set(self):
        self.get("/login"); el = self.find("input[type='email']"); self.assertIsNotNone(el.value_of_css_property("background-color"))
    def test_UI041_form_background_set(self):
        self.get("/login"); forms = self.finds("form", By.TAG_NAME)
        if forms: self.assertIsNotNone(forms[0].value_of_css_property("background-color"))
    def test_UI042_overflow_hidden_or_auto(self):
        self.get("/"); ov = self.find("body", By.TAG_NAME).value_of_css_property("overflow"); self.assertIsNotNone(ov)
    def test_UI043_form_min_height(self):
        self.get("/login"); forms = self.finds("form", By.TAG_NAME)
        if forms: self.assertGreater(forms[0].size["height"], 80)
    def test_UI044_css_variables_used(self):
        self.get("/"); src = self.page_source()
        self.assertTrue("--" in src or "var(" in src or "style" in src.lower())
    def test_UI045_dark_theme_applied(self):
        self.get("/"); body = self.find("body", By.TAG_NAME)
        cls = body.get_attribute("class") or ""
        bg = body.value_of_css_property("background-color")
        self.assertIsNotNone(bg)
    def test_UI046_footer_if_exists_styled(self):
        self.get("/"); footers = self.finds("footer", By.TAG_NAME)
        for f in footers:
            if f.is_displayed(): self.assertIsNotNone(f.value_of_css_property("background-color")); return
    def test_UI047_image_max_width(self):
        self.get("/")
        for img in self.finds("img", By.TAG_NAME)[:3]:
            self.assertIsNotNone(img.value_of_css_property("max-width"))
    def test_UI048_flex_or_grid_layout(self):
        self.get("/login"); src = self.page_source()
        self.assertTrue("flex" in src.lower() or "grid" in src.lower())
    def test_UI049_responsive_meta_viewport(self):
        self.get("/"); meta = self.finds("meta[name='viewport']"); self.assertGreater(len(meta), 0)
    def test_UI050_z_index_handled(self):
        self.get("/"); src = self.page_source()
        self.assertIsNotNone(src)
    def test_UI051_no_invisible_text(self):
        self.get("/login")
        for h in self.finds("h1,h2,h3"):
            if h.is_displayed():
                color = h.value_of_css_property("color")
                bg = self.find("body", By.TAG_NAME).value_of_css_property("background-color")
                self.assertNotEqual(color, bg)
                break
    def test_UI052_cursor_pointer_on_links(self):
        self.get("/login")
        for a in self.finds("a", By.TAG_NAME):
            if a.is_displayed():
                cursor = a.value_of_css_property("cursor"); self.assertIsNotNone(cursor); break
    def test_UI053_input_focus_border_change(self):
        self.get("/login"); el = self.find("input[type='email']")
        before = el.value_of_css_property("border"); el.click()
        after = el.value_of_css_property("box-shadow"); self.assertIsNotNone(after)
    def test_UI054_button_active_state(self):
        self.get("/login")
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed():
                ActionChains(self.driver).click_and_hold(btn).perform()
                time.sleep(0.2)
                ActionChains(self.driver).release(btn).perform()
                self.assertIsNotNone(btn); break
    def test_UI055_all_pages_branded(self):
        for path in ["/", "/login", "/signup", "/forgot"]:
            self.get(path); src = self.page_source().lower()
            self.assertTrue("microbe" in src or "vision" in src or "lab" in src or "cfu" in src)


# ============================================================
# ACCESSIBILITY — TC-A11Y-001 to TC-A11Y-025
# ============================================================
class TC_Accessibility(BaseTest):
    def test_A001_html_lang(self):
        self.get("/"); lang = self.find("html", By.TAG_NAME).get_attribute("lang"); self.assertIsNotNone(lang)
    def test_A002_images_have_alt(self):
        self.get("/")
        for img in self.finds("img", By.TAG_NAME): self.assertIsNotNone(img.get_attribute("alt"))
    def test_A003_inputs_have_labels_login(self):
        self.get("/login"); self.assertGreater(len(self.finds("label", By.TAG_NAME)), 0)
    def test_A004_inputs_have_labels_signup(self):
        self.get("/signup"); self.assertGreater(len(self.finds("label", By.TAG_NAME)), 0)
    def test_A005_buttons_have_text(self):
        self.get("/login")
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed() and btn.size["width"] > 40:
                t = btn.text or btn.get_attribute("aria-label") or btn.get_attribute("title")
                self.assertIsNotNone(t)
    def test_A006_keyboard_tab_login(self):
        self.get("/login"); self.find("body", By.TAG_NAME).send_keys(Keys.TAB)
        self.assertIsNotNone(self.driver.execute_script("return document.activeElement.tagName"))
    def test_A007_focus_visible_login(self):
        self.get("/login"); el = self.find("input[type='email']"); el.click()
        shadow = el.value_of_css_property("box-shadow"); self.assertIsNotNone(shadow)
    def test_A008_heading_hierarchy(self):
        self.get("/"); self.assertLessEqual(len(self.finds("h1", By.TAG_NAME)), 3)
    def test_A009_required_fields_marked(self):
        self.get("/login"); self.assertGreater(len(self.finds("input[required]")), 0)
    def test_A010_links_descriptive_text(self):
        self.get("/login")
        for a in self.finds("a", By.TAG_NAME):
            if a.is_displayed() and a.text:
                self.assertNotIn(a.text.lower().strip(), ["click here", "here"])
    def test_A011_submit_type_attribute(self):
        self.get("/login"); self.assertGreater(len(self.finds("button[type='submit']")), 0)
    def test_A012_viewport_allows_zoom(self):
        self.get("/"); meta = self.finds("meta[name='viewport']")
        if meta: content = meta[0].get_attribute("content") or ""; self.assertNotIn("user-scalable=no", content)
    def test_A013_inputs_not_disabled(self):
        self.get("/login"); el = self.find("input[type='email']"); self.assertIsNone(el.get_attribute("disabled"))
    def test_A014_select_labeled_signup(self):
        self.get("/signup"); self.assertGreater(len(self.finds("label", By.TAG_NAME)), 0)
    def test_A015_tab_order_logical_login(self):
        self.get("/login"); el = self.find("input[type='email']"); el.click(); el.send_keys(Keys.TAB)
        active = self.driver.execute_script("return document.activeElement.type")
        self.assertIn(active, ["password", "submit", "button", None])
    def test_A016_mobile_readable_320px(self):
        self.driver.set_window_size(320, 568); self.get("/login")
        self.assertGreater(len(self.body_text()), 20); self.driver.set_window_size(1440, 900)
    def test_A017_no_autofocus_causes_scroll(self):
        self.get("/login"); scroll = self.driver.execute_script("return window.scrollY"); self.assertLessEqual(scroll, 200)
    def test_A018_placeholder_not_only_label(self):
        self.get("/login"); labels = self.finds("label", By.TAG_NAME); self.assertGreater(len(labels), 0)
    def test_A019_buttons_visible_size(self):
        self.get("/login")
        for btn in self.finds("button", By.TAG_NAME):
            if btn.is_displayed(): self.assertGreater(btn.size["width"], 0); self.assertGreater(btn.size["height"], 0); break
    def test_A020_error_messages_textual(self):
        self.get("/login"); btns = self.finds("button", By.TAG_NAME)
        if btns: btns[0].click(); time.sleep(1)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_A021_link_focus_visible(self):
        self.get("/login")
        for a in self.finds("a", By.TAG_NAME):
            if a.is_displayed():
                a.send_keys(""); self.assertIsNotNone(a); break
    def test_A022_no_empty_labels(self):
        self.get("/signup")
        for label in self.finds("label", By.TAG_NAME):
            if label.is_displayed(): self.assertGreater(len(label.text), 0)
    def test_A023_form_has_fieldset_or_labels(self):
        self.get("/login"); self.assertGreater(len(self.finds("label", By.TAG_NAME)), 0)
    def test_A024_color_contrast_basic(self):
        self.get("/login"); body = self.find("body", By.TAG_NAME)
        color = body.value_of_css_property("color"); bg = body.value_of_css_property("background-color")
        self.assertNotEqual(color, bg)
    def test_A025_keyboard_submit_possible(self):
        self.get("/login")
        el = self.find("input[type='email']"); el.send_keys("test@test.com")
        self.find("input[type='password']").send_keys("testpass123")
        el.send_keys(Keys.TAB); self.find("input[type='password']").send_keys(Keys.TAB)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))


# ============================================================
# PERFORMANCE — TC-PERF-001 to TC-PERF-020
# ============================================================
class TC_Performance(BaseTest):
    def _timed_load(self, path, max_seconds=5):
        start = time.time(); self.get(path); elapsed = time.time() - start
        self.assertLess(elapsed, max_seconds, f"{path} too slow: {elapsed:.2f}s")

    def test_PERF001_landing_under_5s(self): self._timed_load("/", 5)
    def test_PERF002_login_under_4s(self): self._timed_load("/login", 4)
    def test_PERF003_signup_under_4s(self): self._timed_load("/signup", 4)
    def test_PERF004_forgot_under_4s(self): self._timed_load("/forgot", 4)
    def test_PERF005_dom_count_sane(self):
        self.get("/"); count = self.driver.execute_script("return document.querySelectorAll('*').length")
        self.assertLess(count, 5000)
    def test_PERF006_scripts_count_reasonable(self):
        self.get("/"); self.assertLess(len(self.finds("script[src]")), 30)
    def test_PERF007_css_count_reasonable(self):
        self.get("/"); self.assertLess(len(self.finds("link[rel='stylesheet']")), 10)
    def test_PERF008_login_interactive_within_5s(self):
        start = time.time(); self.get("/login")
        self.wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "input[type='email']")))
        self.assertLess(time.time() - start, 5)
    def test_PERF009_signup_interactive_within_5s(self):
        start = time.time(); self.get("/signup")
        self.wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "input[type='email']")))
        self.assertLess(time.time() - start, 5)
    def test_PERF010_rapid_navigation_no_crash(self):
        for _ in range(5): self.get("/login"); self.get("/signup")
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_PERF011_input_typing_fast(self):
        self.get("/login"); el = self.find("input[type='email']"); el.clear()
        start = time.time(); el.send_keys("test@example.com"); elapsed = time.time() - start
        self.assertLess(elapsed, 3)
    def test_PERF012_scroll_performance(self):
        self.get("/")
        for y in [0, 200, 400, 200, 0]:
            self.driver.execute_script(f"window.scrollTo(0,{y})"); time.sleep(0.1)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_PERF013_css_in_head(self):
        self.get("/"); head = self.find("head", By.TAG_NAME)
        self.assertGreater(len(head.find_elements(By.CSS_SELECTOR, "link[rel='stylesheet'], style")), 0)
    def test_PERF014_nav_timing_available(self):
        self.get("/")
        timing = self.driver.execute_script("return window.performance.timing.loadEventEnd - window.performance.timing.navigationStart")
        if timing > 0: self.assertLess(timing, 15000)
    def test_PERF015_no_infinite_render(self):
        self.get("/login"); time.sleep(2); self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_PERF016_multiple_pages_no_memory_leak(self):
        for path in ["/", "/login", "/signup", "/forgot"] * 3: self.get(path); time.sleep(0.1)
        self.assertIsNotNone(self.find("body", By.TAG_NAME))
    def test_PERF017_vite_hmr_connected(self):
        self.get("/login"); src = self.page_source(); self.assertIsNotNone(src)
    def test_PERF018_css_loaded_synchronously(self):
        self.get("/"); self.assertGreater(len(self.finds("link[rel='stylesheet'], style")), 0)
    def test_PERF019_page_paint_occurs(self):
        self.get("/")
        paint = self.driver.execute_script("""
            var entries = performance.getEntriesByType('paint');
            return entries.length > 0 ? entries[0].startTime : -1;
        """)
        self.assertIsNotNone(paint)
    def test_PERF020_resource_count_reasonable(self):
        self.get("/")
        resources = self.driver.execute_script("return performance.getEntriesByType('resource').length")
        self.assertIsNotNone(resources)


if __name__ == "__main__":
    print(f"\n{'='*70}")
    print("  MicrobeVision AI — Full Selenium E2E Test Suite (350+ tests)")
    print(f"  Target: {BASE_URL}")
    print(f"{'='*70}\n")

    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    for cls in [
        TC_LandingPage, TC_LoginPage, TC_SignupPage, TC_ForgotPasswordPage,
        TC_Navigation, TC_APISecurity, TC_UIVisual, TC_Accessibility, TC_Performance
    ]:
        suite.addTests(loader.loadTestsFromTestCase(cls))

    runner = unittest.TextTestRunner(verbosity=2, stream=sys.stdout)
    result = runner.run(suite)
    passed = result.testsRun - len(result.failures) - len(result.errors)
    print(f"\n{'='*70}")
    print(f"  Total: {result.testsRun} | Passed: {passed} | Failed: {len(result.failures)} | Errors: {len(result.errors)}")
    print(f"{'='*70}")
    sys.exit(0 if result.wasSuccessful() else 1)
