#!/usr/bin/env python3
"""Stage exact pinned mm-data blobs and derived ZIPs for suite producers only.

Never read a working-tree data file. The ordinary mm-data Gradle ZIP tasks and
consumer staging paths remain in use outside suite builds.
"""

import argparse
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "gradle"))
from suite_archive_verifier import GENERATED, pinned_tree  # noqa: E402


def prepare(repo, pin, destination):
    repo, destination = Path(repo), Path(destination)
    blobs = pinned_tree(repo, pin)
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = Path(tempfile.mkdtemp(prefix="pinned-data-", dir=destination.parent))
    child = subprocess.Popen(["git", "-C", str(repo), "cat-file", "--batch"],
                             stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE)
    try:
        for name, oid in sorted(blobs.items()):
            parts = name.split("/")
            if (not all(part and part not in (".", "..") for part in parts)
                    or parts[0] != "data" or "\\" in name or "\x00" in name
                    or name in GENERATED):
                raise ValueError(f"unsafe or generated pinned data path: {name}")
            target = temporary.joinpath(*parts)
            target.parent.mkdir(parents=True, exist_ok=True)
            child.stdin.write((oid + "\n").encode("ascii"))
            child.stdin.flush()
            header = child.stdout.readline()
            match = re.fullmatch(rb"([0-9a-f]{40}) blob ([0-9]+)\n", header)
            if not match or match.group(1).decode("ascii") != oid:
                raise ValueError(f"invalid Git blob response for {name}")
            remaining = int(match.group(2))
            with target.open("wb") as output:
                while remaining:
                    chunk = child.stdout.read(min(1024 * 1024, remaining))
                    if not chunk:
                        raise ValueError(f"truncated Git blob: {name}")
                    output.write(chunk)
                    remaining -= len(chunk)
            if child.stdout.read(1) != b"\n":
                raise ValueError(f"invalid Git blob terminator: {name}")
        child.stdin.close()
        if child.wait() != 0:
            raise ValueError(f"Git cat-file failed: {child.stderr.read().decode(errors='replace')}")

        for output_name, prefix in GENERATED.items():
            target = temporary / "generated" / output_name
            target.parent.mkdir(parents=True, exist_ok=True)
            with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_DEFLATED,
                                 allowZip64=True) as archive:
                for name in sorted(blobs):
                    if not name.startswith(prefix):
                        continue
                    member = name[len(prefix):]
                    # Exactly the mm-data unitFilesZip top-level exclude rules.
                    if (prefix == "data/mekfiles/" and "/" not in member
                            and member.endswith((".txt", ".xml", ".cache"))):
                        continue
                    info = zipfile.ZipInfo(member, (1980, 1, 1, 0, 0, 0))
                    info.compress_type = zipfile.ZIP_DEFLATED
                    info.external_attr = 0o100644 << 16
                    with (temporary / name).open("rb") as source, archive.open(info, "w") as sink:
                        shutil.copyfileobj(source, sink, 1024 * 1024)
        if destination.exists():
            shutil.rmtree(destination)
        temporary.rename(destination)
    finally:
        if child.poll() is None:
            child.kill()
            child.wait()
        child.stdin.close()
        child.stdout.close()
        child.stderr.close()
        shutil.rmtree(temporary, ignore_errors=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-repo", required=True)
    parser.add_argument("--pin", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    try:
        prepare(args.data_repo, args.pin, args.output)
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print(f"suite pinned data preparation failed: {error}", file=sys.stderr)
        sys.exit(1)
