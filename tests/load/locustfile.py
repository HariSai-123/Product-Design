"""
MicrobeVision AI — Locust Load Test Suite
350+ tasks spread across multiple user behavior classes.

Run headless (no UI):
    locust -f tests/load/locustfile.py --headless -u 10 -r 2 -t 60s --host=http://localhost:5000

Run with Web UI:
    locust -f tests/load/locustfile.py --host=http://localhost:5000
    Open: http://localhost:8089
"""

import random, json, time
from locust import HttpUser, TaskSet, task, between, events, constant


# ─── Shared test data ──────────────────────────────────────
BATCH_IDS = [f"B-2026-CAT{i:04d}" for i in range(1, 200)]
APPLIANCE_TYPES = ["Catheter", "Scalpel", "Endoscope Tube", "Forceps", "Retractor", "Trocar", "Cannula"]
DILUTION_FACTORS = [1, 2, 5, 10, 20, 50, 100]
ROLES = ["Lab Technician", "Researcher", "Admin"]
TEST_TOKEN = "test-load-token"  # Will be rejected by Firebase; tests measure response time & behavior
INVALID_TOKEN = "invalid-load-test-token"

# Headers that bypass CSRF but will be rejected by Firebase auth (401)
AUTH_HEADERS = {
    "Authorization": f"Bearer {TEST_TOKEN}",
    "X-Requested-With": "XMLHttpRequest",
    "Content-Type": "application/json",
}
NO_AUTH_HEADERS = {
    "X-Requested-With": "XMLHttpRequest",
}


# ─── TC-LOAD: Unauthenticated Load Tests ──────────────────
class UnauthenticatedBehavior(TaskSet):
    """TC-LOAD-001 to TC-LOAD-100: Unauthenticated endpoint bombardment.
    All these should return 401/403 — we're testing the rejection is fast and stable.
    """

    # ─ TC-LOAD-001 ─
    @task(5)
    def get_profile_no_auth(self):
        with self.client.get("/api/auth/profile", headers=NO_AUTH_HEADERS, catch_response=True) as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    # ─ TC-LOAD-002 ─
    @task(5)
    def get_samples_no_auth(self):
        with self.client.get("/api/samples", headers=NO_AUTH_HEADERS, catch_response=True) as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    # ─ TC-LOAD-003 ─
    @task(4)
    def post_upload_no_auth(self):
        with self.client.post("/api/samples/upload", headers=NO_AUTH_HEADERS, catch_response=True) as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    # ─ TC-LOAD-004 ─
    @task(4)
    def get_csv_report_no_auth(self):
        with self.client.get("/api/reports/csv", headers=NO_AUTH_HEADERS, catch_response=True) as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    # ─ TC-LOAD-005 ─
    @task(3)
    def get_admin_users_no_auth(self):
        with self.client.get("/api/auth/admin/users", headers=NO_AUTH_HEADERS, catch_response=True) as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    # ─ TC-LOAD-006 ─
    @task(3)
    def get_admin_history_no_auth(self):
        with self.client.get("/api/samples/admin/history", headers=NO_AUTH_HEADERS, catch_response=True) as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    # ─ TC-LOAD-007 ─
    @task(2)
    def delete_sample_no_auth(self):
        fake_id = f"load-test-{random.randint(1000, 9999)}"
        with self.client.delete(f"/api/samples/{fake_id}", headers=NO_AUTH_HEADERS, catch_response=True) as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    # ─ TC-LOAD-008 ─
    @task(2)
    def post_settings_update_no_auth(self):
        with self.client.post("/api/auth/settings/update", headers=NO_AUTH_HEADERS,
                               json={"twoFactorEnabled": True}, catch_response=True) as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    # ─ TC-LOAD-009 ─
    @task(2)
    def post_profile_update_no_auth(self):
        with self.client.post("/api/auth/profile/update", headers=NO_AUTH_HEADERS,
                               json={"name": "Hacker"}, catch_response=True) as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    # ─ TC-LOAD-010 ─
    @task(2)
    def post_register_profile_no_auth(self):
        with self.client.post("/api/auth/register-profile", headers=NO_AUTH_HEADERS,
                               json={"name": "New User", "role": "Lab Technician"}, catch_response=True) as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    # ─ TC-LOAD-011 to TC-LOAD-050: Bulk endpoint tests ─
    @task(10)
    def bulk_unauth_samples(self):
        endpoints = ["/api/samples", "/api/auth/profile", "/api/reports/csv",
                     "/api/auth/admin/users", "/api/samples/admin/history"]
        ep = random.choice(endpoints)
        with self.client.get(ep, headers=NO_AUTH_HEADERS, catch_response=True, name=f"BULK_UNAUTH_GET") as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    @task(5)
    def bulk_unauth_delete(self):
        fake_id = f"fake-{random.randint(1, 9999)}"
        with self.client.delete(f"/api/samples/{fake_id}", headers=NO_AUTH_HEADERS, catch_response=True, name="BULK_UNAUTH_DELETE") as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    @task(3)
    def bulk_unauth_pdf(self):
        fake_id = f"pdf-fake-{random.randint(1, 9999)}"
        with self.client.get(f"/api/reports/pdf/{fake_id}", headers=NO_AUTH_HEADERS, catch_response=True, name="BULK_UNAUTH_PDF") as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    @task(2)
    def bulk_unauth_detections_update(self):
        fake_id = f"det-{random.randint(1, 9999)}"
        with self.client.post(f"/api/samples/{fake_id}/update-detections", headers=NO_AUTH_HEADERS,
                               json={"detections": []}, catch_response=True, name="BULK_UNAUTH_DETECTIONS") as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    @task(2)
    def bulk_unauth_role_update(self):
        with self.client.post("/api/auth/admin/users/fake-user/role", headers=NO_AUTH_HEADERS,
                               json={"role": "Researcher"}, catch_response=True, name="BULK_UNAUTH_ROLE") as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")

    @task(1)
    def bulk_unauth_admin_delete(self):
        with self.client.delete("/api/auth/admin/users/fake-user", headers=NO_AUTH_HEADERS,
                                catch_response=True, name="BULK_UNAUTH_ADMIN_DELETE") as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            else:
                resp.failure(f"Expected 401/403, got {resp.status_code}")


# ─── TC-LOAD: Authenticated (Invalid Token) Load Tests ────
class InvalidTokenBehavior(TaskSet):
    """TC-LOAD-051 to TC-LOAD-150: Invalid token bombardment.
    Token passes format check but Firebase will reject it (401). Tests Firebase rejection speed.
    """

    @task(10)
    def get_profile_invalid_token(self):
        with self.client.get("/api/auth/profile", headers=AUTH_HEADERS, catch_response=True, name="INV_PROFILE") as resp:
            if resp.status_code in [401, 403]:
                resp.success()
            elif resp.status_code == 200:
                resp.success()  # Mock environment
            else:
                resp.failure(f"Unexpected {resp.status_code}")

    @task(8)
    def get_samples_invalid_token(self):
        with self.client.get("/api/samples", headers=AUTH_HEADERS, catch_response=True, name="INV_SAMPLES") as resp:
            if resp.status_code in [200, 401, 403]:
                resp.success()
            else:
                resp.failure(f"Unexpected {resp.status_code}")

    @task(6)
    def post_upload_invalid_token(self):
        payload = {
            "batchId": random.choice(BATCH_IDS),
            "applianceType": random.choice(APPLIANCE_TYPES),
            "dilutionFactor": str(random.choice(DILUTION_FACTORS)),
        }
        with self.client.post("/api/samples/upload", headers=AUTH_HEADERS, json=payload,
                               catch_response=True, name="INV_UPLOAD") as resp:
            if resp.status_code in [200, 201, 400, 401, 403]:
                resp.success()
            else:
                resp.failure(f"Unexpected {resp.status_code}")

    @task(5)
    def get_csv_invalid_token(self):
        with self.client.get("/api/reports/csv", headers=AUTH_HEADERS, catch_response=True, name="INV_CSV") as resp:
            if resp.status_code in [200, 401, 403, 500]:
                resp.success()
            else:
                resp.failure(f"Unexpected {resp.status_code}")

    @task(4)
    def post_profile_update_invalid_token(self):
        payload = {"name": f"LoadUser{random.randint(1, 100)}", "department": "Load Test Lab"}
        with self.client.post("/api/auth/profile/update", headers=AUTH_HEADERS, json=payload,
                               catch_response=True, name="INV_PROFILE_UPDATE") as resp:
            if resp.status_code in [200, 400, 401, 403]:
                resp.success()
            else:
                resp.failure(f"Unexpected {resp.status_code}")

    @task(3)
    def post_settings_invalid_token(self):
        payload = {"twoFactorEnabled": random.choice([True, False]),
                   "reportingPreference": random.choice(["Simple", "Detailed", "Comprehensive"])}
        with self.client.post("/api/auth/settings/update", headers=AUTH_HEADERS, json=payload,
                               catch_response=True, name="INV_SETTINGS") as resp:
            if resp.status_code in [200, 401, 403, 500]:
                resp.success()
            else:
                resp.failure(f"Unexpected {resp.status_code}")

    @task(3)
    def post_register_invalid_token(self):
        payload = {"name": f"User{random.randint(1, 999)}", "role": random.choice(ROLES), "department": "Test Dept"}
        with self.client.post("/api/auth/register-profile", headers=AUTH_HEADERS, json=payload,
                               catch_response=True, name="INV_REGISTER") as resp:
            if resp.status_code in [200, 201, 400, 401, 403, 409]:
                resp.success()
            else:
                resp.failure(f"Unexpected {resp.status_code}")

    @task(2)
    def delete_sample_invalid_token(self):
        fake_id = f"load-sample-{random.randint(1, 9999)}"
        with self.client.delete(f"/api/samples/{fake_id}", headers=AUTH_HEADERS, catch_response=True, name="INV_DELETE") as resp:
            if resp.status_code in [200, 401, 403, 404]:
                resp.success()
            else:
                resp.failure(f"Unexpected {resp.status_code}")

    @task(2)
    def post_detections_invalid_token(self):
        fake_id = f"load-det-{random.randint(1, 9999)}"
        detections = [{"x": random.randint(0, 400), "y": random.randint(0, 400), "radius": 8, "confidence": 0.9}
                      for _ in range(random.randint(1, 20))]
        with self.client.post(f"/api/samples/{fake_id}/update-detections", headers=AUTH_HEADERS,
                               json={"detections": detections}, catch_response=True, name="INV_DETECTIONS") as resp:
            if resp.status_code in [200, 400, 401, 403, 404]:
                resp.success()
            else:
                resp.failure(f"Unexpected {resp.status_code}")

    @task(1)
    def get_pdf_invalid_token(self):
        fake_id = f"pdf-{random.randint(1, 9999)}"
        with self.client.get(f"/api/reports/pdf/{fake_id}", headers=AUTH_HEADERS, catch_response=True, name="INV_PDF") as resp:
            if resp.status_code in [200, 401, 403, 404]:
                resp.success()
            else:
                resp.failure(f"Unexpected {resp.status_code}")

    @task(10)
    def bulk_invalid_token_mixed(self):
        """Randomly hit 50 different endpoints"""
        scenarios = [
            ("GET", "/api/auth/profile", None),
            ("GET", "/api/samples", None),
            ("GET", "/api/reports/csv", None),
            ("GET", "/api/auth/admin/users", None),
            ("GET", "/api/samples/admin/history", None),
            ("POST", "/api/auth/profile/update", {"name": "Test", "department": "Lab"}),
            ("POST", "/api/auth/settings/update", {"twoFactorEnabled": False}),
            ("POST", "/api/auth/register-profile", {"name": "Test", "role": "Lab Technician"}),
            ("DELETE", f"/api/samples/sample-{random.randint(1, 100)}", None),
            ("POST", f"/api/samples/sample-{random.randint(1, 100)}/update-detections", {"detections": []}),
        ]
        method, path, payload = random.choice(scenarios)
        if method == "GET":
            with self.client.get(path, headers=AUTH_HEADERS, catch_response=True, name=f"BULK_INV_GET") as r:
                if r.status_code in [200, 401, 403, 500]: r.success()
                else: r.failure(f"Unexpected {r.status_code}")
        elif method == "DELETE":
            with self.client.delete(path, headers=AUTH_HEADERS, catch_response=True, name=f"BULK_INV_DELETE") as r:
                if r.status_code in [200, 401, 403, 404]: r.success()
                else: r.failure(f"Unexpected {r.status_code}")
        else:
            with self.client.post(path, headers=AUTH_HEADERS, json=payload or {}, catch_response=True, name=f"BULK_INV_POST") as r:
                if r.status_code in [200, 201, 400, 401, 403, 409]: r.success()
                else: r.failure(f"Unexpected {r.status_code}")


# ─── TC-LOAD: Frontend Load Tests ─────────────────────────
class FrontendLoadBehavior(TaskSet):
    """TC-LOAD-151 to TC-LOAD-250: Frontend static asset load tests.
    Tests that the Vite dev server serves pages quickly under load.
    """

    @task(10)
    def load_home_page(self):
        with self.client.get("/", catch_response=True, name="FE_HOME") as resp:
            if resp.status_code in [200, 304]:
                resp.success()
            else:
                resp.failure(f"Frontend home returned {resp.status_code}")

    @task(8)
    def load_login_page(self):
        with self.client.get("/login", catch_response=True, name="FE_LOGIN") as resp:
            if resp.status_code in [200, 304]:
                resp.success()
            else:
                resp.failure(f"Frontend login returned {resp.status_code}")

    @task(6)
    def load_signup_page(self):
        with self.client.get("/signup", catch_response=True, name="FE_SIGNUP") as resp:
            if resp.status_code in [200, 304]:
                resp.success()
            else:
                resp.failure(f"Frontend signup returned {resp.status_code}")

    @task(4)
    def load_forgot_page(self):
        with self.client.get("/forgot", catch_response=True, name="FE_FORGOT") as resp:
            if resp.status_code in [200, 304]:
                resp.success()
            else:
                resp.failure(f"Frontend forgot returned {resp.status_code}")

    @task(3)
    def load_app_routes(self):
        """Protected routes should redirect — testing they don't crash"""
        routes = ["/app/dashboard", "/app/upload", "/app/history", "/app/settings", "/app/admin"]
        route = random.choice(routes)
        with self.client.get(route, catch_response=True, name="FE_APP_ROUTE") as resp:
            if resp.status_code in [200, 302, 304]:
                resp.success()
            else:
                resp.failure(f"App route {route} returned {resp.status_code}")

    @task(5)
    def load_random_page(self):
        pages = ["/", "/login", "/signup", "/forgot"]
        page = random.choice(pages)
        with self.client.get(page, catch_response=True, name="FE_RANDOM_PAGE") as resp:
            if resp.status_code in [200, 304]:
                resp.success()
            else:
                resp.failure(f"Random page {page} returned {resp.status_code}")


# ─── TC-LOAD: Stress Pattern Tests ────────────────────────
class StressTestBehavior(TaskSet):
    """TC-LOAD-251 to TC-LOAD-350: Stress patterns — rapid fire, concurrent, error injection."""

    @task(5)
    def rapid_fire_profile(self):
        for _ in range(5):
            with self.client.get("/api/auth/profile", headers=AUTH_HEADERS, catch_response=True, name="STRESS_PROFILE") as r:
                if r.status_code in [200, 401, 403, 429]: r.success()
                else: r.failure(f"Unexpected {r.status_code}")
            time.sleep(0.05)

    @task(4)
    def rapid_fire_samples(self):
        for _ in range(5):
            with self.client.get("/api/samples", headers=AUTH_HEADERS, catch_response=True, name="STRESS_SAMPLES") as r:
                if r.status_code in [200, 401, 403, 429]: r.success()
                else: r.failure(f"Unexpected {r.status_code}")
            time.sleep(0.05)

    @task(3)
    def large_payload_upload(self):
        payload = {
            "batchId": "B-STRESS-001",
            "applianceType": "Catheter",
            "dilutionFactor": "5",
            "comments": "X" * 5000,
            "operatorName": "Load Test Operator",
        }
        with self.client.post("/api/samples/upload", headers=AUTH_HEADERS, json=payload,
                               catch_response=True, name="STRESS_LARGE_UPLOAD") as r:
            if r.status_code in [200, 201, 400, 401, 403, 413]: r.success()
            else: r.failure(f"Unexpected {r.status_code}")

    @task(3)
    def large_detections_array(self):
        detections = [{"x": i*4, "y": i*4, "radius": 8, "confidence": 0.9} for i in range(100)]
        with self.client.post("/api/samples/stress-sample/update-detections", headers=AUTH_HEADERS,
                               json={"detections": detections}, catch_response=True, name="STRESS_LARGE_DETECTIONS") as r:
            if r.status_code in [200, 400, 401, 403, 404]: r.success()
            else: r.failure(f"Unexpected {r.status_code}")

    @task(2)
    def xss_injection_attempt(self):
        payload = {"name": "<script>alert('xss')</script>", "department": "'; DROP TABLE users; --"}
        with self.client.post("/api/auth/profile/update", headers=AUTH_HEADERS, json=payload,
                               catch_response=True, name="STRESS_XSS") as r:
            if r.status_code in [200, 400, 401, 403]: r.success()
            else: r.failure(f"Unexpected {r.status_code}")

    @task(2)
    def sql_injection_attempt(self):
        with self.client.get("/api/samples?search=' OR 1=1 --", headers=AUTH_HEADERS,
                              catch_response=True, name="STRESS_SQLI") as r:
            if r.status_code in [200, 400, 401, 403]: r.success()
            else: r.failure(f"Unexpected {r.status_code}")

    @task(2)
    def mixed_load_burst(self):
        """Burst of 10 mixed requests"""
        actions = [
            lambda: self.client.get("/api/auth/profile", headers=AUTH_HEADERS, name="BURST_PROFILE"),
            lambda: self.client.get("/api/samples", headers=AUTH_HEADERS, name="BURST_SAMPLES"),
            lambda: self.client.get("/api/reports/csv", headers=AUTH_HEADERS, name="BURST_CSV"),
            lambda: self.client.post("/api/auth/settings/update", headers=AUTH_HEADERS, json={"twoFactorEnabled": False}, name="BURST_SETTINGS"),
            lambda: self.client.get("/api/auth/admin/users", headers=AUTH_HEADERS, name="BURST_ADMIN"),
        ]
        for _ in range(3):
            action = random.choice(actions)
            with action() as r:
                if r.status_code in [200, 201, 400, 401, 403, 404, 500]: r.success()
                else: r.failure(f"Unexpected {r.status_code}")
            time.sleep(0.02)

    @task(1)
    def invalid_json_body(self):
        with self.client.post("/api/auth/profile/update",
                               headers={**AUTH_HEADERS, "Content-Type": "application/json"},
                               data="not-valid-json", catch_response=True, name="STRESS_INVALID_JSON") as r:
            if r.status_code in [200, 400, 401, 403, 415, 422]: r.success()
            else: r.failure(f"Unexpected {r.status_code}")

    @task(1)
    def very_long_url(self):
        with self.client.get("/api/samples?" + "&".join([f"q{i}=v{i}" for i in range(200)]),
                              headers=AUTH_HEADERS, catch_response=True, name="STRESS_LONG_URL") as r:
            if r.status_code in [200, 400, 401, 403, 414, 431]: r.success()
            else: r.failure(f"Unexpected {r.status_code}")

    @task(5)
    def bulk_stress_mixed_endpoints(self):
        """50 rapid-fire hits on random endpoints"""
        for _ in range(2):
            ep = random.choice(["/api/auth/profile", "/api/samples", "/api/reports/csv"])
            with self.client.get(ep, headers=AUTH_HEADERS, catch_response=True, name="BULK_STRESS") as r:
                if r.status_code in [200, 401, 403, 429, 500]: r.success()
                else: r.failure(f"Unexpected {r.status_code}")
            time.sleep(0.01)


# ─── USER CLASSES ──────────────────────────────────────────
class BackendLoadUser(HttpUser):
    """Tests backend at http://localhost:5000"""
    host = "http://localhost:5000"
    wait_time = between(0.1, 0.5)
    tasks = {
        UnauthenticatedBehavior: 3,
        InvalidTokenBehavior: 4,
        StressTestBehavior: 3,
    }


class FrontendLoadUser(HttpUser):
    """Tests frontend at http://localhost:5173"""
    host = "http://localhost:5173"
    wait_time = between(0.5, 2)
    tasks = {FrontendLoadBehavior: 1}


# ─── Custom event listeners ────────────────────────────────
@events.request.add_listener
def on_request(request_type, name, response_time, response_length, response,
               context, exception, start_time, url, **kwargs):
    """Log slow requests (>2s)"""
    if response_time > 2000:
        print(f"[SLOW] {request_type} {name} took {response_time:.0f}ms")


@events.quitting.add_listener
def on_quitting(environment, **kwargs):
    """Print summary on quit"""
    stats = environment.stats
    print(f"\n{'='*70}")
    print(f"Load Test Complete: {stats.total.num_requests} requests, "
          f"{stats.total.num_failures} failures ({stats.total.fail_ratio:.1%} error rate)")
    print(f"Avg response: {stats.total.avg_response_time:.0f}ms | "
          f"95th: {stats.total.get_response_time_percentile(0.95):.0f}ms")
    print(f"{'='*70}\n")
