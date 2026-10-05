"""Offline mock transaction tests; no GitHub or Gradle commands are invoked."""

import copy
import hashlib
import json
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import suite_release_coordinator as coordinator
import suite_release_publish as publish
from suite_release_plan import REPOS, SOURCES, UnsafeInventory, plan


class TransactionTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.commits = {repo: chr(97 + i) * 40
                        for i, repo in enumerate((*REPOS.values(), "mm-data"))}
        self.inventory = dict(commits=self.commits.copy(), previous=None, membership="weekly",
                              floor="0.51.00", tags={r: [] for r in REPOS.values()},
                              releases={r: [] for r in REPOS.values()})
        self.archives = {}
        self.events = []
        self.reads = []
        self.tags = {r: {} for r in REPOS.values()}
        self.releases = {r: [] for r in REPOS.values()}
        self.blobs = {}
        for product in REPOS:
            path = self.root / f"{product}-0.51.01.tar.gz"
            path.write_bytes(product.encode())
            self.archives[product] = path

    def get(self, endpoint):
        self.reads.append(endpoint)
        parts = endpoint.split("/")
        repo = parts[2]
        if endpoint.endswith("/commits/main"):
            return {"sha": self.commits[repo]}
        if "/commits/v" in endpoint:
            return {"sha": self.tags[repo][parts[-1]]}
        if "/git/ref/tags/" in endpoint:
            tag = parts[-1]
            return {"ref": "refs/tags/" + tag,
                    "object": {"type": "commit", "sha": self.tags[repo][tag]}}
        if "/tags?" in endpoint:
            page = int(endpoint.split("page=")[-1])
            return [{"name": tag} for tag in list(self.tags[repo])[(page - 1) * 100:page * 100]]
        if "/releases?" in endpoint:
            page = int(endpoint.split("page=")[-1])
            return self.releases[repo][(page - 1) * 100:page * 100]
        raise AssertionError(endpoint)

    def fetch(self, repo, asset_id, path):
        path.write_bytes(self.blobs[repo, asset_id])

    def tag(self, repo, tag, sha):
        self.events.append(("tag", repo))
        self.tags[repo][tag] = sha

    def release(self, repo, tag, sha):
        self.events.append(("release", repo))
        ident = 100 + len(self.events)
        self.releases[repo].append(dict(id=ident, tag_name=tag, draft=False, assets=[]))
        return ident

    def upload(self, repo, release_id, path):
        self.events.append(("upload", repo, Path(path).name))
        ident = 100 + len(self.events)
        data = Path(path).read_bytes()
        self.blobs[repo, ident] = data
        host = next(r for r in self.releases[repo] if r["id"] == release_id)
        host["assets"].append(dict(id=ident, name=Path(path).name,
                                   size=len(data), state="uploaded"))
        return ident

    def execute(self, **kwargs):
        return publish.publish(self.inventory, self.archives, getter=self.get,
                               fetch=self.fetch, writes=self,
                               attest=lambda record, paths: self.events.append(("attest",)),
                               validate=lambda path: self.events.append(("validate",)),
                               bootstrap=True, **kwargs)

    def test_record_last_and_versions(self):
        record = self.execute()
        self.assertEqual(list(record["products"]), list(REPOS))
        self.assertEqual([r["products"][p]["commit"] for p, r in
                          ((p, record) for p in REPOS)],
                         [self.commits[repo] for repo in REPOS.values()])
        self.assertEqual(self.events[-1], ("upload", "megamek", "suite-record-0.51.01.json"))
        self.assertEqual(len([e for e in self.events if e[0] == "upload"]), 4)
        self.assertLess(self.events.index(("validate",)), len(self.events) - 1)
        self.assertEqual(json.loads(self.blobs["megamek", self.releases["megamek"][0]
                                     ["assets"][-1]["id"]]), record)

    def test_tag_and_release_movement_have_no_writes(self):
        for kind in ("tag", "release"):
            with self.subTest(kind=kind):
                self.tags = {r: {} for r in REPOS.values()}
                self.releases = {r: [] for r in REPOS.values()}
                self.events.clear()
                if kind == "tag":
                    self.tags["mekhq"]["v0.51.01"] = self.commits["mekhq"]
                else:
                    self.releases["mekhq"].append(dict(id=5, tag_name="v0.51.01",
                                                       draft=True, assets=[]))
                with self.assertRaises(UnsafeInventory):
                    self.execute()
                self.assertFalse(any(e[0] in ("tag", "release", "upload") for e in self.events))

    def assert_frozen_record(self, record):
        for product, repo in REPOS.items():
            self.assertEqual(record["products"][product]["commit"], self.inventory["commits"][repo])
            self.assertEqual(self.tags[repo][record["products"][product]["tag"]],
                             self.inventory["commits"][repo])
        self.assertEqual(record["mmData"]["commit"], self.inventory["commits"]["mm-data"])
        self.assertFalse(any(endpoint.endswith("/commits/main") for endpoint in self.reads))
        self.assertEqual(self.events[-1],
                         ("upload", "megamek", f"suite-record-{record['version']}.json"))

    def test_all_channels_publish_frozen_sources_when_main_advances(self):
        for membership in ("weekly", "development", "milestone"):
            for source in SOURCES:
                with self.subTest(membership=membership, source=source):
                    self.setUp()
                    self.inventory["membership"] = membership
                    version = plan(self.inventory)["versionCandidate"]
                    for product in REPOS:
                        path = self.root / f"{product}-{version}.tar.gz"
                        path.write_bytes(product.encode())
                        self.archives[product] = path
                    self.commits[source] = "f" * 40
                    record = self.execute()
                    self.assertEqual(record["membership"], membership)
                    self.assert_frozen_record(record)

    def test_main_advancing_during_publication_keeps_frozen_record(self):
        for phase in ("first_tag", "last_product_upload"):
            with self.subTest(phase=phase):
                self.setUp()
                original_tag = self.tag
                original_upload = self.upload

                def advance():
                    self.commits.update({repo: "f" * 40 for repo in SOURCES})

                def tag(repo, name, sha):
                    original_tag(repo, name, sha)
                    if phase == "first_tag":
                        advance()

                def upload(repo, release_id, path):
                    result = original_upload(repo, release_id, path)
                    if phase == "last_product_upload" and Path(path).name.startswith("MekHQ-"):
                        advance()
                    return result

                with patch.object(self, "tag", side_effect=tag), patch.object(
                        self, "upload", side_effect=upload):
                    record = self.execute()
                self.assert_frozen_record(record)

    def prepare_noop(self):
        previous = self.execute()
        self.inventory["previous"] = copy.deepcopy(previous)
        self.inventory["floor"] = previous["version"]
        self.inventory["tags"] = {repo: list(tags) for repo, tags in self.tags.items()}
        self.inventory["releases"] = {
            repo: [entry["tag_name"] for entry in entries]
            for repo, entries in self.releases.items()}
        self.commits.update({repo: "f" * 40 for repo in SOURCES})
        self.events.clear()
        self.reads.clear()
        return previous

    def test_weekly_noop_uses_prepared_snapshot_after_main_advances(self):
        previous = self.prepare_noop()
        self.assertEqual(coordinator.verify_noop(
            self.inventory, self.archives, getter=self.get, fetch=self.fetch), previous)
        self.assertFalse(self.events)
        self.assertFalse(any(endpoint.endswith("/commits/main") for endpoint in self.reads))

    def test_weekly_noop_still_rejects_release_and_archive_changes(self):
        for kind in ("tag", "record", "asset", "local"):
            with self.subTest(kind=kind):
                self.setUp()
                previous = self.prepare_noop()
                if kind == "tag":
                    self.tags["megamek"][previous["tag"]] = "f" * 40
                elif kind == "record":
                    previous["membership"] = "development"
                    asset = self.releases["megamek"][0]["assets"][-1]
                    data = json.dumps(previous).encode()
                    self.blobs["megamek", asset["id"]] = data
                    asset["size"] = len(data)
                elif kind == "asset":
                    asset = previous["products"]["MekHQ"]["asset"]
                    self.blobs["mekhq", asset["assetId"]] = b"changed"
                else:
                    self.archives["MekHQ"].write_bytes(b"changed")
                with self.assertRaises(UnsafeInventory):
                    coordinator.verify_noop(
                        self.inventory, self.archives, getter=self.get, fetch=self.fetch)
                self.assertFalse(self.events)

    def test_missing_checks_or_failed_build_never_write(self):
        with self.assertRaises(UnsafeInventory):
            publish.publish(self.inventory, self.archives, getter=self.get,
                            fetch=self.fetch, writes=self, bootstrap=True)
        self.archives["MekHQ"].unlink()
        with self.assertRaises((UnsafeInventory, FileNotFoundError)):
            self.execute()
        self.assertFalse(any(e[0] in ("tag", "release", "upload") for e in self.events))

    def test_failed_record_validator_prevents_writes(self):
        def reject(path):
            raise UnsafeInventory("validator failed")

        with self.assertRaisesRegex(UnsafeInventory, "validator failed"):
            publish.publish(self.inventory, self.archives, getter=self.get,
                            fetch=self.fetch, writes=self, attest=lambda r, p: None,
                            validate=reject, bootstrap=True)
        self.assertEqual(self.releases, {r: [] for r in REPOS.values()})
        self.assertEqual(self.events, [])

    def prepare_reuse(self):
        previous = self.execute()
        self.inventory["previous"] = copy.deepcopy(previous)
        self.inventory["floor"] = previous["version"]
        self.inventory["tags"] = {repo: list(tags) for repo, tags in self.tags.items()}
        self.inventory["releases"] = {
            repo: [entry["tag_name"] for entry in entries]
            for repo, entries in self.releases.items()}
        self.commits["mekhq"] = "f" * 40
        self.inventory["commits"]["mekhq"] = self.commits["mekhq"]
        path = self.root / "MekHQ-0.51.02.tar.gz"
        path.write_bytes(b"new MekHQ")
        self.archives["MekHQ"] = path
        self.events.clear()
        return previous

    def test_reuse_megamek_product_and_separate_record_host(self):
        previous = self.prepare_reuse()
        record = publish.publish(self.inventory, self.archives, getter=self.get,
                                 fetch=self.fetch, writes=self,
                                 attest=lambda r, p: self.events.append(("attest",)),
                                 validate=lambda p: self.events.append(("validate",)))
        for product in ("MegaMek", "MegaMekLab"):
            self.assertEqual(record["products"][product], previous["products"][product])
            self.assertEqual(record["products"][product]["asset"]["sha256"],
                             hashlib.sha256(product.encode()).hexdigest())
        self.assertEqual(record["products"]["MekHQ"]["version"], "0.51.02")
        self.assertEqual([e[:2] for e in self.events if e[0] in ("tag", "release")],
                         [("tag", "mekhq"), ("release", "mekhq"),
                          ("tag", "megamek"), ("release", "megamek")])
        self.assertEqual([e for e in self.events if e[0] == "upload"],
                         [("upload", "mekhq", "MekHQ-0.51.02.tar.gz"),
                          ("upload", "megamek", "suite-record-0.51.02.json")])
        self.assertEqual(self.events[-1], ("upload", "megamek", "suite-record-0.51.02.json"))
        host = next(r for r in self.releases["megamek"] if r["tag_name"] == "v0.51.02")
        self.assertNotEqual(host["id"], record["products"]["MegaMek"]["releaseId"])
        self.assertEqual(json.loads(self.blobs["megamek", host["assets"][0]["id"]]), record)

    def test_milestone_record_only_reuses_three_assets(self):
        prior = self.execute()
        self.inventory["previous"] = copy.deepcopy(prior)
        self.inventory["floor"] = prior["version"]
        self.inventory["membership"] = "milestone"
        self.inventory["tags"] = {repo: list(tags) for repo, tags in self.tags.items()}
        self.inventory["releases"] = {
            repo: [r["tag_name"] for r in entries]
            for repo, entries in self.releases.items()}
        self.events.clear()
        record = publish.publish(self.inventory, self.archives, getter=self.get,
                                 fetch=self.fetch, writes=self,
                                 attest=lambda r, p: self.events.append(("attest",)),
                                 validate=lambda p: self.events.append(("validate",)))
        self.assertEqual(record["version"], "0.52.00")
        self.assertEqual(record["products"], prior["products"])
        self.assertEqual([e for e in self.events if e[0] == "upload"],
                         [("upload", "megamek", "suite-record-0.52.00.json")])

    def test_late_page_two_collision_blocks_product_and_suite_host(self):
        for host in (False, True):
            with self.subTest(suite_host=host):
                self.setUp()
                if host:
                    self.prepare_reuse()
                    repo = "megamek"
                    version = "v0.51.02"
                else:
                    repo = "megamek"
                    version = "v0.51.01"
                for index in range(100):
                    tag = f"unrelated-{index}"
                    self.tags[repo][tag] = self.commits[repo]
                    self.inventory["tags"][repo].append(tag)
                validated = 0
                def validate(path):
                    nonlocal validated
                    validated += 1
                def late_get(endpoint):
                    if (validated >= (2 if host else 1)
                            and endpoint == f"repos/MegaMek/{repo}/tags?per_page=100&page=2"):
                        return [{"name": version}]
                    return self.get(endpoint)
                self.events.clear()
                with self.assertRaisesRegex(UnsafeInventory, "collision"):
                    publish.publish(self.inventory, self.archives, getter=late_get,
                                    fetch=self.fetch, writes=self,
                                    attest=lambda r, p: None, validate=validate,
                                    bootstrap=not host)
                if host:
                    self.assertFalse(any(e[:2] == ("tag", "megamek") for e in self.events))
                    self.assertFalse(any(e[0] == "upload" and e[1] == "megamek"
                                         for e in self.events))
                else:
                    self.assertFalse(any(e[0] in ("tag", "release", "upload")
                                         for e in self.events))


class BindingTests(unittest.TestCase):
    def test_real_write_command_shapes_and_response_guards(self):
        calls = []
        def runner(args, **kwargs):
            calls.append((args, kwargs))
            return subprocess.CompletedProcess(args, 0, json.dumps(response[0]).encode())
        sha = "a" * 40
        with tempfile.TemporaryDirectory() as directory:
            archive = Path(directory) / "MekHQ-0.51.01.tar.gz"
            archive.write_bytes(b"archive")
            writes = publish.GitHubWrites()
            response = [{"ref": "refs/tags/v0.51.01", "object": {"sha": sha}}]
            original_api = publish.api
            with patch.object(publish, "api",
                              side_effect=lambda cmd, **kw: original_api(cmd, runner=runner, **kw)):
                writes.tag("mekhq", "v0.51.01", sha)
                self.assertEqual(calls[-1][0], ["gh", "api", "--method", "POST",
                                 "repos/MegaMek/mekhq/git/refs", "-f",
                                 "ref=refs/tags/v0.51.01", "-f", f"sha={sha}"])
                response[0] = {"id": 10, "tag_name": "v0.51.01", "draft": False}
                self.assertEqual(writes.release("mekhq", "v0.51.01", sha), 10)
                self.assertEqual(calls[-1][0], ["gh", "api", "--method", "POST",
                                 "repos/MegaMek/mekhq/releases", "-f", "tag_name=v0.51.01",
                                 "-f", f"target_commitish={sha}", "-F", "draft=false",
                                 "-F", "prerelease=false", "-f", "name=v0.51.01"])
                response[0] = {"id": 11, "name": archive.name, "state": "uploaded"}
                self.assertEqual(writes.upload("mekhq", 10, archive), 11)
                self.assertEqual(calls[-1][0], ["gh", "api", "--method", "POST",
                                 "https://uploads.github.com/repos/MegaMek/mekhq/releases/"
                                 "10/assets?name=MekHQ-0.51.01.tar.gz",
                                 "-H", "Content-Type: application/octet-stream",
                                 "--input", str(archive)])
                for operation, bad in ((lambda: writes.tag("mekhq", "v0.51.01", sha),
                                         {"ref": "refs/tags/other", "object": {"sha": sha}}),
                                        (lambda: writes.release("mekhq", "v0.51.01", sha),
                                         {"id": True, "tag_name": "v0.51.01", "draft": False}),
                                        (lambda: writes.upload("mekhq", 10, archive),
                                         {"id": 11, "name": archive.name, "state": "pending"})):
                    response[0] = bad
                    with self.assertRaises(UnsafeInventory):
                        operation()

    def test_approved_binding_callbacks_and_invalid_build_shape(self):
        tx = TransactionTests()
        tx.setUp()
        self.addCleanup(tx.temp.cleanup)
        proposal = publish.plan(tx.inventory)
        result = dict(status="OFFLINE_NONPUBLISHING",
                      versionCandidate=proposal["versionCandidate"],
                      productVersions={p: proposal["versionCandidate"] for p in REPOS},
                      archives=tx.archives)
        sources = tx.root / "sources"
        worktrees = {p: tx.root / REPOS[p] for p in REPOS}
        events = []
        def fake_publish(inventory, archives, **kwargs):
            record = {"record": True}
            kwargs["attest"](record, archives)
            path = tx.root / "suite-record.json"
            path.write_text("{}")
            kwargs["validate"](path)
            self.assertIs(kwargs["writes"], tx)
            self.assertEqual(kwargs["getter"], tx.get)
            self.assertEqual(kwargs["fetch"], tx.fetch)
            return record
        with (patch.object(publish, "stage", return_value=worktrees) as stage,
              patch.object(publish, "attest_archives",
                           side_effect=lambda *args, **kw: events.append(("attest", args, kw))),
              patch.object(publish, "clean",
                           side_effect=lambda *args: events.append(("clean", args))),
              patch.object(publish, "run",
                           side_effect=lambda *args, **kw: events.append(("run", args, kw))),
              patch.object(publish, "publish", side_effect=fake_publish) as bound):
            self.assertEqual(publish.publish_approved(tx.inventory, result, sources,
                             bootstrap=True, getter=tx.get, fetch=tx.fetch, writes=tx),
                             {"record": True})
            stage.assert_called_once()
            bound.assert_called_once()
            self.assertEqual(events[0], ("attest", ({"record": True}, tx.archives),
                                          {"worktrees": worktrees}))
            commands = [event for event in events if event[0] == "run"]
            self.assertEqual(len(commands), 1)
            self.assertIn(":megamek:validateSuiteRecord", commands[0][1][0])
            self.assertNotIn("--offline", commands[0][1][0])
            self.assertIn("--no-daemon", commands[0][1][0])
            self.assertIn(f"-PsuiteRecordFile={tx.root / 'suite-record.json'}",
                          commands[0][1][0])
            self.assertEqual(commands[0][2]["cwd"], worktrees["MegaMek"])
            self.assertEqual(len([e for e in events if e[0] == "clean"]), 2 * len(SOURCES))
            for bad in (dict(result, status="SUCCESS"),
                        dict(result, archives={}),
                        dict(result, versionCandidate="0.51.99"),
                        dict(result, productVersions={})):
                with self.subTest(bad=bad), self.assertRaises(UnsafeInventory):
                    publish.publish_approved(tx.inventory, bad, sources, bootstrap=True)
            stage.assert_called_once()

    def test_record_validator_dependency_failure_blocks_publish(self):
        tx = TransactionTests()
        tx.setUp()
        self.addCleanup(tx.temp.cleanup)
        proposal = publish.plan(tx.inventory)
        result = dict(status="OFFLINE_NONPUBLISHING",
                      versionCandidate=proposal["versionCandidate"],
                      productVersions={p: proposal["versionCandidate"] for p in REPOS},
                      archives=tx.archives)
        worktrees = {p: tx.root / REPOS[p] for p in REPOS}
        def fail(command, **kwargs):
            self.assertIn(":megamek:validateSuiteRecord", command)
            self.assertNotIn("--offline", command)
            self.assertIn("--no-daemon", command)
            raise subprocess.CalledProcessError(
                1, command, stderr="Could not resolve dependency: repository unavailable")
        def validate_only(inventory, archives, **kwargs):
            kwargs["validate"](tx.root / "suite-record.json")
            self.fail("validator failure must prevent publication")
        original_run = publish.run
        with (patch.object(publish, "stage", return_value=worktrees),
              patch.object(publish, "run",
                           side_effect=lambda command, **kw: original_run(
                               command, runner=fail, **kw)),
              patch.object(publish, "publish", side_effect=validate_only)):
            with self.assertRaisesRegex(UnsafeInventory, "Could not resolve dependency"):
                publish.publish_approved(tx.inventory, result, tx.root,
                                         bootstrap=True, writes=tx)
        self.assertFalse(tx.events)


if __name__ == "__main__":
    unittest.main()
