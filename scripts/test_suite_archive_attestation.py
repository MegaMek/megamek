"""Focused synthetic archive tests; no Gradle invocation or network access."""

import hashlib
import io
import json
import subprocess
import tarfile
import tempfile
import unittest
from pathlib import Path

import suite_archive_attestation as attestation
import suite_release_plan as release


class AttestationTests(unittest.TestCase):
    def setUp(self):
        fixture = Path(__file__).resolve().parents[1] / (
            "megamek/testresources/suite-records/complete.json")
        self.record = json.loads(fixture.read_text(encoding="utf-8"))
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.worktrees = {name: self.root / name for name in release.REPOS}
        for directory in self.worktrees.values():
            directory.mkdir()
            (directory / "gradlew.bat").touch()
            (directory / "gradlew").touch()
        self.props = {}
        self.paths = {}
        for product in release.REPOS:
            item = self.record["products"][product]
            self.paths[product] = self.root / item["asset"]["name"]
            self.props[product] = dict(
                schemaVersion="1", product=product, version=item["version"],
                megamekCommit=self.record["products"]["MegaMek"]["commit"],
                megameklabCommit=self.record["products"]["MegaMekLab"]["commit"],
                mekhqCommit=self.record["products"]["MekHQ"]["commit"],
                mmDataCommit=self.record["mmData"]["commit"], minimumJavaVersion="21")
        self.props["MegaMekLab"]["megamekVersion"] = self.props["MegaMek"]["version"]
        self.props["MekHQ"].update(
            megamekVersion=self.props["MegaMek"]["version"],
            megameklabVersion=self.props["MegaMekLab"]["version"])

    def write(self, product, entries=None, *, extra=()):
        path = self.paths[product]
        root = path.name.removesuffix(".tar.gz")
        payload = ("\n".join(f"{key}={value}" for key, value in
                             self.props[product].items()) + "\n").encode()
        if entries is None:
            entries = [(root + "/suite-build.properties", payload)]
            for jar in ("MegaMek.jar", "MegaMekLab.jar", "MekHQ.jar")[
                    :list(release.REPOS).index(product) + 1]:
                for place in (("lib/",) if product == "MegaMekLab" and
                              jar == "MegaMek.jar" else ("", "lib/")):
                    entries.append((root + "/" + place + jar, jar.encode()))
        entries = [*entries, *((root + "/" + name, data) for name, data in extra)]
        with tarfile.open(path, "w:gz") as archive:
            for name, data in entries:
                info = tarfile.TarInfo(name)
                info.size = len(data)
                archive.addfile(info, io.BytesIO(data))

    def write_all(self):
        for product in release.REPOS:
            self.write(product)

    def run_fake(self, *, portable=True):
        calls = []

        def fake(command, **kwargs):
            calls.append((command, kwargs))
            return subprocess.CompletedProcess(command, 0)

        attestation.attest_archives(
            self.record, self.paths, fake, self.worktrees, portable=portable)
        return calls

    def test_historical_read_only_contract_does_not_disable_identity_checks(self):
        self.write_all()
        aliases = (("bin/MegaMek", b"legacy"), ("bin/megamek", b"legacy"))
        self.write("MekHQ", extra=aliases)
        with self.assertRaisesRegex(release.UnsafeInventory, "case/normalization"):
            self.run_fake()
        self.assertEqual(len(self.run_fake(portable=False)), 3)
        self.props["MekHQ"]["mmDataCommit"] = "0" * 40
        self.write("MekHQ", extra=aliases)
        with self.assertRaisesRegex(release.UnsafeInventory, "identity differs"):
            self.run_fake(portable=False)

    def test_external_commands_use_downloaded_bytes_and_per_product_pins(self):
        # The old MegaMek archive does not claim the current Lab/HQ source commits.
        self.props["MegaMek"]["megameklabCommit"] = "d" * 40
        self.props["MegaMek"]["mekhqCommit"] = "e" * 40
        self.props["MegaMekLab"]["mekhqCommit"] = "f" * 40
        self.write_all()
        calls = self.run_fake()
        self.assertEqual(len(calls), 3)
        for (command, kwargs), product in zip(calls, release.REPOS):
            flags = dict(argument[2:].split("=", 1) for argument in command if argument.startswith("-P"))
            self.assertEqual(flags["suiteArchiveFile"], str(self.paths[product].resolve()))
            self.assertEqual(flags["suiteReleaseVersion"], self.record["version"])
            self.assertEqual(flags["suiteMegaMekVersion"], self.props["MegaMek"]["version"])
            self.assertEqual(flags["suiteMegaMekLabVersion"], self.props["MegaMekLab"]["version"])
            versions = [release.numbers(flags[k]) for k in (
                "suiteMegaMekVersion", "suiteMegaMekLabVersion",
                "suiteMekHQVersion", "suiteReleaseVersion")]
            self.assertEqual(versions, sorted(versions))
            self.assertEqual(flags["suiteMegaMekLabCommit"],
                             self.props[product]["megameklabCommit"])
            self.assertEqual(flags["suiteMekHQCommit"], self.props[product]["mekhqCommit"])
            self.assertEqual(flags["suiteMmDataCommit"], self.record["mmData"]["commit"])
            self.assertNotIn("--offline", command)
            self.assertIn("--no-daemon", command)
            self.assertTrue(kwargs["check"])
        self.assertNotIn("-PsuiteMegaMekArchiveFile", calls[0][0])
        self.assertIn(f"-PsuiteMegaMekArchiveFile={self.paths['MegaMek'].resolve()}", calls[1][0])
        self.assertIn(f"-PsuiteMegaMekLabArchiveFile={self.paths['MegaMekLab'].resolve()}",
                      calls[2][0])

    def test_identity_and_dependency_failure_precede_any_gradle_call(self):
        for product, key, value in (
                ("MegaMek", "version", "0.51.00"),
                ("MegaMekLab", "megameklabCommit", "a" * 40),
                ("MekHQ", "mmDataCommit", "b" * 40),
                ("MegaMekLab", "megamekCommit", "c" * 40),
                ("MekHQ", "megameklabVersion", "0.51.00")):
            with self.subTest(product=product, key=key):
                self.write_all()
                original = self.props[product][key]
                self.props[product][key] = value
                self.write(product)
                with self.assertRaises(release.UnsafeInventory):
                    attestation.attest_archives(
                        self.record, self.paths, lambda *a, **kw: self.fail("Gradle called"),
                        self.worktrees)
                self.props[product][key] = original

    def test_unsafe_and_duplicate_entries_and_verifier_failure(self):
        self.write_all()
        name = self.paths["MekHQ"].name.removesuffix(".tar.gz") + "/suite-build.properties"
        for entries in ([(name, b"schemaVersion=1\n")] * 2,
                        [("../escape", b"bad")],
                        [(name, b"schemaVersion=1\nschemaVersion=1\n")]):
            self.write("MekHQ", entries)
            with self.assertRaises(release.UnsafeInventory):
                self.run_fake()
        self.write("MekHQ")
        def fail(command, **kwargs):
            self.assertNotIn("--offline", command)
            raise subprocess.CalledProcessError(
                1, command, stderr="FAILURE: Build failed\nCould not resolve dependency\n"
                "Authorization: bearer github_pat_supersecret")
        with self.assertRaisesRegex(release.UnsafeInventory, "Could not resolve dependency") as caught:
            attestation.attest_archives(self.record, self.paths, fail, self.worktrees)
        self.assertNotIn("supersecret", str(caught.exception))

    def test_companion_bytes_and_links_fail_closed(self):
        self.write_all()
        root = self.paths["MekHQ"].name.removesuffix(".tar.gz")
        with tarfile.open(self.paths["MekHQ"], "w:gz") as archive:
            entry = tarfile.TarInfo(root + "/link")
            entry.type = tarfile.SYMTYPE
            entry.linkname = "../../escape"
            archive.addfile(entry)
        with self.assertRaisesRegex(release.UnsafeInventory, "unsafe"):
            self.run_fake()
        self.write_all()
        root = self.paths["MekHQ"].name.removesuffix(".tar.gz")
        props = self.props["MekHQ"]
        payload = ("\n".join(f"{key}={value}" for key, value in props.items()) + "\n").encode()
        entries = [(root + "/suite-build.properties", payload)]
        for jar in ("MegaMek.jar", "MegaMekLab.jar", "MekHQ.jar"):
            data = b"different" if jar == "MegaMekLab.jar" else jar.encode()
            entries.extend((root + "/" + prefix + jar, data) for prefix in ("", "lib/"))
        self.write("MekHQ", entries)
        with self.assertRaisesRegex(release.UnsafeInventory, "divergent companion jar"):
            self.run_fake()

    def test_jar_directory_and_lab_predecessor_mismatch(self):
        self.write_all()
        root = self.paths["MegaMekLab"].name.removesuffix(".tar.gz")
        payload = ("\n".join(f"{k}={v}" for k, v in self.props["MegaMekLab"].items())
                   + "\n").encode()
        with tarfile.open(self.paths["MegaMekLab"], "w:gz") as archive:
            for name, data in (
                    ("suite-build.properties", payload),
                    ("MegaMekLab.jar", b"MegaMekLab.jar"),
                    ("lib/MegaMekLab.jar", b"MegaMekLab.jar")):
                entry = tarfile.TarInfo(root + "/" + name)
                entry.size = len(data)
                archive.addfile(entry, io.BytesIO(data))
            entry = tarfile.TarInfo(root + "/lib/MegaMek.jar/")
            entry.type = tarfile.DIRTYPE
            archive.addfile(entry)
        with self.assertRaisesRegex(release.UnsafeInventory, "missing or divergent companion jar"):
            self.run_fake()
        self.write_all()
        self.write("MegaMekLab", [
            (root + "/suite-build.properties", payload),
            (root + "/MegaMekLab.jar", b"MegaMekLab.jar"),
            (root + "/lib/MegaMekLab.jar", b"MegaMekLab.jar"),
            (root + "/lib/MegaMek.jar", b"different")])
        with self.assertRaisesRegex(release.UnsafeInventory, "companion jar"):
            self.run_fake()

    def test_discovery_rejects_attestation_failure(self):
        self.write_all()
        details = {}
        for product, repo in release.REPOS.items():
            item = self.record["products"][product]
            blob = self.paths[product].read_bytes()
            item["asset"]["size"] = len(blob)
            item["asset"]["sha256"] = hashlib.sha256(blob).hexdigest()
            details[repo] = [{"id": item["releaseId"], "tag_name": item["tag"],
                              "draft": False, "assets": [{
                                  "id": item["asset"]["assetId"], "name": item["asset"]["name"],
                                  "size": len(blob), "state": "uploaded"}]}]
        raw = json.dumps(self.record).encode()
        details["megamek"].append({
            "id": 999, "tag_name": self.record["tag"], "draft": False,
            "assets": [{"id": 998, "name": f"suite-record-{self.record['version']}.json",
                        "size": len(raw), "state": "uploaded"}]})

        def getter(endpoint):
            if "/releases?" in endpoint:
                return details[endpoint.split("/")[2]]
            self.fail(f"discovery advanced after attestation: {endpoint}")

        def fetch(repo, asset_id, destination):
            if asset_id == 998:
                destination.write_bytes(raw)
            else:
                product = next(n for n, r in release.REPOS.items() if r == repo)
                destination.write_bytes(self.paths[product].read_bytes())

        self.props["MegaMekLab"]["megamekVersion"] = "0.51.00"
        self.write("MegaMekLab")
        # Preserve the declared byte digest so this tests archive attestation,
        # not the earlier asset-integrity gate.
        blob = self.paths["MegaMekLab"].read_bytes()
        self.record["products"]["MegaMekLab"]["asset"].update(
            size=len(blob), sha256=hashlib.sha256(blob).hexdigest())
        details["megameklab"][0]["assets"][0]["size"] = len(blob)
        raw = json.dumps(self.record).encode()
        details["megamek"][-1]["assets"][0]["size"] = len(raw)
        with self.assertRaisesRegex(release.UnsafeInventory, "dependency closure"):
            release.discovered_inventory(
                "weekly", getter, fetch, attest=lambda record, paths:
                attestation.attest_archives(record, paths,
                                             lambda *args, **kwargs: self.fail("Gradle called"),
                                             self.worktrees))

    def test_downloaded_bytes_attested_before_reuse(self):
        self.write_all()
        details = {}
        for product, repo in release.REPOS.items():
            item = self.record["products"][product]
            blob = self.paths[product].read_bytes()
            item["asset"]["size"] = len(blob)
            item["asset"]["sha256"] = hashlib.sha256(blob).hexdigest()
            details[repo] = [{"id": item["releaseId"], "tag_name": item["tag"],
                              "draft": False, "assets": [{
                                  "id": item["asset"]["assetId"], "name": item["asset"]["name"],
                                  "size": len(blob), "state": "uploaded"}]}]

        def fetch(repo, asset_id, destination):
            product = next(name for name, key in release.REPOS.items() if key == repo)
            destination.write_bytes(self.paths[product].read_bytes())

        calls = []
        def attest(record, paths):
            calls.extend(attestation.attest_archives(
                record, paths, lambda command, **kwargs: None, self.worktrees) or
                         [paths.copy()])
        release.check_product_assets(self.record, details, fetch, attest)
        self.assertEqual(set(calls[0]), set(release.REPOS))
        self.record["products"]["MegaMek"]["asset"]["sha256"] = "0" * 64
        with self.assertRaisesRegex(release.UnsafeInventory, "asset bytes differ"):
            release.check_product_assets(self.record, details, fetch, attest)
        self.assertEqual(len(calls), 1)


if __name__ == "__main__":
    unittest.main()
