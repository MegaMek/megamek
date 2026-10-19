#!/usr/bin/env python3
"""Read-only, provisional suite release planning. Never publish from this module.

The inventory is deliberately not an authority for publication: remote archive
bytes, embedded identities and product release IDs must be verified separately.
"""

import argparse
import hashlib
import json
import re
import subprocess
import sys
import tempfile
import io
import shutil
from pathlib import Path

REPOS = {"MegaMek": "megamek", "MegaMekLab": "megameklab", "MekHQ": "mekhq"}
SOURCES = (*REPOS.values(), "mm-data")
SHA = re.compile(r"[0-9a-f]{40}\Z")
VERSION = re.compile(r"(0|[1-9][0-9]*)\.([0-9]+)\.([0-9]+)\Z")
MAX = 2147483647
MAX_RECORD_BYTES = 1024 * 1024
CHUNK_BYTES = 1024 * 1024
MAX_DOWNLOAD_BYTES = 4 * 1024**3


class UnsafeInventory(ValueError):
    pass


def failure_detail(error):
    """Bounded subprocess diagnostics without echoing commands or auth-bearing lines."""
    if isinstance(error, OSError):
        return f"could not start executable ({type(error).__name__}; check installation/PATH)"
    code = getattr(error, "returncode", "unknown")
    tails = []
    gradle_errors = []
    for output in (getattr(error, "stderr", None), getattr(error, "stdout", None)):
        if not output:
            continue
        if isinstance(output, bytes):
            output = output.decode("utf-8", errors="replace")
        lines = []
        for line in output.splitlines():
            # Do not display credentials, URLs with embedded credentials, or opaque
            # strings that could be tokens. Keep ordinary Gradle/gh error messages.
            if re.search(r"token|authorization|password|secret|cookie|https?://", line, re.I):
                continue
            line = re.sub(r"[A-Za-z0-9_+/\-=]{32,}", "[redacted]", line.strip())
            if line:
                lines.append(line[:200])
        tails.extend(lines[-5:])
        if "* What went wrong:" in lines:
            start = lines.index("* What went wrong:")
            end = next((i for i in range(start + 1, len(lines))
                        if lines[i].startswith("* ")), len(lines))
            gradle_errors.extend(lines[start:end][:10])
    # Gradle's trailing advice otherwise displaces the actual failure.
    lines = gradle_errors or tails
    return f"exit {code}" + (": " + " | ".join(lines) if lines else
                             " (inspect local command output for details)")


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise UnsafeInventory(f"duplicate JSON key: {key}")
        result[key] = value
    return result


def parse_json(data):
    return json.loads(data, object_pairs_hook=unique_object)


def numbers(value, allow_revision=False):
    if not isinstance(value, str):
        raise UnsafeInventory("version is not a string")
    parts = value.split(".")
    if allow_revision and len(parts) == 4 and parts[3].isdigit():
        parts = parts[:3]
    match = VERSION.fullmatch(".".join(parts))
    if not match:
        raise UnsafeInventory(f"invalid numeric version: {value}")
    result = tuple(map(int, match.groups()))
    if any(n > MAX for n in result):
        raise UnsafeInventory("version exceeds Java integer range")
    return result


def padded(parts):
    return f"{parts[0]}.{parts[1]:02d}.{parts[2]:02d}"


def canonical(value):
    return padded(numbers(value)) == value


def reserve(tags, releases, floor, membership="weekly"):
    """Treat aliases and legacy fourth-component tags/releases as occupied slots."""
    if not canonical(floor):
        raise UnsafeInventory("floor must be a canonical padded version")
    if membership not in ("weekly", "milestone", "development"):
        raise UnsafeInventory("unknown membership")
    ceiling = numbers(floor)
    for repo in REPOS.values():
        if (repo not in tags or not isinstance(tags[repo], list)
                or repo not in releases or not isinstance(releases[repo], list)):
            raise UnsafeInventory(f"missing tag/release inventory for {repo}")
        for tag in tags[repo] + releases[repo]:
            if not isinstance(tag, str):
                raise UnsafeInventory("invalid tag")
            if tag.startswith("v") and re.fullmatch(r"v[0-9]+(?:\.[0-9]+){2,3}", tag):
                ceiling = max(ceiling, numbers(tag[1:], allow_revision=True))
    if membership == "milestone" or ceiling[2] == MAX:
        if ceiling[1] == MAX:
            if ceiling[0] == MAX:
                raise UnsafeInventory("version space exhausted")
            return padded((ceiling[0] + 1, 0, 0))
        return padded((ceiling[0], ceiling[1] + 1, 0))
    return padded((ceiling[0], ceiling[1], ceiling[2] + 1))


def validate_previous(record):
    """Minimal strict schema-1 checks for selection; not an archive attestation."""
    if not isinstance(record, dict) or set(record) - {"schemaVersion", "version", "membership",
                                                       "tag", "products", "mmData",
                                                       "minimumLauncherVersion"}:
        raise UnsafeInventory("unexpected record keys")
    if type(record.get("schemaVersion")) is not int or record["schemaVersion"] != 1:
        raise UnsafeInventory("invalid schema")
    version = record.get("version")
    if not canonical(version) or record.get("tag") != "v" + version:
        raise UnsafeInventory("invalid suite version or tag")
    if record.get("membership") not in ("weekly", "milestone", "development"):
        raise UnsafeInventory("invalid membership")
    if "minimumLauncherVersion" in record:
        value = record["minimumLauncherVersion"]
        if not isinstance(value, str) or not re.fullmatch(r"(0|[1-9][0-9]*)"
                                                         r"(?:\.(0|[1-9][0-9]*)){2}", value):
            raise UnsafeInventory("invalid launcher version")
    products = record.get("products")
    if not isinstance(products, dict) or set(products) != set(REPOS):
        raise UnsafeInventory("invalid products")
    previous_versions = []
    for name, repo in REPOS.items():
        item = products[name]
        if not isinstance(item, dict) or set(item) != {
                "repository", "commit", "version", "tag", "releaseId", "asset"}:
            raise UnsafeInventory(f"invalid {name} keys")
        pv = item["version"]
        if (item["repository"] != f"https://github.com/MegaMek/{repo}"
                or not valid_sha(item["commit"]) or not canonical(pv)
                or item["tag"] != "v" + pv or numbers(pv) > numbers(version)
                or type(item["releaseId"]) is not int or item["releaseId"] <= 0
                or item["releaseId"] > 2**63 - 1):
            raise UnsafeInventory(f"invalid {name} identity")
        asset = item["asset"]
        if (not isinstance(asset, dict) or set(asset) != {"assetId", "name", "sha256", "size"}
                or type(asset["assetId"]) is not int or not 0 < asset["assetId"] <= 2**63 - 1
                or type(asset["size"]) is not int or not 0 < asset["size"] <= 2**63 - 1
                or asset["name"] != f"{name}-{pv}.tar.gz"
                or not isinstance(asset["sha256"], str)
                or not re.fullmatch(r"[0-9a-f]{64}", asset["sha256"])):
            raise UnsafeInventory(f"invalid {name} asset")
        previous_versions.append(numbers(pv))
    if previous_versions != sorted(previous_versions):
        raise UnsafeInventory("dependency versions out of order")
    data = record.get("mmData")
    if not isinstance(data, dict) or set(data) != {"repository", "commit"} or (
            data["repository"] != "https://github.com/MegaMek/mm-data"
            or not valid_sha(data["commit"])):
        raise UnsafeInventory("invalid mm-data identity")


def valid_sha(value):
    return isinstance(value, str) and SHA.fullmatch(value) is not None


def select(commits, previous, membership):
    if set(commits) != set(SOURCES) or not all(valid_sha(v) for v in commits.values()):
        raise UnsafeInventory("missing or invalid frozen source SHA")
    if membership not in ("weekly", "milestone", "development"):
        raise UnsafeInventory("invalid membership")
    if previous is None:
        return list(REPOS), False
    validate_previous(previous)
    old = previous["products"]
    mm = (old["MegaMek"]["commit"] != commits["megamek"]
          or previous["mmData"]["commit"] != commits["mm-data"])
    lab = mm or old["MegaMekLab"]["commit"] != commits["megameklab"]
    hq = lab or old["MekHQ"]["commit"] != commits["mekhq"]
    changed = [name for name, flag in zip(REPOS, (mm, lab, hq)) if flag]
    # Only a verified latest complete Weekly can be a real no-op. This is a
    # candidate, never a publication decision.
    candidate = not changed and membership == "weekly" and previous["membership"] == "weekly"
    return changed, candidate


def plan(inventory):
    if not isinstance(inventory, dict) or set(inventory) != {
            "commits", "tags", "releases", "previous", "membership", "floor"}:
        raise UnsafeInventory("inventory fields missing or unknown")
    previous = inventory["previous"]
    changed, candidate = select(inventory["commits"], previous, inventory["membership"])
    if previous is not None and numbers(inventory["floor"]) < numbers(previous["version"]):
        raise UnsafeInventory("floor predates previous record")
    version = reserve(inventory["tags"], inventory["releases"], inventory["floor"],
                      inventory["membership"])
    if previous is not None and numbers(version) <= numbers(previous["version"]):
        raise UnsafeInventory("suite version is not monotonic")
    return {
        "status": "PROVISIONAL_NONPUBLISHING",
        "versionCandidate": version,
        "membership": inventory["membership"],
        "frozenCommits": inventory["commits"],
        "buildCandidates": changed,
        "reuseCandidates": [name for name in REPOS if name not in changed],
        "weeklyNoopCandidate": candidate,
        "warning": "No record or release is verified or published by this plan",
    }


def gh_get(endpoint, runner=subprocess.run):
    # Only GET endpoints are accepted. No API write or arbitrary CLI command.
    match = re.fullmatch(r"repos/MegaMek/([a-z-]+)/"
                         r"(commits/(?:main|v[0-9]+\.[0-9]+\.[0-9]+)"
                         r"|git/ref/tags/v[0-9]+\.[0-9]+\.[0-9]+"
                         r"|(?:tags|releases)\?per_page=100&page=[1-9][0-9]*)",
                         endpoint)
    if (match is None or match[1] not in SOURCES
            or (match[1] == "mm-data" and match[2] != "commits/main")
            or (match[2].startswith(("commits/v", "git/ref/tags/"))
                and (match[1] not in REPOS.values()
                     or not canonical(match[2].rsplit("/", 1)[-1][1:])))):
        raise UnsafeInventory("unapproved read-only API endpoint")
    try:
        result = runner(["gh", "api", endpoint], capture_output=True, check=True)
    except (OSError, subprocess.CalledProcessError) as error:
        raise UnsafeInventory(f"gh API GET {endpoint} failed: {failure_detail(error)}") from None
    return parse_json(result.stdout)


def download_asset(repo, asset_id, destination, popen=subprocess.Popen):
    """Stream authenticated API bytes to disk with a hard download limit."""
    if repo not in REPOS.values() or type(asset_id) is not int or not 0 < asset_id <= 2**63 - 1:
        raise UnsafeInventory("invalid asset download identity")
    command = ["gh", "api", f"repos/MegaMek/{repo}/releases/assets/{asset_id}",
               "-H", "Accept: application/octet-stream"]
    try:
        with tempfile.TemporaryFile() as diagnostics:
            with popen(command, stdout=subprocess.PIPE, stderr=diagnostics) as process:
                size = 0
                with destination.open("wb") as output:
                    while chunk := process.stdout.read(CHUNK_BYTES):
                        size += len(chunk)
                        if size > MAX_DOWNLOAD_BYTES:
                            process.kill()
                            process.wait()
                            raise UnsafeInventory("asset download exceeds size limit")
                        output.write(chunk)
                code = process.wait()
            if code != 0:
                diagnostics.seek(0, io.SEEK_END)
                diagnostics.seek(max(0, diagnostics.tell() - 4096))
                detail = failure_detail(subprocess.CalledProcessError(
                    code, "gh api", stderr=diagnostics.read()))
                raise UnsafeInventory(
                    f"gh asset download {repo}/{asset_id} failed: {detail}")
    except OSError as error:
        raise UnsafeInventory(
            f"gh asset download {repo}/{asset_id} failed: {failure_detail(error)}") from None


def published_tag(entry):
    if not isinstance(entry, dict) or type(entry.get("draft")) is not bool:
        raise UnsafeInventory("incomplete release response")
    if entry["draft"]:
        return None
    if (type(entry.get("id")) is not int or entry["id"] <= 0
            or not isinstance(entry.get("tag_name"), str) or not entry["tag_name"]
            or not isinstance(entry.get("assets"), list)):
        raise UnsafeInventory("incomplete published release response")
    return entry["tag_name"]


def complete_record(releases, fetch=download_asset, allow_missing=False):
    """Read the latest *candidate* complete record; never interpret a partial as reusable.

    Release-list ordering is not a version ordering. A malformed record-shaped asset
    is an error rather than a reason to fall back to an older record.
    """
    if not isinstance(releases, list):
        raise UnsafeInventory("invalid MegaMek release list")
    found = []
    for release in releases:
        tag = published_tag(release)
        if tag is None:
            # Drafts cannot be used as a previously published complete record.
            continue
        for asset in release["assets"]:
            if not isinstance(asset, dict) or not isinstance(asset.get("name"), str):
                raise UnsafeInventory("incomplete release asset response")
            if not asset["name"].startswith("suite-record-"):
                continue
            name = asset["name"]
            if not re.fullmatch(r"suite-record-[0-9]+\.[0-9]+\.[0-9]+\.json", name):
                raise UnsafeInventory("malformed complete-record asset name")
            if type(asset.get("id")) is not int or asset["id"] <= 0 or (
                    type(asset.get("size")) is not int) or asset["size"] <= 0 or (
                    asset.get("state") != "uploaded"):
                raise UnsafeInventory("incomplete record asset")
            if asset["size"] > MAX_RECORD_BYTES:
                raise UnsafeInventory("complete-record asset exceeds size limit")
            with tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / "record.json"
                fetch("megamek", asset["id"], path)
                if path.stat().st_size != asset["size"]:
                    raise UnsafeInventory("complete-record asset size mismatch")
                record = parse_json(path.read_bytes())
            validate_previous(record)
            # A new suite release can reference an older MegaMek product release.
            if name != f"suite-record-{record['version']}.json" or tag != record["tag"]:
                raise UnsafeInventory("record release identity mismatch")
            found.append(record)
    if not found:
        if allow_missing:
            return None
        raise UnsafeInventory("no complete record: first publication needs explicit bootstrap review")
    versions = [numbers(r["version"]) for r in found]
    if len(set(versions)) != len(versions):
        raise UnsafeInventory("duplicate complete suite version")
    return found[versions.index(max(versions))]


def check_product_assets(record, releases, fetch=download_asset, attest=None, retain=None):
    """Check release/asset IDs and bytes; optionally attest all three retained archives."""
    validate_previous(record)
    with tempfile.TemporaryDirectory() as directory:
        paths = {}
        for product, repo in REPOS.items():
            item = record["products"][product]
            matches = [r for r in releases[repo] if r.get("id") == item["releaseId"]]
            if len(matches) != 1 or matches[0].get("draft") is not False or (
                    matches[0].get("tag_name") != item["tag"]):
                raise UnsafeInventory(f"missing release identity for {product}")
            assets = matches[0].get("assets")
            if not isinstance(assets, list):
                raise UnsafeInventory(f"missing assets for {product}")
            expected = item["asset"]
            matches = [a for a in assets if a.get("id") == expected["assetId"]]
            if len(matches) != 1 or any(matches[0].get(k) != v for k, v in (
                    ("name", expected["name"]), ("size", expected["size"]), ("state", "uploaded"))):
                raise UnsafeInventory(f"missing asset identity for {product}")
            if attest is not None and expected["size"] > 4 * 1024**3:
                raise UnsafeInventory(f"archive exceeds attestation size limit: {product}")
            path = Path(directory) / expected["name"]
            fetch(repo, expected["assetId"], path)
            digest = hashlib.sha256()
            size = 0
            with path.open("rb") as stream:
                while chunk := stream.read(CHUNK_BYTES):
                    size += len(chunk)
                    if size > expected["size"]:
                        raise UnsafeInventory(f"asset bytes differ for {product}")
                    digest.update(chunk)
            if size != expected["size"] or digest.hexdigest() != expected["sha256"]:
                raise UnsafeInventory(f"asset bytes differ for {product}")
            paths[product] = path
        if attest is not None:
            attest(record, paths)
        if retain is not None:
            target = Path(retain)
            if target.exists():
                raise UnsafeInventory("prior archive directory already exists")
            target.mkdir()
            for product, path in paths.items():
                shutil.copyfile(path, target / path.name)


def release_details(getter=gh_get):
    result = {}
    for repo in REPOS.values():
        entries = []
        page = 1
        while True:
            batch = getter(f"repos/MegaMek/{repo}/releases?per_page=100&page={page}")
            if not isinstance(batch, list) or len(batch) > 100:
                raise UnsafeInventory("invalid release page")
            for entry in batch:
                published_tag(entry)
            entries.extend(batch)
            if len(batch) < 100:
                break
            page += 1
        result[repo] = entries
    return result


def discovered_inventory(membership, getter=gh_get, fetch=download_asset,
                         bootstrap=False, bootstrap_floor=None, attest=None, retain=None):
    """Remote preflight, including archive attestation; still no publication authority."""
    details = release_details(getter)
    if bootstrap and bootstrap_floor is None:
        raise UnsafeInventory("bootstrap requires an explicit canonical floor")
    if not bootstrap and bootstrap_floor is not None:
        raise UnsafeInventory("bootstrap floor requires explicit bootstrap")
    if bootstrap and not canonical(bootstrap_floor):
        raise UnsafeInventory("bootstrap floor must be canonical")
    previous = complete_record(details["megamek"], fetch, allow_missing=bootstrap)
    if bootstrap and previous is not None:
        raise UnsafeInventory("bootstrap refused: complete record already exists")
    if previous is not None:
        if attest is None:
            from suite_archive_attestation import attest_archives
            attest = attest_archives
        check_product_assets(previous, details, fetch, attest, retain)
    inventory = freeze_and_inventory(
        membership, bootstrap_floor if bootstrap else previous["version"], getter)
    if any(inventory["releases"][repo] != [
            tag for entry in details[repo] if (tag := published_tag(entry)) is not None]
           for repo in REPOS.values()):
        raise UnsafeInventory("release inventory moved during discovery")
    inventory["previous"] = previous
    return inventory


def release_inventory(getter=gh_get):
    """Read tags and published releases without replacing frozen source commits."""
    tags = {}
    releases = {}
    for repo in REPOS.values():
        tags[repo] = []
        releases[repo] = []
        for kind, target, field in (("tags", tags[repo], "name"),
                                    ("releases", releases[repo], "tag_name")):
            page = 1
            while True:
                entries = getter(f"repos/MegaMek/{repo}/{kind}?per_page=100&page={page}")
                if not isinstance(entries, list) or len(entries) > 100:
                    raise UnsafeInventory(f"invalid {kind} for {repo}")
                if kind == "tags":
                    if any(not isinstance(e, dict) or not isinstance(e.get(field), str)
                           for e in entries):
                        raise UnsafeInventory(f"invalid tags for {repo}")
                    target.extend(e[field] for e in entries)
                else:
                    target.extend(tag for e in entries if (tag := published_tag(e)) is not None)
                if len(entries) < 100:
                    break
                page += 1
    return {"tags": tags, "releases": releases}


def freeze_and_inventory(membership, floor, getter=gh_get):
    """Capture main once; previous is null until separately attested."""
    commits = {}
    for repo in SOURCES:
        head = getter(f"repos/MegaMek/{repo}/commits/main")
        sha = head.get("sha") if isinstance(head, dict) else None
        if not valid_sha(sha):
            raise UnsafeInventory(f"invalid main HEAD for {repo}")
        commits[repo] = sha
    return {"commits": commits, **release_inventory(getter), "previous": None,
            "membership": membership, "floor": floor}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inventory", type=Path, help="offline JSON inventory (never authoritative)")
    parser.add_argument("--membership", choices=("weekly", "milestone", "development"),
                        default="weekly")
    parser.add_argument("--floor", default="0.51.00",
                        help="canonical version floor, not a version reservation")
    parser.add_argument("--bootstrap", action="store_true",
                        help="read-only first-record candidate; requires --bootstrap-floor")
    parser.add_argument("--bootstrap-floor",
                        help="explicit canonical floor for read-only bootstrap candidate")
    args = parser.parse_args()
    try:
        if args.inventory and (args.bootstrap or args.bootstrap_floor is not None):
            raise UnsafeInventory("bootstrap options require remote discovery")
        inventory = (parse_json(args.inventory.read_bytes()) if args.inventory
                     else discovered_inventory(args.membership, bootstrap=args.bootstrap,
                                               bootstrap_floor=args.bootstrap_floor))
        print(json.dumps(plan(inventory), indent=2))
        # Deliberately nonzero: an unverified plan must never look like a release.
        print("BLOCKED: archive attestation, source-ref checks and publication are not implemented",
              file=sys.stderr)
        return 2
    except (UnsafeInventory, ValueError, OSError, subprocess.CalledProcessError) as error:
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
