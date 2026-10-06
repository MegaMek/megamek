"""Shared synthetic regression suite for the three Gradle archive adapters.

Run only at the coordinator's validation checkpoint:
    python -m unittest discover -s gradle -p test_suite_archive_verifier.py
"""

import gzip
import io
import os
from pathlib import Path
import struct
import subprocess
import tarfile
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import suite_archive_verifier as verifier


PINS = {"megamekCommit": "a" * 40, "megameklabCommit": "b" * 40,
        "mekhqCommit": "c" * 40, "mmDataCommit": None}
VERSIONS = {"MegaMek": "0.51.01", "MegaMekLab": "0.51.02", "MekHQ": "0.51.03"}


def zipped(files):
    sink = io.BytesIO()
    with zipfile.ZipFile(sink, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for name, data in files.items():
            archive.writestr(name, data)
    return sink.getvalue()


def jar(product):
    if product == "MegaMek":
        content = b"major=0\nminor=51\npatch=01\nrevision=\n"
        return zipped({"Version.properties": content})
    return zipped({"META-INF/MANIFEST.MF": (
        "Manifest-Version: 1.0\r\nImplementation-Version: " +
        VERSIONS[product] + "\r\n\r\n").encode()})


class SuiteArchiveTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.data = self.base / "mm-data"
        self.data.mkdir()
        self.payload = {
            "data/fonts/normal.ttf": b"font",
            "data/names/first.txt": b"name",
            "data/images/fluff/banner.png": b"fluff",
            "data/images/misc/megameklab.ico": b"icon",
            "data/images/universe/factions/logo.png": b"faction",
            "data/boards/arena.board": b"board",
            "data/mekfiles/units.blk": b"unit",
            "data/mekfiles/meklist.txt": b"meklist",
            "data/rat/default.txt": b"rat",
            "data/universe/planetary_systems/canon_systems/sol.xml": b"sol",
            "data/universe/planetary_systems/connector_systems/jump.xml": b"jump",
        }
        for name, content in self.payload.items():
            target = self.data / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(content)
        subprocess.run(["git", "-C", str(self.data), "init", "-q"], check=True)
        subprocess.run(["git", "-C", str(self.data), "add", "data"], check=True)
        subprocess.run(["git", "-C", str(self.data), "-c", "user.name=Fixture",
                        "-c", "user.email=fixture@example.invalid",
                        "commit", "-qm", "pinned"], check=True)
        pin = subprocess.check_output(["git", "-C", str(self.data), "rev-parse", "HEAD"],
                                      text=True).strip()
        self.pins = dict(PINS, mmDataCommit=pin)
        self.jar = {product: jar(product) for product in verifier.PRODUCTS}
        self.archives = {}
        for product in verifier.PRODUCTS:
            self.write(product)

    def entries(self, product, *, older_successors=False):
        index = verifier.PRODUCTS.index(product)
        fields = dict(self.pins, schemaVersion="1", product=product,
                      version=VERSIONS[product], minimumJavaVersion="21")
        if index > 0:
            fields["megamekVersion"] = VERSIONS["MegaMek"]
        if index > 1:
            fields["megameklabVersion"] = VERSIONS["MegaMekLab"]
        if older_successors and product == "MegaMek":
            # Old archives may contain valid, unrelated older successor pins.
            fields["megameklabCommit"] = "d" * 40
            fields["mekhqCommit"] = "e" * 40
        if older_successors and product == "MegaMekLab":
            fields["mekhqCommit"] = "f" * 40
        files = {"suite-build.properties": "".join(
            f"{key}={value}\n" for key, value in fields.items()).encode()}
        for member in verifier.PRODUCTS[:index + 1]:
            files["lib/" + member + ".jar"] = self.jar[member]
            if member == product or product == "MekHQ":
                files[member + ".jar"] = self.jar[member]
        launchers = {
            "MegaMek": ("MegaMek.sh",),
            "MegaMekLab": ("MegaMekLab.sh", "bin/MegaMekLab", "bin/MegaMekLab.bat"),
            "MekHQ": ("MekHQ.sh", "MegaMek.sh", "MegaMekLab.sh", "bin/MekHQ"),
        }
        files.update({name: b"launcher" for name in launchers[product]})
        files.update({name: content for name, content in self.payload.items()
                      if verifier.selected(name, product)})
        if product == "MekHQ":
            files["data/images/force/Units/logo.png"] = self.payload[
                "data/images/universe/factions/logo.png"]
        if product != "MegaMekLab":
            files["data/images/imgFileAtlasMap.yml"] = b"---\nrecords:\n"
        generated = list(verifier.GENERATED)[:2 if index < 2 else 4]
        for name in generated:
            prefix = verifier.GENERATED[name]
            members = {path[len(prefix):]: body for path, body in self.payload.items()
                       if path.startswith(prefix) and
                       (prefix != "data/mekfiles/" or
                        not ("/" not in path[len(prefix):] and
                             path.endswith((".txt", ".xml", ".cache"))))}
            files[name] = zipped(members)
        return files

    def write(self, product, files=None, *, format=tarfile.PAX_FORMAT):
        path = self.base / f"{product}-{VERSIONS[product]}.tar.gz"
        with tarfile.open(path, "w:gz", format=format) as archive:
            for name, content in (files or self.entries(product)).items():
                entry = tarfile.TarInfo(f"{product}-{VERSIONS[product]}/{name}")
                entry.size = len(content)
                archive.addfile(entry, io.BytesIO(content))
        self.archives[product] = path
        return path

    def check(self, product, *, external=False, older_successors=False):
        index = verifier.PRODUCTS.index(product)
        pins = dict(self.pins)
        if older_successors and product == "MegaMek":
            pins.update(megameklabCommit="d" * 40, mekhqCommit="e" * 40)
        if older_successors and product == "MegaMekLab":
            pins["mekhqCommit"] = "f" * 40
        companions = ({member: self.archives[member] for member in
                       verifier.PRODUCTS[:index]} if external else {})
        sources = {}
        if not external:
            for member in verifier.PRODUCTS[:index + 1]:
                source = self.base / (member + ".jar")
                source.write_bytes(self.jar[member])
                sources[member] = source
        versions = {"megamekVersion": VERSIONS["MegaMek"],
                    "megameklabVersion": VERSIONS["MegaMekLab"]}
        return verifier.verify(self.archives[product], product, VERSIONS[product],
                               pins, versions, self.data, companions, sources)

    def test_all_three_built_and_external_adapters_and_older_successor_pins(self):
        for product in verifier.PRODUCTS:
            with self.subTest(product=product):
                self.check(product)
        for product in verifier.PRODUCTS[:2]:
            self.write(product, self.entries(product, older_successors=True))
        for product in verifier.PRODUCTS:
            with self.subTest(product=product):
                # In external mode there is no producer or built JAR to consult.
                with patch("suite_archive_verifier.subprocess.run",
                           wraps=subprocess.run) as runner:
                    self.check(product, external=True, older_successors=True)
                    self.assertTrue(all(call.args[0][0] == "git" for call in
                                        runner.call_args_list))

    def test_external_gradle_adapters_have_no_producer_tasks(self):
        worktrees = Path(__file__).resolve().parents[2]
        clean = all(not subprocess.check_output(
            ["git", "-C", str(worktrees / repo), "status", "--porcelain",
             "--untracked-files=no"]) for repo in
            ("megamek", "megameklab", "mekhq", "mm-data"))
        pins = {name: subprocess.check_output(
            ["git", "-C", str(worktrees / repo), "rev-parse", "HEAD"],
            text=True).strip() for name, repo in (
                ("suiteMegaMekCommit", "megamek"),
                ("suiteMegaMekLabCommit", "megameklab"),
                ("suiteMekHQCommit", "mekhq"),
                ("suiteMmDataCommit", "mm-data"))}
        flags = ["-PsuiteReleaseVersion=0.51.03",
                 "-PsuiteMegaMekVersion=0.51.01",
                 "-PsuiteMegaMekLabVersion=0.51.02",
                 "-PsuiteMekHQVersion=0.51.03"]
        flags += [f"-P{key}={value}" for key, value in pins.items()]
        for repo, task, product in (
            ("megamek", ":megamek:verifySuiteMegaMekArchive", "MegaMek"),
            ("megameklab", ":megameklab:verifySuiteArchive", "MegaMekLab"),
            ("mekhq", ":MekHQ:verifySuiteMekHQArchive", "MekHQ"),
        ):
            with self.subTest(product=product):
                folder = worktrees / repo
                command = [str(folder / ("gradlew.bat" if os.name == "nt" else "gradlew")),
                           task, "--dry-run", "--console=plain",
                           *flags, f"-PsuiteArchiveFile={self.archives[product]}"]
                if product != "MegaMek":
                    command.append(f"-PsuiteMegaMekArchiveFile={self.archives['MegaMek']}")
                if product == "MekHQ":
                    command.append(f"-PsuiteMegaMekLabArchiveFile={self.archives['MegaMekLab']}")
                result = subprocess.run(command, cwd=folder, text=True,
                                        capture_output=True, check=False)
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                self.assertNotIn(":distTar", result.stdout)
                self.assertNotIn(":jar", result.stdout)
                if clean:
                    # Built task graphs run source-pin preflight; on dirty dev
                    # worktrees only the read-only external graph is meaningful.
                    built = subprocess.run(
                        [str(folder / ("gradlew.bat" if os.name == "nt" else "gradlew")),
                         task, "--dry-run", "--console=plain", *flags],
                        cwd=folder, text=True, capture_output=True, check=False)
                    self.assertEqual(built.returncode, 0, built.stdout + built.stderr)
                    self.assertIn(":distTar", built.stdout)

    def test_missing_fonts_names_mismatch_and_generated_zip_contents(self):
        for product in verifier.PRODUCTS:
            for missing in ("data/fonts/normal.ttf", "data/names/first.txt"):
                with self.subTest(product=product, missing=missing):
                    files = self.entries(product)
                    del files[missing]
                    self.write(product, files)
                    with self.assertRaisesRegex(verifier.VerificationError, "missing/extra data"):
                        self.check(product)
            for corrupt in ("data/images/fluff/banner.png", "data/rat/rat_default.zip"):
                with self.subTest(product=product, corrupt=corrupt):
                    files = self.entries(product)
                    files[corrupt] = b"bad"
                    self.write(product, files)
                    with self.assertRaises((verifier.VerificationError, zipfile.BadZipFile)):
                        self.check(product)
            for members in ({}, {"default.txt": b"rat", "extra.txt": b"extra"}):
                with self.subTest(product=product, generated_members=members):
                    files = self.entries(product)
                    files["data/rat/rat_default.zip"] = zipped(members)
                    self.write(product, files)
                    with self.assertRaisesRegex(verifier.VerificationError,
                                                "generated ZIP differs"):
                        self.check(product)
            self.write(product)

    def test_companion_hash_and_version_pin(self):
        files = self.entries("MekHQ")
        files["lib/MegaMek.jar"] = zipped({"Version.properties":
                                            b"major=0\nminor=51\npatch=01\n"})
        files["MegaMek.jar"] = files["lib/MegaMek.jar"]
        self.write("MekHQ", files)
        with self.assertRaisesRegex(verifier.VerificationError, "companion JAR differs"):
            self.check("MekHQ", external=True)

    def test_packaged_runtime_and_built_source_jar(self):
        for product in verifier.PRODUCTS:
            with self.subTest(product=product):
                files = self.entries(product)
                wrong = (zipped({"Version.properties":
                                 b"major=0\nminor=51\npatch=99\n"})
                         if product == "MegaMek" else zipped({
                             "META-INF/MANIFEST.MF":
                                 b"Manifest-Version: 1.0\r\nImplementation-Version: 0.51.99\r\n\r\n"}))
                files[product + ".jar"] = files["lib/" + product + ".jar"] = wrong
                self.write(product, files)
                with self.assertRaisesRegex(verifier.VerificationError, "runtime version"):
                    self.check(product, external=True)
                self.write(product)
                source = self.base / (product + ".jar")
                source.write_bytes(b"wrong built jar")
                with self.assertRaisesRegex(verifier.VerificationError, "differs from source"):
                    verifier.verify(self.archives[product], product, VERSIONS[product],
                                    self.pins,
                                    {"megamekVersion": VERSIONS["MegaMek"],
                                     "megameklabVersion": VERSIONS["MegaMekLab"]},
                                    self.data, source_jars={product: source})

    def test_oversized_tar_extension_header_rejected_before_payload_read(self):
        path = self.archives["MegaMek"]
        def header(size, kind):
            block = bytearray(512)
            block[:4] = b"pax\0"
            block[124:136] = ("%011o\0" % size).encode()
            block[156] = kind
            block[257:263] = b"ustar\0"
            block[148:156] = b"        "
            block[148:156] = ("%06o\0 " % sum(block)).encode()
            return block
        for kind in ("x", "g", "L"):
            with self.subTest(extension=kind):
                with gzip.open(path, "wb") as output:
                    output.write(header(2 * 1024**3 + 1, ord(kind)))
                with self.assertRaisesRegex(verifier.VerificationError, "excessive"):
                    self.check("MegaMek")
        # Eight small PAX records may be legal; the ninth must fail before allocation.
        with gzip.open(path, "wb") as output:
            output.write((header(0, ord("x"))) * 9)
        with self.assertRaisesRegex(verifier.VerificationError, "excessive"):
            self.check("MegaMek")
        with gzip.open(path, "wb") as output:
            output.write(header(0, ord("S")))
        with self.assertRaisesRegex(verifier.VerificationError, "sparse"):
            self.check("MegaMek")

    def test_zip_directory_count_and_zip64_bound_before_zipfile(self):
        path = self.base / "malicious.jar"
        path.write_bytes(zipped({"META-INF/MANIFEST.MF": b"Manifest-Version: 1.0\r\n\r\n"}))
        payload = bytearray(path.read_bytes())
        payload[-10:-6] = struct.pack("<I", verifier.MAX_ZIP_DIRECTORY + 1)
        path.write_bytes(payload)
        with patch.object(zipfile, "ZipFile", side_effect=AssertionError("allocated")):
            with self.assertRaisesRegex(verifier.VerificationError, "excessive"):
                verifier.bounded_zip(path)
        payload[-10:-6] = b"\xff" * 4
        path.write_bytes(payload)
        with self.assertRaisesRegex(verifier.VerificationError, "Zip64"):
            verifier.bounded_zip(path)
        path.write_bytes(zipped({"Version.properties": b"major=0\n"})[:-4])
        with self.assertRaises(verifier.VerificationError):
            verifier.bounded_zip(path)

    def test_valid_zip64_directory_and_invalid_oversized_count(self):
        path = self.base / "zip64.jar"
        body = zipped({"META-INF/MANIFEST.MF": b"Manifest-Version: 1.0\r\n\r\n"})
        end = bytearray(body[-22:])
        _, _, _, on_disk, count, size, offset, _ = struct.unpack("<IHHHHIIH", end)
        self.assertEqual(on_disk, count)
        zip64 = struct.pack("<IQHHIIQQQQ", 0x06064b50, 44, 45, 45,
                            0, 0, count, count, size, offset)
        locator = struct.pack("<IIQI", 0x07064b50, 0, len(body) - 22, 1)
        end[8:12] = b"\xff" * 4
        end[12:20] = b"\xff" * 8
        path.write_bytes(body[:-22] + zip64 + locator + end)
        with verifier.zip_members(path) as archive:
            self.assertIn("META-INF/MANIFEST.MF", archive.namelist())
        invalid = bytearray(path.read_bytes())
        zip64_offset = len(body) - 22
        # entries-on-disk and total-entries are the two Q fields at +24/+32.
        struct.pack_into("<QQ", invalid, zip64_offset + 24, 100001, 100001)
        path.write_bytes(invalid)
        with self.assertRaisesRegex(verifier.VerificationError, "excessive"):
            verifier.bounded_zip(path)
        # ZipFile also honors a locator when legacy EOCD fields are *not*
        # sentinel values. Preflight must inspect it before construction.
        path.write_bytes(bytes(invalid[:-22]) + body[-22:])
        with patch.object(zipfile, "ZipFile", side_effect=AssertionError("allocated")):
            with self.assertRaisesRegex(verifier.VerificationError, "excessive"):
                verifier.zip_members(path)
