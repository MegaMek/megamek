"""Shared, read-only suite archive verifier. Used by all three composite builds.

No tar extraction and no unbounded tar/ZIP metadata parsing: extension headers
are checked before decoding, and ZIP directories before ZipFile is constructed.
"""

import argparse
import gzip
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import unicodedata
import zipfile

PRODUCTS = ("MegaMek", "MegaMekLab", "MekHQ")
FIELDS = {
    "MegaMek": {"schemaVersion", "product", "version", "megamekCommit",
                "megameklabCommit", "mekhqCommit", "mmDataCommit", "minimumJavaVersion"},
    "MegaMekLab": {"schemaVersion", "product", "version", "megamekVersion",
                   "megamekCommit", "megameklabCommit", "mekhqCommit",
                   "mmDataCommit", "minimumJavaVersion"},
    "MekHQ": {"schemaVersion", "product", "version", "megamekVersion",
              "megameklabVersion", "megamekCommit", "megameklabCommit",
              "mekhqCommit", "mmDataCommit", "minimumJavaVersion"},
}
FORBIDDEN = {"clientsettings.xml", "gameoptions.xml", "mm.preferences",
             "mml.preferences", "mhq.preferences", "megameklab.properties",
             "megameklab.properties.bak", "recent-advanced-searches.json",
             "recent_boards.yml"}
GENERATED = {
    "data/mekfiles/unit_files.zip": "data/mekfiles/",
    "data/rat/rat_default.zip": "data/rat/",
    "data/universe/planetary_systems/canon_systems.zip":
        "data/universe/planetary_systems/canon_systems/",
    "data/universe/planetary_systems/connector_systems.zip":
        "data/universe/planetary_systems/connector_systems/",
}
MAX_ENTRIES = 100000
MAX_ENTRY = 2 * 1024**3
MAX_TOTAL = 8 * 1024**3
MAX_ZIP_DIRECTORY = 32 * 1024**2
DATA_RULES = json.loads(Path(__file__).with_name("suite_data_rules.json").read_text(
    encoding="utf-8"))


class VerificationError(ValueError):
    pass


def require(condition, message):
    if not condition:
        raise VerificationError(message)


def exact_properties(payload, keys):
    require(len(payload) <= 65536, "oversized properties")
    props = {}
    for line in payload.decode("ascii").splitlines():
        require(re.fullmatch(r"[A-Za-z][A-Za-z0-9]*=[A-Za-z0-9.]+", line),
                "malformed properties")
        key, value = line.split("=", 1)
        require(key not in props, "duplicate property")
        props[key] = value
    require(set(props) == keys, "missing or unexpected properties")
    return props


def _number(raw):
    # Reject base-256 and negative sizes: never pass them to a tar library.
    require(raw and raw[0] < 128, "invalid tar number")
    value = raw.strip(b"\0 ")
    require(not value or re.fullmatch(rb"[0-7]+", value), "invalid tar number")
    return int(value or b"0", 8)


def _read(stream, count):
    value = stream.read(count)
    require(len(value) == count, "truncated archive")
    return value


def _path(name, root, directory):
    require(len(name) <= 4096 and "\x00" not in name and "\\" not in name,
            "unsafe archive path")
    value = name.rstrip("/") if directory else name
    require(value == root or value.startswith(root + "/"), "wrong archive root")
    require(all(part not in ("", ".", "..") for part in value.split("/")),
            "unsafe archive path")
    return value


def _pax(payload):
    result = {}
    while payload:
        match = re.match(rb"([0-9]+) ", payload)
        require(match is not None, "invalid PAX record")
        length = int(match.group(1))
        require(0 < length <= len(payload), "invalid PAX length")
        record = payload[len(match.group(0)):length]
        require(record.endswith(b"\n") and b"=" in record, "invalid PAX field")
        key, value = record[:-1].split(b"=", 1)
        require(key in (b"path", b"mtime", b"atime", b"ctime", b"uid", b"gid",
                        b"uname", b"gname", b"size"), "unsupported PAX field")
        require(not key.startswith(b"GNU.sparse") and key not in result,
                "sparse or duplicate PAX field")
        result[key] = value
        payload = payload[length:]
    return result


def _portable_path(path, directory, paths):
    require(len(path.encode("utf-16-le")) // 2 <= 512, "non-portable archive path length")
    parts = path.split("/")
    for part in parts:
        require(":" not in part and not part.endswith((".", " "))
                and not re.fullmatch(r"(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\..*)?",
                                     part, flags=re.ASCII),
                f"non-portable archive path: {path}")
        require(part.lower() != ".mm-launcher", f"reserved archive path: {path}")
    key = unicodedata.normalize("NFC", path).lower()
    prior = paths.get(key)
    require(prior is None or (prior[0] == path and prior[1] and directory and not prior[2]),
            f"duplicate/case/normalization archive path: {prior[0] if prior else path} and {path}")
    for depth in range(1, len(parts)):
        parent = "/".join(parts[:depth])
        parent_key = unicodedata.normalize("NFC", parent).lower()
        parent_prior = paths.get(parent_key)
        require(parent_prior is None or (parent_prior[0] == parent and parent_prior[1]),
                f"archive parent collision: {parent_prior[0] if parent_prior else parent} and {path}")
        if parent_prior is None:
            require(len(paths) < MAX_ENTRIES, "excessive archive namespace")
            paths[parent_key] = (parent, True, False)
    require(key in paths or len(paths) < MAX_ENTRIES, "excessive archive namespace")
    paths[key] = (path, directory, True)


def scan(archive, product, version, *, keep_jars=False, capture=(), portable=True):
    """Stream a bounded tar. Return identity, file digests, and optional temporary jars.

    The caller owns the returned TemporaryDirectory and must close it.
    """
    archive = Path(archive)
    root = f"{product}-{version}"
    require(archive.is_file() and archive.name == root + ".tar.gz",
            f"missing or incorrectly named archive: {archive}")
    tmp = tempfile.TemporaryDirectory(prefix="suite-verifier-")
    files, jars, seen = {}, {}, set()
    paths = {}
    properties = None
    total = extensions = extension_bytes = metadata_total = 0
    pending = {}
    try:
        with archive.open("rb") as raw, gzip.GzipFile(fileobj=raw) as stream:
            while True:
                header = stream.read(512)
                require(len(header) == 512, "truncated tar header")
                if header == bytes(512):
                    require(_read(stream, 512) == bytes(512),
                            "truncated tar end marker")
                    require(not pending, "dangling tar extension")
                    # GNU tar pads to a 10240-byte record. Still consume to EOF
                    # so gzip's CRC and length trailer are checked.
                    padding = stream.read(10241)
                    require(len(padding) <= 10240 and not any(padding),
                            "trailing tar payload")
                    break
                require(header[257:263] in (b"ustar\0", b"ustar "),
                        "unsupported tar header")
                check = _number(header[148:156])
                require(check == sum(header[:148]) + 8 * 32 + sum(header[156:]),
                        "invalid tar checksum")
                size = _number(header[124:136])
                require(size <= MAX_ENTRY and total + size <= MAX_TOTAL,
                        "excessive tar payload")
                kind = header[156:157]
                require(kind in (b"0", b"\0", b"5", b"L", b"K", b"x", b"g"),
                        "unsafe tar type (link or sparse)")
                require(not header[157:257].strip(b"\0"),
                        "tar links are not allowed")
                if kind in (b"L", b"K", b"x", b"g"):
                    extensions += 1
                    extension_bytes += size
                    metadata_total += size
                    require(extensions <= 8 and extension_bytes <= 8192 and size <= 4096,
                            "excessive tar extension metadata")
                    require(metadata_total <= MAX_ZIP_DIRECTORY,
                            "excessive aggregate tar metadata")
                    require(kind != b"K", "tar links are not allowed")
                    payload = _read(stream, size)
                    _read(stream, (-size) % 512)
                    if kind == b"L":
                        require(payload.endswith(b"\0"), "invalid GNU long name")
                        pending[b"path"] = payload[:-1]
                    else:
                        fields = _pax(payload)
                        if kind == b"g":
                            require(b"path" not in fields and b"size" not in fields,
                                    "unsafe global PAX fields")
                        else:
                            pending.update(fields)
                    continue
                require(b"size" not in pending or int(pending[b"size"]) == size,
                        "conflicting PAX size")
                name = pending.get(b"path")
                if name is None:
                    prefix = header[345:500].split(b"\0", 1)[0]
                    name = header[:100].split(b"\0", 1)[0]
                    if prefix:
                        name = prefix + b"/" + name
                path = _path(name.decode("utf-8"), root, kind == b"5")
                require(path not in seen and len(seen) < MAX_ENTRIES,
                        "duplicate or excessive tar entries")
                seen.add(path)
                if portable:
                    _portable_path(path, kind == b"5", paths)
                pending = {}
                extensions = extension_bytes = 0
                if kind == b"5":
                    require(size == 0, "directory with payload")
                    continue
                require(path != root, "root must be a directory")
                relative = path[len(root) + 1:]
                require(relative.rsplit("/", 1)[-1] not in FORBIDDEN
                        and not relative.startswith(("userdata/", "mmconf/searches/"))
                        and not (relative.startswith("data/") and relative.endswith(".cache")),
                        f"private or cache file: {relative}")
                digest = hashlib.sha256()
                blob = hashlib.sha1()
                blob.update(f"blob {size}\0".encode("ascii"))
                keep = relative in capture or (keep_jars and relative in (
                    {p + ".jar" for p in PRODUCTS} |
                    {"lib/" + p + ".jar" for p in PRODUCTS}))
                require(relative != "data/images/imgFileAtlasMap.yml" or size <= 8 * 1024**2,
                        "oversized image atlas map")
                saved = Path(tmp.name) / str(len(jars)) if keep else None
                identity_entry = relative == "suite-build.properties"
                require(not identity_entry or size <= 65536, "oversized suite identity")
                data = io.BytesIO() if identity_entry else None
                with (saved.open("wb") if saved else io.BytesIO()) as output:
                    remaining = size
                    while remaining:
                        chunk = _read(stream, min(65536, remaining))
                        remaining -= len(chunk)
                        digest.update(chunk)
                        blob.update(chunk)
                        if saved:
                            output.write(chunk)
                        if data is not None:
                            data.write(chunk)
                _read(stream, (-size) % 512)
                total += size
                files[relative] = (digest.hexdigest(), blob.hexdigest())
                if saved:
                    jars[relative] = saved
                if identity_entry:
                    properties = exact_properties(data.getvalue(), FIELDS[product])
        require(properties is not None, "missing suite identity")
        return properties, files, jars, tmp
    except BaseException:
        tmp.cleanup()
        raise


def bounded_zip(path):
    """Check EOCD, Zip64, central record lengths and offsets before ZipFile allocates."""
    length = Path(path).stat().st_size
    with open(path, "rb") as stream:
        stream.seek(max(0, length - 65557))
        tail = stream.read()
        at = tail.rfind(b"PK\x05\x06")
        require(at >= 0 and at + 22 <= len(tail), "missing ZIP end record")
        end = tail[at:]
        disk, cd_disk, disk_count, count, size, offset, comment = struct.unpack_from(
            "<HHHHIIH", end, 4)
        require(at + 22 + comment == len(tail) and disk == cd_disk == 0
                and disk_count == count, "multi-disk or trailing ZIP bytes")
        end_offset = length - len(tail) + at
        sentinel = count == 65535 or size == 0xffffffff or offset == 0xffffffff
        locator = None
        if end_offset >= 20:
            stream.seek(end_offset - 20)
            candidate = _read(stream, 20)
            if candidate[:4] == b"PK\x06\x07":
                locator = candidate
        require(not sentinel or locator is not None, "missing Zip64 locator")
        if locator is not None:
            loc = locator
            signature, disk64, where, disks = struct.unpack("<IIQI", loc)
            require(signature == 0x07064b50 and disk64 == 0 and disks == 1
                    and where < end_offset - 20, "invalid Zip64 locator")
            stream.seek(where)
            zip64 = _read(stream, 56)
            require(zip64[:4] == b"PK\x06\x06" and
                    struct.unpack_from("<Q", zip64, 4)[0] == 44,
                    "invalid Zip64 end record")
            _, _, _, disk, cd_disk, disk_count, count, size, offset = struct.unpack(
                "<QHHIIQQQQ", zip64[4:])
            require(disk == cd_disk == 0 and disk_count == count,
                    "invalid Zip64 directory")
            directory_end = where
        else:
            directory_end = end_offset
        require(count <= MAX_ENTRIES and size <= MAX_ZIP_DIRECTORY
                and offset + size <= length, "excessive ZIP directory")
        require(offset + size <= directory_end, "invalid ZIP directory offset")
        stream.seek(offset)
        consumed = 0
        for _ in range(count):
            fixed = _read(stream, 46)
            require(fixed[:4] == b"PK\x01\x02", "invalid ZIP central entry")
            name_len, extra_len, comment_len = struct.unpack_from("<HHH", fixed, 28)
            consumed += 46 + name_len + extra_len + comment_len
            require(consumed <= size and name_len <= 4096,
                    "excessive ZIP central entry")
            stream.seek(name_len + extra_len + comment_len, os.SEEK_CUR)
        require(consumed == size, "ZIP central count or size mismatch")


def zip_members(path):
    bounded_zip(path)
    return zipfile.ZipFile(path)


def safe_zip_name(name):
    require(len(name) <= 4096 and not name.startswith("/") and
            "\\" not in name and "\0" not in name and
            all(part not in ("", ".", "..") for part in name.rstrip("/").split("/")),
            f"unsafe ZIP entry: {name}")


def safe_zip_type(entry):
    require((entry.external_attr >> 16) & 0o170000 != 0o120000,
            f"ZIP symlink is not allowed: {entry.filename}")


def jar_version(path, product):
    with zip_members(path) as jar:
        seen = set()
        for item in jar.infolist():
            safe_zip_name(item.filename)
            safe_zip_type(item)
            require(item.filename not in seen and len(seen) < MAX_ENTRIES,
                    "duplicate or excessive JAR entries")
            seen.add(item.filename)
        metadata = "Version.properties" if product == "MegaMek" else "META-INF/MANIFEST.MF"
        require(metadata in seen, f"missing {product} JAR metadata")
        item = jar.getinfo(metadata)
        require(0 <= item.file_size <= 65536, "oversized JAR metadata")
        with jar.open(item) as source:
            content = source.read(65537)
        require(len(content) <= 65536, "oversized JAR metadata")
        if product == "MegaMek":
            props = {}
            for line in content.decode("utf-8").splitlines():
                if not line.strip() or line.startswith(("#", "!")):
                    continue
                match = re.fullmatch(r"(major|minor|patch|revision)=([A-Za-z0-9.]*)", line)
                require(match is not None and match.group(1) not in props,
                        "ambiguous Version.properties")
                props[match.group(1)] = match.group(2)
            require(not props.get("revision", "").strip(), "point-release JAR revision")
            return ".".join(props.get(key, "") for key in ("major", "minor", "patch"))
        # Manifest lines may fold; only Implementation-Version matters.
        lines = content.decode("utf-8").replace("\r\n ", "").splitlines()
        versions = [line.split(": ", 1)[1] for line in lines
                    if line.startswith("Implementation-Version: ")]
        require(len(versions) == 1, "missing or duplicate JAR version")
        return versions[0]


def pinned_tree(repo, pin):
    require(re.fullmatch("[0-9a-f]{40}", pin), "invalid mm-data pin")
    result = subprocess.run(["git", "-C", str(repo), "ls-tree", "-rz", pin, "--", "data"],
                            capture_output=True, check=True).stdout
    blobs = {}
    for entry in result.split(b"\0"):
        if entry:
            match = re.fullmatch(rb"100(?:644|755) blob ([0-9a-f]{40})\t(data/.+)", entry)
            require(match is not None, "invalid pinned data tree")
            blobs[match.group(2).decode("utf-8")] = match.group(1).decode("ascii")
    require(blobs, "empty pinned mm-data tree")
    return blobs


def selected(path, product):
    """Mirror the products' split Sync include rules and common staging exclusions."""
    if (any(p in path.split("/") for p in (".DS_Store", "Thumbs.db", ".gitignore"))
            or path.endswith(".cache") or (product != "MekHQ" and path.endswith(".psd"))):
        return False
    if path in GENERATED:
        return False
    if path.startswith(("data/fonts/", "data/boards/")):
        return product != "MegaMekLab" or path.startswith("data/fonts/")
    if path.startswith("data/images/"):
        category = path.split("/")[2]
        if product == "MekHQ":
            return True
        return category in DATA_RULES[product]["images"] and "." in path.rsplit("/", 1)[-1]
    if path.startswith("data/mekfiles/"):
        return "/" not in path[len("data/mekfiles/"):] and path.endswith((".txt", ".xml"))
    if path.startswith("data/rat/"):
        return False
    if product == "MekHQ":
        return (not path.startswith(("data/universe/planetary_systems/canon_systems/",
                                     "data/universe/planetary_systems/connector_systems/"))
                and "." in path.rsplit("/", 1)[-1])
    rel = path[len("data/"):]
    for pattern in DATA_RULES[product]["loose"]:
        if pattern.endswith("/**/*.*"):
            if rel.startswith(pattern[:-6]) and "." in rel.rsplit("/", 1)[-1]:
                return True
        elif pattern.endswith("/*.*"):
            prefix = pattern[:-3]
            if rel.startswith(prefix) and "/" not in rel[len(prefix):] and "." in rel[len(prefix):]:
                return True
        elif rel == pattern:
            return True
    return False


def verify_zip_data(jar_path, prefix, blobs):
    expected = {name[len(prefix):]: blob for name, blob in blobs.items()
                if name.startswith(prefix)}
    if prefix == "data/mekfiles/":
        expected = {name: blob for name, blob in expected.items()
                    if not ("/" not in name and name.endswith((".txt", ".xml", ".cache")))}
    found = {}
    seen = set()
    expanded = 0
    with zip_members(jar_path) as jar:
        for item in jar.infolist():
            safe_zip_name(item.filename)
            safe_zip_type(item)
            require(item.filename not in seen and len(seen) < MAX_ENTRIES,
                    "duplicate or excessive generated ZIP entry")
            seen.add(item.filename)
            if item.is_dir():
                continue
            expanded += item.file_size
            require(item.filename not in found and 0 <= item.file_size <= MAX_ENTRY
                    and expanded <= MAX_TOTAL,
                    "duplicate or oversized generated ZIP entry")
            sha = hashlib.sha1()
            sha.update(f"blob {item.file_size}\0".encode())
            with jar.open(item) as source:
                remaining = item.file_size
                while remaining:
                    chunk = source.read(min(65536, remaining))
                    require(chunk, "truncated generated ZIP entry")
                    remaining -= len(chunk)
                    sha.update(chunk)
                require(not source.read(1), "oversized generated ZIP entry")
            found[item.filename] = sha.hexdigest()
    require(found == expected, f"generated ZIP differs from pinned {prefix} tree")


def atlas_replacements(path, available):
    """Only atlas references produced by CreateImageAtlases may replace loose images."""
    if path is None:
        return set(), set()
    lines = Path(path).read_text(encoding="utf-8").splitlines()
    unfolded = []
    for line in lines:
        if unfolded and unfolded[-1].endswith("\\") and line.startswith("    \\"):
            unfolded[-1] = unfolded[-1][:-1] + line[5:]
        else:
            unfolded.append(line)
    lines = unfolded
    require(lines[:2] == ["---", "records:"] and len(lines) <= MAX_ENTRIES,
            "invalid atlas map")
    replaced, atlases = set(), set()
    require(len(lines[2:]) % 2 == 0, "incomplete atlas mapping")
    for original, location in zip(lines[2::2], lines[3::2]):
        match = re.fullmatch(r'- originalFilePath: "(data/images/(?:units|hexes)/[^"]+)"',
                             original)
        target = re.fullmatch(r'  atlasFilePath: "(data/images/(?:units|hexes)/[^"]+_atlas\.png)'
                              r'\([0-9]+,[0-9]+-[0-9]+,[0-9]+\)"', location)
        require(match and target, "invalid atlas reference")
        old, new = match.group(1), target.group(1)
        folder, name = old.rsplit("/", 1)
        require("largeTextures" not in folder and
                name.lower().endswith((".png", ".gif", ".jpg", ".jpeg")) and
                new == f"{folder}/{folder.rsplit('/', 1)[-1]}_atlas.png" and
                new in available and old not in replaced, "unexpected atlas substitution")
        replaced.add(old)
        atlases.add(new)
    return replaced, atlases


def verify(archive, product, version, pins, versions, data_repo, companions=None,
           source_jars=None, copy_jar=None):
    companions = companions or {}
    source_jars = source_jars or {}
    own = {"MegaMek": "megamekCommit", "MegaMekLab": "megameklabCommit",
           "MekHQ": "mekhqCommit"}[product]
    generated = list(GENERATED)[:2] if product != "MekHQ" else list(GENERATED)
    captures = generated + (["data/images/imgFileAtlasMap.yml"]
                            if product in ("MegaMek", "MekHQ") else [])
    props, files, jars, temp = scan(archive, product, version, keep_jars=True,
                                    capture=captures)
    try:
        require(props["schemaVersion"] == "1" and props["product"] == product
                and props["version"] == version and props["minimumJavaVersion"] == "21",
                "wrong archive identity")
        for key in ("megamekCommit", "megameklabCommit", "mekhqCommit", "mmDataCommit"):
            require(re.fullmatch("[0-9a-f]{40}", props[key]), f"invalid {key}")
            if key in pins:
                require(props[key] == pins[key], f"{key} differs from pin")
        for key in ("megamekVersion", "megameklabVersion"):
            if key in props and key in versions:
                require(props[key] == versions[key], f"{key} differs from pin")
        require(own in pins and props[own] == pins[own], "product source pin differs")
        required = {"suite-build.properties", product + ".jar", "lib/" + product + ".jar"}
        launchers = {"MegaMek": {"MegaMek.sh"},
                     "MegaMekLab": {"MegaMekLab.sh", "bin/MegaMekLab",
                                    "bin/MegaMekLab.bat"},
                     "MekHQ": {"MekHQ.sh", "MegaMek.sh", "MegaMekLab.sh",
                               "bin/MekHQ"}}[product]
        required |= launchers
        included = PRODUCTS[:PRODUCTS.index(product) + 1]
        for dependency in included[:-1]:
            required.add("lib/" + dependency + ".jar")
            if product == "MekHQ":
                required.add(dependency + ".jar")
        require(required <= files.keys(), f"missing archive entries: {required - files.keys()}")
        for dependency in included:
            name = dependency + ".jar"
            if name in files:
                require(files[name][0] == files["lib/" + name][0],
                        f"divergent root/lib JAR: {name}")
            expected_version = (version if dependency == product else
                                versions.get({"MegaMek": "megamekVersion",
                                              "MegaMekLab": "megameklabVersion"}[dependency]))
            require(expected_version and jar_version(jars["lib/" + name], dependency) ==
                    expected_version, f"incorrect {dependency} runtime version")
            if name in jars:
                require(jar_version(jars[name], dependency) == expected_version,
                        f"incorrect {dependency} launcher version")
        for dependency, source in source_jars.items():
            digest = hashlib.sha256()
            with open(source, "rb") as input_file:
                for chunk in iter(lambda: input_file.read(65536), b""):
                    digest.update(chunk)
            require(digest.hexdigest() == files["lib/" + dependency + ".jar"][0],
                    f"packaged {dependency} JAR differs from source")
        blobs = pinned_tree(data_repo, props["mmDataCommit"])
        expected = {path: blob for path, blob in blobs.items() if selected(path, product)}
        if product == "MekHQ":
            for path, blob in blobs.items():
                prefix = "data/images/universe/factions/"
                if path.startswith(prefix):
                    expected["data/images/force/Units/" + path[len(prefix):]] = blob
        expected.update({path: None for path in generated})
        # The producer replaces correctly-sized unit/hex images with atlases;
        # its manifest is mandatory and cannot excuse missing fonts/names/etc.
        if product in ("MegaMek", "MekHQ"):
            require("data/images/imgFileAtlasMap.yml" in files,
                    "missing image atlas map")
            replaced, atlases = atlas_replacements(
                jars["data/images/imgFileAtlasMap.yml"], files)
            require(replaced <= expected.keys(), "atlas map references unpinned data")
            expected = {path: blob for path, blob in expected.items()
                        if path not in replaced or path in files}
            for path in files:
                if path == "data/images/imgFileAtlasMap.yml" or path in atlases:
                    expected[path] = None
        actual = {path: digest for path, digest in files.items() if path.startswith("data/")}
        require(actual.keys() == expected.keys(),
                f"missing/extra data: missing={list(expected.keys() - actual.keys())[:8]}, "
                f"extra={list(actual.keys() - expected.keys())[:8]}")
        for path, blob in expected.items():
            if blob is not None:
                require(files[path][1] == blob, f"data differs from pinned tree: {path}")
        for path in generated:
            # Generated ZIP content is checked against the pinned *tree*, not a
            # potentially stale build/staging ZIP and not a new producer invocation.
            # Only bounded members, never the entire tar, are spooled.
            verify_zip_data(jars[path], GENERATED[path], blobs)
        companion_files = {}
        for dependency, companion in companions.items():
            dep_version = versions[{"MegaMek": "megamekVersion",
                                    "MegaMekLab": "megameklabVersion"}[dependency]]
            meta, contents, _, lease = scan(companion, dependency, dep_version)
            try:
                require(meta["schemaVersion"] == "1" and meta["product"] == dependency
                        and meta["version"] == dep_version
                        and meta["minimumJavaVersion"] == "21",
                        f"{dependency} companion identity differs")
                for key in ("megamekCommit", "megameklabCommit",
                            "mekhqCommit", "mmDataCommit"):
                    require(re.fullmatch("[0-9a-f]{40}", meta[key]),
                            f"invalid {dependency} companion pin")
                for key in ("megamekCommit", "mmDataCommit"):
                    require(meta[key] == props[key], f"{dependency} companion {key} differs")
                if dependency == "MegaMekLab":
                    require(meta["megameklabCommit"] == props["megameklabCommit"]
                            and meta["megamekVersion"] == props["megamekVersion"],
                            "Lab companion closure differs")
                require(contents[dependency + ".jar"][0] ==
                        contents["lib/" + dependency + ".jar"][0] ==
                        files["lib/" + dependency + ".jar"][0],
                        f"{dependency} companion JAR differs")
                companion_files[dependency] = contents
            finally:
                lease.cleanup()
        if "MegaMek" in companion_files and "MegaMekLab" in companion_files:
            require(companion_files["MegaMekLab"]["lib/MegaMek.jar"][0] ==
                    companion_files["MegaMek"]["MegaMek.jar"][0],
                    "Lab and MegaMek companion JARs differ")
        if copy_jar:
            target = Path(copy_jar)
            target.parent.mkdir(parents=True, exist_ok=True)
            temporary = target.with_name(target.name + ".verified.tmp")
            try:
                shutil.copyfile(jars[product + ".jar"], temporary)
                os.replace(temporary, target)
            finally:
                temporary.unlink(missing_ok=True)
        return props, files
    finally:
        temp.cleanup()


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--product", required=True, choices=PRODUCTS)
    parser.add_argument("--archive", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--data-repo", required=True)
    parser.add_argument("--pin", action="append", default=[])
    parser.add_argument("--dependency-version", action="append", default=[])
    parser.add_argument("--companion", action="append", default=[])
    parser.add_argument("--source-jar", action="append", default=[])
    parser.add_argument("--copy-jar")
    args = parser.parse_args(argv)
    def pairs(values):
        return dict(value.split("=", 1) for value in values)
    verify(args.archive, args.product, args.version, pairs(args.pin),
           pairs(args.dependency_version), args.data_repo,
           pairs(args.companion), pairs(args.source_jar), args.copy_jar)


if __name__ == "__main__":
    try:
        main()
    except (VerificationError, OSError, ValueError, KeyError, zipfile.BadZipFile,
            subprocess.CalledProcessError) as exc:
        print(f"suite archive verification failed: {exc}", file=sys.stderr)
        sys.exit(1)
