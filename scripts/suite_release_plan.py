#!/usr/bin/env python3
"""Read-only suite release planning for the Core and Experimental channels. Never publishes.

Experimental builds the four repositories' ``main`` branches. Core builds the newest
``releases/<line>`` branch, which must exist in all four. The release line
(``major.minor.patch``) comes from MegaMek's ``Version.properties`` at the frozen commit; the
fourth component is the next build number not yet used on that line by any tag or published
release of the three game repositories. Draft releases do not reserve a number; publication
refuses a candidate whose tag a draft already holds. MegaMek, MegaMekLab and MekHQ always share
that version.

The inventory is deliberately not an authority for publication: built archive bytes,
embedded identities and product release IDs are verified separately.
"""

import argparse
import base64
import io
import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

REPOS = {"MegaMek": "megamek", "MegaMekLab": "megameklab", "MekHQ": "mekhq"}
SOURCES = (*REPOS.values(), "mm-data")
CHANNELS = ("core", "experimental")
SCHEMA_VERSION = 2
SHA = re.compile(r"[0-9a-f]{40}\Z")
# major.minor.patch[.build]; the unpadded build number is 1 or more (0.51.01.1).
VERSION = re.compile(r"(0|[1-9][0-9]*)\.([0-9]+)\.([0-9]+)(?:\.([1-9][0-9]*))?\Z")
TAG = re.compile(r"v([0-9]+)\.([0-9]+)\.([0-9]+)(?:\.([0-9]+))?\Z")
RELEASE_BRANCH = re.compile(r"releases/(.+)\Z")
MAX = 2147483647
MAX_RECORD_BYTES = 1024 * 1024
CHUNK_BYTES = 1024 * 1024
MAX_DOWNLOAD_BYTES = 4 * 1024**3
VERSION_PROPERTIES = "megamek/resources/Version.properties"


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


def numbers(value):
    """(major, minor, patch, build); build 0 means a three-component version."""
    if not isinstance(value, str):
        raise UnsafeInventory("version is not a string")
    match = VERSION.fullmatch(value)
    if not match:
        raise UnsafeInventory(f"invalid numeric version: {value}")
    result = tuple(int(n or 0) for n in match.groups())
    if any(n > MAX for n in result):
        raise UnsafeInventory("version exceeds Java integer range")
    return result


def padded(parts):
    base = f"{parts[0]}.{parts[1]:02d}.{parts[2]:02d}"
    return base + (f".{parts[3]}" if len(parts) > 3 and parts[3] else "")


def canonical(value):
    return padded(numbers(value)) == value


def tag_numbers(tag):
    """Numeric identity of a historical tag (v0.51.0 == v0.51.00); None if not a version tag."""
    match = TAG.fullmatch(tag)
    if match is None:
        return None
    result = tuple(int(n or 0) for n in match.groups())
    return result if all(n <= MAX for n in result) else None


def canonical_line(value):
    """``value`` when it is a canonical three-component line such as 0.51.01, otherwise None."""
    try:
        return value if canonical(value) and numbers(value)[3] == 0 else None
    except UnsafeInventory:
        return None


def branch_line(branch):
    """Line named by a Core branch such as ``releases/0.51.01``; None for any other name."""
    match = RELEASE_BRANCH.fullmatch(branch) if isinstance(branch, str) else None
    return canonical_line(match[1]) if match else None


def properties_line(text):
    """Release line from the major, minor and patch entries of Version.properties."""
    values = {}
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        if separator:
            values[key.strip()] = value.strip()
    try:
        parts = tuple(int(values[key]) for key in ("major", "minor", "patch"))
    except (KeyError, ValueError):
        raise UnsafeInventory("Version.properties lacks numeric major, minor and patch") from None
    if any(part < 0 or part > MAX for part in parts):
        raise UnsafeInventory("Version.properties component outside Java integer range")
    return padded(parts)


def reserve(tags, releases, line):
    """The next build number on ``line`` unused by any tag or published release (drafts excluded).

    Release line 0.51.01 with tags v0.51.01 and v0.51.01.1 allocates 0.51.01.2. Numerically
    equal spellings (v0.51.1.1) occupy the same build.
    """
    if canonical_line(line) is None:
        raise UnsafeInventory(f"invalid release line: {line}")
    base = numbers(line)[:3]
    occupied = set()
    for repo in REPOS.values():
        if (repo not in tags or not isinstance(tags[repo], list)
                or repo not in releases or not isinstance(releases[repo], list)):
            raise UnsafeInventory(f"missing tag/release inventory for {repo}")
        for tag in tags[repo] + releases[repo]:
            if not isinstance(tag, str):
                raise UnsafeInventory("invalid tag")
            if (parts := tag_numbers(tag)) is not None:
                occupied.add(parts)
    build = max((parts[3] for parts in occupied if parts[:3] == base), default=0) + 1
    if build > MAX:
        raise UnsafeInventory("build number space exhausted")
    return padded((*base, build))


def valid_sha(value):
    return isinstance(value, str) and SHA.fullmatch(value) is not None


def validate_record(record):
    """Strict schema-2 checks of a complete suite record; not an archive attestation."""
    required = {"schemaVersion", "version", "channel", "tag", "products", "mmData"}
    if (not isinstance(record, dict) or not required <= set(record)
            or set(record) - required - {"minimumLauncherVersion"}):
        raise UnsafeInventory("unexpected record keys")
    if type(record["schemaVersion"]) is not int or record["schemaVersion"] != SCHEMA_VERSION:
        raise UnsafeInventory("invalid schema")
    version = record["version"]
    if not canonical(version) or numbers(version)[3] == 0 or record["tag"] != "v" + version:
        raise UnsafeInventory("invalid suite version or tag")
    if record["channel"] not in CHANNELS:
        raise UnsafeInventory("invalid channel")
    if "minimumLauncherVersion" in record:
        value = record["minimumLauncherVersion"]
        if not isinstance(value, str) or not re.fullmatch(r"(0|[1-9][0-9]*)"
                                                         r"(?:\.(0|[1-9][0-9]*)){2}", value):
            raise UnsafeInventory("invalid launcher version")
    products = record["products"]
    if not isinstance(products, dict) or set(products) != set(REPOS):
        raise UnsafeInventory("invalid products")
    for name, repo in REPOS.items():
        item = products[name]
        if not isinstance(item, dict) or set(item) != {
                "repository", "commit", "version", "tag", "releaseId", "asset"}:
            raise UnsafeInventory(f"invalid {name} keys")
        if (item["repository"] != f"https://github.com/MegaMek/{repo}"
                or not valid_sha(item["commit"]) or item["version"] != version
                or item["tag"] != record["tag"]
                or type(item["releaseId"]) is not int or item["releaseId"] <= 0
                or item["releaseId"] > 2**63 - 1):
            raise UnsafeInventory(f"invalid {name} identity")
        asset = item["asset"]
        if (not isinstance(asset, dict) or set(asset) != {"assetId", "name", "sha256", "size"}
                or type(asset["assetId"]) is not int or not 0 < asset["assetId"] <= 2**63 - 1
                or type(asset["size"]) is not int or not 0 < asset["size"] <= 2**63 - 1
                or asset["name"] != f"{name}-{version}.tar.gz"
                or not isinstance(asset["sha256"], str)
                or not re.fullmatch(r"[0-9a-f]{64}", asset["sha256"])):
            raise UnsafeInventory(f"invalid {name} asset")
    data = record["mmData"]
    if not isinstance(data, dict) or set(data) != {"repository", "commit"} or (
            data["repository"] != "https://github.com/MegaMek/mm-data"
            or not valid_sha(data["commit"])):
        raise UnsafeInventory("invalid mm-data identity")


def record_commits(record):
    commits = {repo: record["products"][product]["commit"] for product, repo in REPOS.items()}
    commits["mm-data"] = record["mmData"]["commit"]
    return commits


def plan(inventory):
    """Validate a frozen inventory and propose the version; never a publication decision."""
    if not isinstance(inventory, dict) or set(inventory) != {
            "channel", "branch", "rehearsal", "line", "commits", "tags", "releases", "previous"}:
        raise UnsafeInventory("inventory fields missing or unknown")
    channel = inventory["channel"]
    branch = inventory["branch"]
    rehearsal = inventory["rehearsal"]
    line = inventory["line"]
    commits = inventory["commits"]
    if channel not in CHANNELS or type(rehearsal) is not bool:
        raise UnsafeInventory("invalid channel or rehearsal flag")
    if canonical_line(line) is None:
        raise UnsafeInventory(f"invalid release line: {line}")
    if (not isinstance(commits, dict) or set(commits) != set(SOURCES)
            or not all(valid_sha(value) for value in commits.values())):
        raise UnsafeInventory("missing or invalid frozen source SHA")
    if channel == "experimental":
        if branch != "main" or rehearsal:
            raise UnsafeInventory("Experimental builds main")
    elif rehearsal:
        if branch != "main":
            raise UnsafeInventory("a cut rehearsal builds main")
    elif branch_line(branch) != line:
        raise UnsafeInventory(f"Core branch {branch} must build its own line, "
                              f"but Version.properties says {line}")
    previous = inventory["previous"]
    if previous is not None:
        validate_record(previous)
        if previous["channel"] != channel:
            raise UnsafeInventory("previous record belongs to another channel")
    version = reserve(inventory["tags"], inventory["releases"], line)
    if previous is not None and numbers(version) <= numbers(previous["version"]):
        raise UnsafeInventory(f"{version} is not newer than the latest {channel} "
                              f"release {previous['version']}")
    return {
        "status": "PROVISIONAL_NONPUBLISHING",
        "channel": channel,
        "branch": branch,
        "rehearsal": rehearsal,
        "versionCandidate": version,
        "previousTag": previous["tag"] if previous is not None else None,
        "frozenCommits": commits,
        # Nothing changed since this channel's latest release: publish nothing.
        "noop": previous is not None and record_commits(previous) == commits,
        "warning": "No record or release is verified or published by this plan",
    }


API = re.compile(
    r"repos/MegaMek/(?P<repo>[a-z-]+)/(?P<path>"
    r"git/ref/heads/(?P<branch>main|releases/[0-9]+\.[0-9]+\.[0-9]+)"
    r"|git/matching-refs/heads/releases/\?per_page=100"
    r"|contents/megamek/resources/Version\.properties\?ref=[0-9a-f]{40}"
    r"|commits/v(?P<commit_tag>[0-9]+\.[0-9]+\.[0-9]+(?:\.[0-9]+)?)"
    r"|git/ref/tags/v(?P<ref_tag>[0-9]+\.[0-9]+\.[0-9]+(?:\.[0-9]+)?)"
    r"|(?:tags|releases)\?per_page=100&page=[1-9][0-9]*)\Z")


def gh_get(endpoint, runner=subprocess.run):
    # Only GET endpoints are accepted. No API write or arbitrary CLI command.
    match = API.fullmatch(endpoint)
    if match is None or match["repo"] not in SOURCES:
        raise UnsafeInventory("unapproved read-only API endpoint")
    path, repo, tag = match["path"], match["repo"], match["commit_tag"] or match["ref_tag"]
    if ((match["branch"] not in (None, "main") and branch_line(match["branch"]) is None)
            or (path.startswith("contents/") and repo != "megamek")
            or (repo == "mm-data" and not path.startswith(("git/ref/heads/", "git/matching-refs/")))
            or (tag is not None and not canonical(tag))):
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
    """Tag of a published release; None for a draft. Malformed entries fail closed."""
    if not isinstance(entry, dict) or type(entry.get("draft")) is not bool:
        raise UnsafeInventory("incomplete release response")
    if entry["draft"]:
        return None
    if (type(entry.get("id")) is not int or entry["id"] <= 0
            or not isinstance(entry.get("tag_name"), str) or not entry["tag_name"]
            or not isinstance(entry.get("assets"), list)):
        raise UnsafeInventory("incomplete published release response")
    return entry["tag_name"]


def release_tag(entry):
    """Tag name of any release, including a draft; validates the entry either way."""
    published_tag(entry)
    if not isinstance(entry.get("tag_name"), str):
        raise UnsafeInventory("incomplete release response")
    return entry["tag_name"]


def published_release_tags(entries):
    """Tags of the published releases among ``entries``, in order.

    Drafts are left out: only tokens with write access can list them, so the read-only build
    and the publish recheck would otherwise see different inventories. Publication still
    refuses a suite tag held by any release, drafts included.
    """
    tags = [release_tag(entry) for entry in entries]  # Validates drafts too.
    return [tag for tag, entry in zip(tags, entries) if not entry["draft"]]


def complete_records(releases, fetch=download_asset):
    """Every complete record attached to a published MegaMek release, oldest first.

    A malformed record-shaped asset is an error rather than a reason to ignore it.
    """
    if not isinstance(releases, list):
        raise UnsafeInventory("invalid MegaMek release list")
    found = []
    for release in releases:
        tag = published_tag(release)
        if tag is None:
            continue
        for asset in release["assets"]:
            if not isinstance(asset, dict) or not isinstance(asset.get("name"), str):
                raise UnsafeInventory("incomplete release asset response")
            name = asset["name"]
            if not name.startswith("suite-record-"):
                continue
            if not re.fullmatch(r"suite-record-[0-9]+\.[0-9]+\.[0-9]+(?:\.[0-9]+)?\.json", name):
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
            validate_record(record)
            if name != f"suite-record-{record['version']}.json" or tag != record["tag"]:
                raise UnsafeInventory("record release identity mismatch")
            found.append(record)
    versions = [numbers(record["version"]) for record in found]
    if len(set(versions)) != len(versions):
        raise UnsafeInventory("duplicate complete suite version")
    return sorted(found, key=lambda record: numbers(record["version"]))


def latest(records, channel):
    """The newest complete record of ``channel``, or None."""
    matching = [record for record in records if record["channel"] == channel]
    return matching[-1] if matching else None


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
                release_tag(entry)
            entries.extend(batch)
            if len(batch) < 100:
                break
            page += 1
        result[repo] = entries
    return result


def release_inventory(getter=gh_get):
    """Every tag and published release tag of the three game repositories."""
    tags = {}
    releases = {}
    for repo in REPOS.values():
        tags[repo] = []
        releases[repo] = []
        for kind, target in (("tags", tags[repo]), ("releases", releases[repo])):
            page = 1
            while True:
                entries = getter(f"repos/MegaMek/{repo}/{kind}?per_page=100&page={page}")
                if not isinstance(entries, list) or len(entries) > 100:
                    raise UnsafeInventory(f"invalid {kind} for {repo}")
                if kind == "tags":
                    if any(not isinstance(e, dict) or not isinstance(e.get("name"), str)
                           for e in entries):
                        raise UnsafeInventory(f"invalid tags for {repo}")
                    target.extend(e["name"] for e in entries)
                else:
                    target.extend(published_release_tags(entries))
                if len(entries) < 100:
                    break
                page += 1
    return {"tags": tags, "releases": releases}


def branch_head(repo, branch, getter=gh_get):
    ref = getter(f"repos/MegaMek/{repo}/git/ref/heads/{branch}")
    target = ref.get("object") if isinstance(ref, dict) else None
    if (not isinstance(target, dict) or ref.get("ref") != f"refs/heads/{branch}"
            or target.get("type") != "commit" or not valid_sha(target.get("sha"))):
        raise UnsafeInventory(f"invalid {branch} head for {repo}")
    return target["sha"]


def freeze(branch, getter=gh_get):
    """Capture the branch head of all four repositories once."""
    return {repo: branch_head(repo, branch, getter) for repo in SOURCES}


def release_branches(repo, getter=gh_get):
    """Canonical ``releases/<line>`` branch names of a repository, oldest line first."""
    refs = getter(f"repos/MegaMek/{repo}/git/matching-refs/heads/releases/?per_page=100")
    if not isinstance(refs, list) or len(refs) >= 100:
        raise UnsafeInventory(f"invalid or unexpectedly long release branch list for {repo}")
    names = []
    for ref in refs:
        if not isinstance(ref, dict) or not isinstance(ref.get("ref"), str):
            raise UnsafeInventory(f"invalid release branch entry for {repo}")
        name = ref["ref"].removeprefix("refs/heads/")
        if branch_line(name) is not None:
            names.append(name)
    return sorted(names, key=lambda name: numbers(branch_line(name)))


def newest_release_branch(getter=gh_get):
    branches = release_branches("megamek", getter)
    if not branches:
        raise UnsafeInventory("no releases/<line> branch exists; run the Core cut first")
    return branches[-1]


def version_line(megamek_sha, getter=gh_get):
    """Release line of MegaMek's Version.properties at an exact commit."""
    data = getter(f"repos/MegaMek/megamek/contents/{VERSION_PROPERTIES}?ref={megamek_sha}")
    if (not isinstance(data, dict) or data.get("encoding") != "base64"
            or not isinstance(data.get("content"), str)):
        raise UnsafeInventory("unreadable Version.properties")
    try:
        text = base64.b64decode(data["content"], validate=False).decode("utf-8")
    except (ValueError, UnicodeError):
        raise UnsafeInventory("unreadable Version.properties") from None
    return properties_line(text)


def discovered_inventory(channel, branch=None, rehearsal=False, getter=gh_get,
                         fetch=download_asset):
    """Remote preflight of one channel; still no publication authority."""
    if channel not in CHANNELS:
        raise UnsafeInventory("unknown channel")
    if branch is None:
        branch = "main" if channel == "experimental" or rehearsal else newest_release_branch(getter)
    details = release_details(getter)
    records = complete_records(details["megamek"], fetch)
    commits = freeze(branch, getter)
    line = version_line(commits["megamek"], getter)
    if channel == "experimental" and f"releases/{line}" in release_branches("megamek", getter):
        raise UnsafeInventory(f"main is still on line {line}, which already has a Core branch; "
                              "bump Version.properties on main first")
    inventory = {
        "channel": channel, "branch": branch, "rehearsal": rehearsal,
        "line": line, "commits": commits,
        **release_inventory(getter), "previous": latest(records, channel),
    }
    if any(inventory["releases"][repo] != published_release_tags(details[repo])
           for repo in REPOS.values()):
        raise UnsafeInventory("release inventory moved during discovery")
    plan(inventory)
    return inventory


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--channel", choices=CHANNELS, default="experimental")
    parser.add_argument("--branch", help="override the channel's branch (Core only)")
    args = parser.parse_args()
    try:
        print(json.dumps(plan(discovered_inventory(args.channel, args.branch)), indent=2))
        return 0
    except (UnsafeInventory, ValueError, OSError, subprocess.CalledProcessError) as error:
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
