"""Offline fake-runner coverage; never invokes Git or Gradle."""

import json
import hashlib
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import suite_pinned_build as pinned
from suite_release_plan import UnsafeInventory


class PinnedBuildTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.sources = self.root / "sources"
        self.sources.mkdir()
        for repo in pinned.SOURCES:
            (self.sources / repo / ".git").mkdir(parents=True)
        self.output = self.root / "result"
        self.record = json.loads((Path(__file__).resolve().parents[1] /
                                  "megamek/testresources/suite-records/complete.json").read_text())
        self.commits = {repo: self.record["products"][product]["commit"]
                        for product, repo in pinned.REPOS.items()}
        self.commits["mm-data"] = self.record["mmData"]["commit"]
        self.inventory = dict(commits=self.commits, previous=None, membership="weekly",
                              floor=self.record["version"],
                              tags={repo: [] for repo in pinned.REPOS.values()},
                              releases={repo: [] for repo in pinned.REPOS.values()})
        self.calls = []

    def fake(self, command, **kwargs):
        self.calls.append((command, kwargs))
        if command[:2] == ["git", "clone"]:
            Path(command[-1]).mkdir()
            (Path(command[-1]) / "gradlew").touch()
            (Path(command[-1]) / "gradlew.bat").touch()
        if any(command[1] == tasks[1] for tasks in pinned.TASKS.values()):
            product = next(p for p, tasks in pinned.TASKS.items() if tasks[1] in command)
            repo = pinned.REPOS[product]
            version = next(flag.split("=", 1)[1] for flag in command
                           if flag.startswith("-PsuiteReleaseVersion="))
            # Model the Gradle project output independently of the builder's lookup.
            project_dir = {"MegaMek": "megamek", "MegaMekLab": "megameklab",
                           "MekHQ": "MekHQ"}[product]
            path = kwargs["cwd"] / project_dir / "build" / "distributions" / (
                f"{product}-{version}.tar.gz")
            path.parent.mkdir(parents=True)
            path.write_bytes(b"fake archive")
        if command[:2] == ["git", "-C"]:
            if "remote" in command and "get-url" in command:
                return subprocess.CompletedProcess(command, 0,
                                                   stdout=pinned.official(Path(command[2]).name))
            if "rev-parse" in command:
                value = command[-1].removesuffix("^{commit}")
                if value == "HEAD":
                    value = self.commits[Path(command[2]).name]
                return subprocess.CompletedProcess(command, 0, stdout=value)
        return subprocess.CompletedProcess(command, 0, stdout="")

    def identity(self, path, product, version):
        meta = dict(schemaVersion="1", product=product, version=version,
                    minimumJavaVersion="21", mmDataCommit=self.commits["mm-data"],
                    megamekCommit=self.commits["megamek"],
                    megameklabCommit=self.commits["megameklab"],
                    mekhqCommit=self.commits["mekhq"],
                    megamekVersion=version, megameklabVersion=version)
        return meta, {product + ".jar": b"jar", "lib/MegaMek.jar": b"jar",
                      "lib/MegaMekLab.jar": b"jar"}

    def test_bootstrap_exact_shas_order_and_flags(self):
        with patch.object(pinned, "archive_identity", side_effect=self.identity):
            result = pinned.build(self.inventory, self.sources, {}, self.output,
                                  bootstrap=True, runner=self.fake)
        self.assertEqual(list(result["archives"]), list(pinned.REPOS))
        self.assertEqual(len([c for c, _ in self.calls if c[:2] == ["git", "clone"]]), 4)
        checkouts = [c for c, _ in self.calls if "checkout" in c]
        self.assertEqual([c[-1] for c in checkouts], list(self.commits.values()))
        self.assertTrue(all("--detach" in c and "main" not in c for c in checkouts))
        gradle = [c for c, _ in self.calls if c[0].endswith(("gradlew", "gradlew.bat"))]
        self.assertEqual([c[1] for c in gradle],
                         [task for tasks in pinned.TASKS.values() for task in tasks])
        for command in gradle:
            self.assertNotIn("--offline", command)
            self.assertIn("--no-daemon", command)
            for key, sha in (("suiteMegaMekCommit", self.commits["megamek"]),
                             ("suiteMegaMekLabCommit", self.commits["megameklab"]),
                             ("suiteMekHQCommit", self.commits["mekhq"]),
                             ("suiteMmDataCommit", self.commits["mm-data"])):
                self.assertIn(f"-P{key}={sha}", command)
        self.assertTrue(any(c.startswith("-PsuiteMegaMekArchiveFile=") for c in gradle[-1]))
        self.assertTrue(any(c.startswith("-PsuiteMegaMekLabArchiveFile=") for c in gradle[-1]))
        hq_verify = next((c, kw) for c, kw in self.calls
                         if c[1] == pinned.TASKS["MekHQ"][2])
        hq_archive = next(Path(flag.split("=", 1)[1]) for flag in hq_verify[0]
                          if flag.startswith("-PsuiteArchiveFile="))
        self.assertEqual(hq_archive.parent.parts[-3:], ("MekHQ", "build", "distributions"))
        self.assertEqual(hq_archive.parent.parent.parent.parent.name, "mekhq")
        self.assertEqual((self.output / hq_archive.name).read_bytes(), b"fake archive")

    def test_dependency_resolution_failure_stops_pinned_build(self):
        directory = self.root / "megamek"
        directory.mkdir()
        (directory / "gradlew").touch()
        (directory / "gradlew.bat").touch()
        def unavailable(command, **kwargs):
            self.assertNotIn("--offline", command)
            raise subprocess.CalledProcessError(
                1, command, stderr="Could not resolve dependency: repository unavailable")
        with self.assertRaisesRegex(UnsafeInventory, "Could not resolve dependency"):
            pinned.gradle("MegaMek", pinned.TASKS["MegaMek"][0],
                          {"MegaMek": directory}, [], unavailable)

    def test_fail_closed_before_clone(self):
        for change in ({"previous": None}, {"commits": {"megamek": "main"}}):
            with self.subTest(change=change):
                self.calls.clear()
                inventory = dict(self.inventory, **change)
                with self.assertRaises(UnsafeInventory):
                    pinned.build(inventory, self.sources, {}, self.output, runner=self.fake)
                self.assertFalse(self.calls)

    def test_wrong_remote_and_dirty_stage_block_build(self):
        for fault in ("remote", "dirty"):
            self.calls.clear()
            def runner(command, **kwargs):
                response = self.fake(command, **kwargs)
                if fault == "remote" and "get-url" in command:
                    return subprocess.CompletedProcess(command, 0, stdout="https://evil.invalid/repo")
                if fault == "dirty" and "status" in command:
                    return subprocess.CompletedProcess(command, 0, stdout="?? unknown")
                return response
            with self.subTest(fault=fault), self.assertRaises(UnsafeInventory):
                pinned.build(self.inventory, self.sources, {}, self.output,
                             bootstrap=True, runner=runner)
            self.assertFalse(any(c[0].endswith(("gradlew", "gradlew.bat"))
                                 for c, _ in self.calls))

    def test_hq_only_reuses_verified_companions_and_own_versions(self):
        self.inventory["previous"] = self.record
        self.commits["mekhq"] = "f" * 40
        self.inventory["commits"] = self.commits
        prior = {}
        for product in pinned.REPOS:
            item = self.record["products"][product]
            path = self.root / item["asset"]["name"]
            path.write_bytes(b"prior")
            item["asset"]["size"] = 5
            item["asset"]["sha256"] = hashlib.sha256(b"prior").hexdigest()
            prior[product] = path
        self.inventory["floor"] = self.record["version"]
        seen = []
        def attest(record, paths, **kwargs):
            seen.append((record, dict(paths)))
            self.assertFalse(any(c[0].endswith(("gradlew", "gradlew.bat"))
                                 for c, _ in self.calls))
        def identity(path, product, version):
            meta, jars = self.identity(path, product, version)
            meta["megamekVersion"] = self.record["products"]["MegaMek"]["version"]
            meta["megameklabVersion"] = self.record["products"]["MegaMekLab"]["version"]
            return meta, jars
        with patch.object(pinned, "archive_identity", side_effect=identity):
            result = pinned.build(self.inventory, self.sources, prior, self.output,
                                  runner=self.fake, attest=attest)
        gradle = [c for c, _ in self.calls if c[0].endswith(("gradlew", "gradlew.bat"))]
        self.assertEqual([c[1] for c in gradle], list(pinned.TASKS["MekHQ"]))
        self.assertEqual(len(seen), 1)
        self.assertEqual(result["productVersions"]["MegaMek"],
                         self.record["products"]["MegaMek"]["version"])
        self.assertEqual(result["productVersions"]["MekHQ"], result["versionCandidate"])
        for command in gradle:
            self.assertIn(f"-PsuiteMegaMekArchiveFile={prior['MegaMek']}", command)
            self.assertIn(f"-PsuiteMegaMekLabArchiveFile={prior['MegaMekLab']}", command)
        self.assertEqual((self.output / prior["MegaMek"].name).read_bytes(), b"prior")

    def test_prior_digest_tamper_blocks_staging(self):
        self.inventory["previous"] = self.record
        self.commits["mekhq"] = "f" * 40
        self.inventory["commits"] = self.commits
        prior = {}
        for product, item in self.record["products"].items():
            path = self.root / item["asset"]["name"]
            path.write_bytes(b"wrong")
            prior[product] = path
        with self.assertRaisesRegex(UnsafeInventory, "prior archive"):
            pinned.build(self.inventory, self.sources, prior, self.output, runner=self.fake)
        self.assertFalse(self.calls)

    def test_record_only_reuses_all_verified_archives_without_gradle_build(self):
        self.inventory["previous"] = self.record
        self.inventory["membership"] = "development"
        prior = {}
        for product, item in self.record["products"].items():
            path = self.root / item["asset"]["name"]
            path.write_bytes(product.encode())
            item["asset"]["size"] = len(product)
            item["asset"]["sha256"] = hashlib.sha256(product.encode()).hexdigest()
            prior[product] = path
        seen = []
        result = pinned.build(self.inventory, self.sources, prior, self.output,
                              runner=self.fake,
                              attest=lambda r, p, **kw: seen.append(dict(p)))
        self.assertEqual(len(seen), 1)
        self.assertEqual(set(result["archives"]), set(pinned.REPOS))
        self.assertFalse(any(c[0].endswith(("gradlew", "gradlew.bat"))
                             for c, _ in self.calls))
