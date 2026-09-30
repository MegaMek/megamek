"""Focused read-only coordinator tests: python3 -m unittest discover -s scripts."""

import copy
import contextlib
import hashlib
import io
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import suite_release_plan as release

ROOT = Path(__file__).resolve().parents[1]
FIXTURE = ROOT / "megamek/testresources/suite-records/complete.json"


class PlanTests(unittest.TestCase):
    def setUp(self):
        self.old = json.loads(FIXTURE.read_text(encoding="utf-8"))
        self.commits = {
            repo: self.old["products"][product]["commit"]
            for product, repo in release.REPOS.items()
        }
        self.commits["mm-data"] = self.old["mmData"]["commit"]
        self.inventory = {
            "commits": self.commits, "previous": self.old,
            "tags": {repo: ["v0.51.100"] for repo in release.REPOS.values()},
            "releases": {repo: [] for repo in release.REPOS.values()},
            "membership": "weekly", "floor": "0.51.100",
        }

    def test_dependency_closure_and_reuse(self):
        self.assertEqual(release.select(self.commits, self.old, "weekly"),
                         ([], False))  # fixture is milestone
        self.old["membership"] = "weekly"
        self.assertEqual(release.select(self.commits, self.old, "weekly"), ([], True))
        self.assertEqual(release.select(self.commits, self.old, "milestone"), ([], False))
        self.commits["mekhq"] = "e" * 40
        self.assertEqual(release.select(self.commits, self.old, "weekly"), (["MekHQ"], False))
        self.commits["megameklab"] = "e" * 40
        self.assertEqual(release.select(self.commits, self.old, "weekly"),
                         (["MegaMekLab", "MekHQ"], False))
        self.commits["mm-data"] = "e" * 40
        self.assertEqual(release.select(self.commits, self.old, "weekly"),
                         (list(release.REPOS), False))

    def test_alias_and_fourth_component_reservations(self):
        tags = {repo: [] for repo in release.REPOS.values()}
        tags["megamek"] = ["v0.51.101.1"]
        tags["mekhq"] = ["v0.51.0102", "v0.51.9", "unrelated"]
        releases = {repo: [] for repo in release.REPOS.values()}
        self.assertEqual(release.reserve(tags, releases, "0.51.100"), "0.51.103")
        releases["megameklab"] = ["v0.51.104"]
        self.assertEqual(release.reserve(tags, releases, "0.51.103"), "0.51.105")
        tags["megamek"] = ["v0.51.2147483647"]
        self.assertEqual(release.reserve(tags, releases, "0.51.100"), "0.52.00")
        self.assertEqual(release.reserve(tags, releases, "0.51.100", "milestone"), "0.52.00")
        tags["megamek"] = ["v0.51.103"]
        self.assertEqual(release.reserve(tags, releases, "0.51.100", "milestone"), "0.52.00")
        tags["megamek"] = ["v0.52.00"]
        self.assertEqual(release.reserve(tags, releases, "0.51.100", "milestone"), "0.53.00")

    def test_provisional_not_success_or_noop(self):
        self.old["membership"] = "weekly"
        result = release.plan(self.inventory)
        self.assertEqual(result["status"], "PROVISIONAL_NONPUBLISHING")
        self.assertTrue(result["weeklyNoopCandidate"])
        self.assertEqual(result["versionCandidate"], "0.51.101")
        self.assertEqual(result["reuseCandidates"], list(release.REPOS))
        self.inventory["floor"] = "0.51.01"
        with self.assertRaises(release.UnsafeInventory):
            release.plan(self.inventory)

    def test_malformed_record_and_snapshot_fail_closed(self):
        for change in (
            lambda r: r["products"]["MegaMek"]["asset"].update(assetId=True),
            lambda r: r["products"]["MegaMekLab"].update(version="0.51.0100"),
            lambda r: r["products"]["MegaMek"].update(commit="not-a-sha"),
            lambda r: r["products"]["MekHQ"]["asset"].update(size=0),
            lambda r: r.update(membership="surprise"),
            lambda r: r["products"]["MekHQ"].update(extra=1),
        ):
            record = copy.deepcopy(self.old)
            change(record)
            with self.subTest(record=record), self.assertRaises(release.UnsafeInventory):
                release.validate_previous(record)
        with self.assertRaises(release.UnsafeInventory):
            release.parse_json(b'{"previous": null, "previous": null}')
        del self.inventory["commits"]["mm-data"]
        with self.assertRaises(release.UnsafeInventory):
            release.plan(self.inventory)

    def test_fake_cli_only_reads_and_rejects_bad_response(self):
        calls = []

        def fake_run(argv, **kwargs):
            calls.append(argv)
            return subprocess.CompletedProcess(argv, 0, b'{"sha":"' + b"a" * 40 + b'"}')

        result = release.gh_get("repos/MegaMek/megamek/commits/main", fake_run)
        self.assertEqual(result["sha"], "a" * 40)
        self.assertEqual(calls, [["gh", "api", "repos/MegaMek/megamek/commits/main"]])
        with self.assertRaises(release.UnsafeInventory):
            release.gh_get("repos/MegaMek/megamek/releases", fake_run)
        self.assertEqual(len(calls), 1)

        def failed_get(argv, **kwargs):
            raise subprocess.CalledProcessError(
                1, argv, stderr=b"error: HTTP 403 rate limit\n"
                b"Authorization: bearer github_pat_supersecret\n")

        with self.assertRaisesRegex(release.UnsafeInventory,
                                    "gh API GET .* failed: exit 1: error: HTTP 403") as caught:
            release.gh_get("repos/MegaMek/megamek/commits/main", failed_get)
        self.assertNotIn("supersecret", str(caught.exception))

        def fake_get(endpoint):
            if endpoint.endswith("/commits/main"):
                return {"sha": "a" * 40}
            if "/tags?" in endpoint:
                return [{"name": "v0.51.101"}]
            if "/releases?" in endpoint:
                return [{"id": 1, "tag_name": "v0.51.102", "draft": False, "assets": []}]
            raise AssertionError("unexpected page")

        snapshot = release.freeze_and_inventory("development", "0.51.100", fake_get)
        self.assertEqual(len(snapshot["commits"]), 4)
        self.assertEqual(release.plan(snapshot)["versionCandidate"], "0.51.103")
        with self.assertRaises(release.UnsafeInventory):
            release.freeze_and_inventory("weekly", "0.51.100",
                                         lambda endpoint: {"sha": "bad"})

    def test_exact_ref_getter_whitelist(self):
        calls = []

        def runner(argv, **kwargs):
            calls.append(argv)
            return subprocess.CompletedProcess(argv, 0, b'{"sha":"' + b"a" * 40 + b'"}')

        for endpoint in ("repos/MegaMek/megamek/git/ref/tags/v0.51.101",
                         "repos/MegaMek/mekhq/commits/v0.51.101",
                         "repos/MegaMek/mm-data/commits/main"):
            release.gh_get(endpoint, runner)
            self.assertEqual(calls[-1], ["gh", "api", endpoint])
        count = len(calls)
        for endpoint in ("repos/MegaMek/megamek/git/refs",
                         "repos/MegaMek/megamek/git/ref/tags/v0.51.0101",
                         "repos/MegaMek/megamek/commits/v0.51.1",
                         "repos/MegaMek/mm-data/git/ref/tags/v0.51.101",
                         "repos/MegaMek/unapproved/commits/main",
                         "repos/MegaMek/megamek/releases/1",
                         "repos/MegaMek/megamek/tags?per_page=100&page=0",
                         "--method POST repos/MegaMek/megamek/git/refs"):
            with self.subTest(endpoint=endpoint), self.assertRaises(release.UnsafeInventory):
                release.gh_get(endpoint, runner)
        self.assertEqual(len(calls), count)

    def test_cli_never_reports_success_even_for_a_valid_candidate(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "input.json"
            path.write_text(json.dumps(self.inventory), encoding="utf-8")
            old_argv = sys.argv
            try:
                sys.argv = ["suite_release_plan.py", "--inventory", str(path)]
                with contextlib.redirect_stdout(io.StringIO()) as stdout, \
                        contextlib.redirect_stderr(io.StringIO()) as stderr:
                    self.assertEqual(release.main(), 2)
                self.assertIn("PROVISIONAL_NONPUBLISHING", stdout.getvalue())
                self.assertIn("BLOCKED", stderr.getvalue())
            finally:
                sys.argv = old_argv

    def test_complete_record_and_byte_attestation(self):
        details = {repo: [] for repo in release.REPOS.values()}
        for product, repo in release.REPOS.items():
            item = self.old["products"][product]
            blob = product.encode()
            item["asset"]["size"] = len(blob)
            item["asset"]["sha256"] = hashlib.sha256(blob).hexdigest()
            details[repo].append({
                "id": item["releaseId"], "tag_name": item["tag"], "draft": False,
                "assets": [{"id": item["asset"]["assetId"], "name": item["asset"]["name"],
                            "size": len(blob), "state": "uploaded"}]})
        record_bytes = json.dumps(self.old).encode()
        suite_release = {
            "id": 999, "tag_name": self.old["tag"], "draft": False,
            "assets": [{"id": 998, "name": f"suite-record-{self.old['version']}.json",
                        "size": len(record_bytes), "state": "uploaded"}]}
        details["megamek"].append(suite_release)
        def fetch(repo, asset_id, destination):
            if asset_id == 998:
                destination.write_bytes(record_bytes)
            else:
                destination.write_bytes(next(name.encode() for name, r in release.REPOS.items()
                                       if r == repo and self.old["products"][name]["asset"]["assetId"] == asset_id))

        found = release.complete_record(details["megamek"], fetch)
        self.assertEqual(found, self.old)
        release.check_product_assets(found, details, fetch)
        def tampered(repo, asset_id, destination):
            destination.write_bytes(b"tampered")
        with self.assertRaises(release.UnsafeInventory):
            release.check_product_assets(found, details, tampered)
        suite_release["assets"].append(dict(suite_release["assets"][0]))
        with self.assertRaises(release.UnsafeInventory):
            release.complete_record(details["megamek"], fetch)
        suite_release["assets"].pop()
        suite_release["tag_name"] = "v0.51.99"
        with self.assertRaises(release.UnsafeInventory):
            release.complete_record(details["megamek"], fetch)
        suite_release["tag_name"] = self.old["tag"]
        suite_release["assets"][0]["size"] = release.MAX_RECORD_BYTES + 1
        with self.assertRaisesRegex(release.UnsafeInventory, "size limit"):
            release.complete_record(details["megamek"], fetch)
        suite_release["assets"][0]["size"] = len(record_bytes)
        details["mekhq"][0]["assets"][0]["state"] = "starter"
        with self.assertRaises(release.UnsafeInventory):
            release.check_product_assets(found, details, fetch)

    def test_draft_bootstrap_and_published_release_validation(self):
        drafts = [{"draft": True, "tag_name": None, "assets": None}]
        with self.assertRaisesRegex(release.UnsafeInventory, "bootstrap"):
            release.complete_record(drafts, lambda *args: self.fail("draft fetched"))
        self.assertIsNone(release.complete_record(drafts, lambda *args: self.fail("draft fetched"),
                                                   allow_missing=True))
        for invalid in (
            {"draft": False, "id": 1, "tag_name": None, "assets": []},
            {"draft": False, "id": 1, "tag_name": "v0.51.01", "assets": None},
            {"draft": "false", "id": 1, "tag_name": "v0.51.01", "assets": []},
        ):
            with self.subTest(invalid=invalid), self.assertRaises(release.UnsafeInventory):
                release.complete_record(drafts + [invalid])

    def test_explicit_remote_bootstrap_is_only_a_full_build_candidate(self):
        details = {repo: [] for repo in release.REPOS.values()}
        calls = []

        def getter(endpoint):
            calls.append(endpoint)
            if endpoint.endswith("/commits/main"):
                return {"sha": "a" * 40}
            if "/tags?" in endpoint:
                return [{"name": "v0.51.01"}]
            if "/releases?" in endpoint:
                return details[endpoint.split("/")[2]]
            self.fail(endpoint)

        with self.assertRaisesRegex(release.UnsafeInventory, "bootstrap"):
            release.discovered_inventory("weekly", getter)
        with self.assertRaisesRegex(release.UnsafeInventory, "floor"):
            release.discovered_inventory("weekly", getter, bootstrap=True)
        with self.assertRaisesRegex(release.UnsafeInventory, "canonical"):
            release.discovered_inventory("weekly", getter, bootstrap=True,
                                         bootstrap_floor="0.51.1")
        snapshot = release.discovered_inventory(
            "weekly", getter, bootstrap=True, bootstrap_floor="0.51.00")
        candidate = release.plan(snapshot)
        self.assertIsNone(snapshot["previous"])
        self.assertEqual(candidate["buildCandidates"], list(release.REPOS))
        self.assertEqual(candidate["reuseCandidates"], [])
        self.assertFalse(candidate["weeklyNoopCandidate"])
        self.assertEqual(candidate["versionCandidate"], "0.51.02")
        self.assertEqual(candidate["status"], "PROVISIONAL_NONPUBLISHING")
        with self.assertRaisesRegex(release.UnsafeInventory, "requires explicit bootstrap"):
            release.discovered_inventory("weekly", getter, bootstrap_floor="0.51.00")
        record = copy.deepcopy(self.old)
        raw = json.dumps(record).encode()
        details["megamek"].append({
            "id": 999, "tag_name": record["tag"], "draft": False,
            "assets": [{"id": 998, "name": f"suite-record-{record['version']}.json",
                        "size": len(raw), "state": "uploaded"}]})

        def fetch(repo, asset_id, destination):
            self.assertEqual((repo, asset_id), ("megamek", 998))
            destination.write_bytes(raw)

        with self.assertRaisesRegex(release.UnsafeInventory, "already exists"):
            release.discovered_inventory("weekly", getter, fetch, bootstrap=True,
                                         bootstrap_floor="0.51.00")

    def test_streamed_download_and_chunked_verification(self):
        with tempfile.TemporaryDirectory() as directory:
            destination = Path(directory) / "asset"
            calls = []

            class FakeProcess:
                stdout = io.BytesIO(b"chunk")

                def __enter__(self):
                    return self

                def __exit__(self, *args):
                    pass

                def wait(self):
                    return 0

            def runner(argv, **kwargs):
                calls.append((argv, kwargs))
                return FakeProcess()

            release.download_asset("megamek", 42, destination, runner)
            self.assertEqual(destination.read_bytes(), b"chunk")
            self.assertEqual(calls[0][1]["stdout"], subprocess.PIPE)
            self.assertNotEqual(calls[0][1]["stderr"], subprocess.DEVNULL)
            with self.assertRaises(release.UnsafeInventory):
                release.download_asset("mm-data", 42, destination, runner)

            class FailedProcess(FakeProcess):
                def wait(self):
                    return 22

            def failed_runner(argv, **kwargs):
                kwargs["stderr"].write(
                    b"error: HTTP 403 rate limit\nAuthorization: bearer github_pat_supersecret\n")
                return FailedProcess()

            with self.assertRaisesRegex(release.UnsafeInventory,
                                        "gh asset download megamek/42 failed: exit 22: "
                                        "error: HTTP 403 rate limit") as caught:
                release.download_asset("megamek", 42, destination, failed_runner)
            self.assertNotIn("supersecret", str(caught.exception))
            error = subprocess.CalledProcessError(
                1, ["gh", "api"], stderr=b"error: HTTP 403 rate limit\n"
                b"Authorization: bearer github_pat_supersecret\n")
            detail = release.failure_detail(error)
            self.assertIn("HTTP 403 rate limit", detail)
            self.assertNotIn("supersecret", detail)

        product = self.old["products"]["MegaMek"]
        product["asset"]["size"] = release.CHUNK_BYTES + 1
        product["asset"]["sha256"] = hashlib.sha256(b"x" * (release.CHUNK_BYTES + 1)).hexdigest()
        details = {repo: [] for repo in release.REPOS.values()}
        for name, repo in release.REPOS.items():
            item = self.old["products"][name]
            if name != "MegaMek":
                item["asset"]["size"] = 1
                item["asset"]["sha256"] = hashlib.sha256(b"x").hexdigest()
            details[repo] = [{"id": item["releaseId"], "tag_name": item["tag"],
                              "draft": False, "assets": [{
                                  "id": item["asset"]["assetId"], "name": item["asset"]["name"],
                                  "size": item["asset"]["size"], "state": "uploaded"}]}]

        def fetch(repo, asset_id, destination):
            with destination.open("wb") as stream:
                stream.write(b"x" * (release.CHUNK_BYTES + 1 if repo == "megamek" else 1))

        release.check_product_assets(self.old, details, fetch)
        self.old["products"]["MegaMek"]["asset"]["size"] -= 1
        with self.assertRaises(release.UnsafeInventory):
            release.check_product_assets(self.old, details, fetch)

    def test_discovery_pages_drafts_and_release_mismatch(self):
        record = self.old
        details = {}
        for name, repo in release.REPOS.items():
            item = record["products"][name]
            blob = name.encode()
            item["asset"]["size"] = len(blob)
            item["asset"]["sha256"] = hashlib.sha256(blob).hexdigest()
            details[repo] = [{"id": item["releaseId"], "tag_name": item["tag"],
                              "draft": False, "assets": [{
                                  "id": item["asset"]["assetId"], "name": item["asset"]["name"],
                                  "size": len(blob), "state": "uploaded"}]}]
        record_bytes = json.dumps(record).encode()
        details["megamek"].append({
            "id": 999, "tag_name": record["tag"], "draft": False,
            "assets": [{"id": 998, "name": f"suite-record-{record['version']}.json",
                        "size": len(record_bytes), "state": "uploaded"}]})
        details["megamek"].insert(0, {"draft": True, "tag_name": None})
        pages = []

        def getter(endpoint):
            pages.append(endpoint)
            if endpoint.endswith("/commits/main"):
                return {"sha": "a" * 40}
            repo = endpoint.split("/")[2]
            page = int(endpoint.rsplit("=", 1)[1])
            if "/tags?" in endpoint:
                return [{"name": "v0.51.01"}] if page == 1 else []
            if "/releases?" in endpoint:
                return details[repo] if page == 1 else []
            self.fail(endpoint)

        def fetch(repo, asset_id, destination):
            if asset_id == 998:
                destination.write_bytes(record_bytes)
            else:
                name = next(n for n, r in release.REPOS.items()
                            if r == repo and record["products"][n]["asset"]["assetId"] == asset_id)
                destination.write_bytes(name.encode())

        with tempfile.TemporaryDirectory() as directory:
            retained = Path(directory) / "prior"
            inventory = release.discovered_inventory(
                "weekly", getter, fetch, attest=lambda record, paths: None,
                retain=retained)
            for product in release.REPOS:
                self.assertEqual(
                    (retained / record["products"][product]["asset"]["name"]).read_bytes(),
                    product.encode())
        self.assertEqual(inventory["previous"], record)
        self.assertEqual(inventory["floor"], record["version"])
        self.assertNotIn(None, inventory["releases"]["megamek"])
        self.assertEqual(release.plan(inventory)["status"], "PROVISIONAL_NONPUBLISHING")
        details["megameklab"][0]["tag_name"] = "v0.51.99"
        with self.assertRaisesRegex(release.UnsafeInventory, "release identity"):
            release.discovered_inventory("weekly", getter, fetch,
                                         attest=lambda record, paths: None)
        details["megameklab"][0]["tag_name"] = record["products"]["MegaMekLab"]["tag"]
        details["megamek"].append({"draft": False, "id": 1000, "tag_name": None, "assets": []})
        with self.assertRaises(release.UnsafeInventory):
            release.discovered_inventory("weekly", getter, fetch,
                                         attest=lambda record, paths: None)

        def paged(endpoint):
            if "/releases?" in endpoint and "/megamek/" in endpoint:
                page = int(endpoint.rsplit("=", 1)[1])
                pages.append(endpoint)
                return ([{"draft": True, "tag_name": None}] * 100 if page == 1
                        else details["megamek"] if page == 2 else [])
            return getter(endpoint)

        del details["megamek"][-1]
        inventory = release.discovered_inventory("weekly", paged, fetch,
                                                  attest=lambda record, paths: None)
        self.assertIn("repos/MegaMek/megamek/releases?per_page=100&page=2", pages)
        self.assertEqual(inventory["previous"], record)


if __name__ == "__main__":
    unittest.main()
