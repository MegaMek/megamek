#!/usr/bin/env python3
"""Guarded, single-attempt write phase. Not a workflow entry point.

The caller must supply an approved inventory and the three archives from the pinned
nonpublishing build. A failure may leave product tags and releases, but never a partial
suite record; the next run allocates the next build number and starts over.
"""

import hashlib
import json
import os
import subprocess
import tempfile
from pathlib import Path

from suite_release_plan import (
    REPOS, SOURCES, UnsafeInventory, canonical, complete_records, download_asset,
    failure_detail, gh_get, latest, parse_json, plan, release_details, release_inventory,
    valid_sha, validate_record,
)

from suite_archive_attestation import attest_archives
from suite_pinned_build import clean, run, stage

# GitHub rejects release bodies over 125,000 characters; leave room for the suite header.
NOTES_LIMIT = 100_000


def sha_size(path):
    digest = hashlib.sha256()
    size = 0
    with Path(path).open("rb") as stream:
        while block := stream.read(1024 * 1024):
            size += len(block)
            digest.update(block)
    if not size or size > 4 * 1024**3:
        raise UnsafeInventory("archive size outside download limit")
    return digest.hexdigest(), size


def api(command, *, payload=None, input_file=None, runner=subprocess.run):
    """Only fixed GitHub API write shapes; never retry a possibly successful POST."""
    with tempfile.TemporaryDirectory(prefix="suite-api-") as temporary:
        if payload is not None:
            # A file keeps long release bodies off the command line.
            input_file = Path(temporary) / "payload.json"
            input_file.write_text(json.dumps(payload), encoding="utf-8")
        args = ["gh", "api", *command]
        if input_file is not None:
            args += ["--input", str(input_file)]
        try:
            result = runner(args, capture_output=True, check=True)
        except (OSError, subprocess.CalledProcessError) as error:
            raise UnsafeInventory(f"write failed (do not retry): {failure_detail(error)}") from None
    return parse_json(result.stdout)


class GitHubWrites:
    def notes(self, repo, tag, sha, previous_tag):
        """GitHub's generated notes (merged PRs) since ``previous_tag``. Writes nothing."""
        payload = {"tag_name": tag, "target_commitish": sha}
        if previous_tag is not None:
            payload["previous_tag_name"] = previous_tag
        result = api(["--method", "POST", f"repos/MegaMek/{repo}/releases/generate-notes"],
                     payload=payload)
        if not isinstance(result, dict) or not isinstance(result.get("body"), str):
            raise UnsafeInventory("release notes response mismatch")
        return result["body"]

    def tag(self, repo, tag, sha):
        result = api(["--method", "POST", f"repos/MegaMek/{repo}/git/refs"],
                     payload={"ref": f"refs/tags/{tag}", "sha": sha})
        if result.get("ref") != f"refs/tags/{tag}" or result.get("object", {}).get("sha") != sha:
            raise UnsafeInventory("tag creation response mismatch")

    def release(self, repo, tag, sha, *, name, body, prerelease):
        result = api(["--method", "POST", f"repos/MegaMek/{repo}/releases"], payload={
            "tag_name": tag, "target_commitish": sha, "name": name, "body": body,
            "draft": False, "prerelease": prerelease,
            # Only Core becomes the repository's "Latest" release.
            "make_latest": "false" if prerelease else "true",
        })
        if (result.get("tag_name") != tag or result.get("draft") is not False
                or result.get("prerelease") is not prerelease
                or result.get("name") != name or result.get("body") != body
                or type(result.get("id")) is not int or result["id"] <= 0):
            raise UnsafeInventory("release creation response mismatch")
        return result["id"]

    def upload(self, repo, release_id, path):
        name = Path(path).name
        # gh api accepts absolute upload host URLs and --input sends raw file bytes.
        result = api(["--method", "POST",
                      f"https://uploads.github.com/repos/MegaMek/{repo}/releases/"
                      f"{release_id}/assets?name={name}",
                      "-H", "Content-Type: application/octet-stream"], input_file=path)
        if (type(result.get("id")) is not int or result["id"] <= 0
                or result.get("name") != name or result.get("state") != "uploaded"):
            raise UnsafeInventory("upload response mismatch")
        return result["id"]


def release_presentation(record, product, notes):
    experimental = record["channel"] == "experimental"
    version = record["version"]
    name = f"{product} {version}" + (" (Experimental)" if experimental else "")
    record_url = (f"https://github.com/MegaMek/megamek/releases/download/"
                  f"{record['tag']}/suite-record-{version}.json")
    lines = [
        f"**Channel: {'Experimental' if experimental else 'Core'}**",
        "",
        ("Experimental builds come from the development branch. They have the newest "
         "features and the newest bugs." if experimental else
         "Core is the stable release line."),
        "",
        f"MegaMek, MegaMekLab and MekHQ {version} are released together. The "
        f"[suite record]({record_url}) lists the exact downloads and is uploaded last, "
        "after every archive has been verified.",
        "",
        "| Product | Download | Source |",
        "| --- | --- | --- |",
    ]
    for included, item in record["products"].items():
        url = (f"{item['repository']}/releases/download/{item['tag']}/"
               f"{item['asset']['name']}")
        source = f"{item['repository']}/commit/{item['commit']}"
        lines.append(f"| {included} | [{item['asset']['name']}]({url}) | "
                     f"[`{item['commit'][:12]}`]({source}) |")
    data = record["mmData"]
    lines += [f"| mm-data | | [`{data['commit'][:12]}`]({data['repository']}/commit/"
              f"{data['commit']}) |"]
    notes = notes.strip()
    if len(notes) > NOTES_LIMIT:
        notes = notes[:NOTES_LIMIT].rsplit("\n", 1)[0] + "\n\n(Release notes truncated.)"
    if notes:
        lines += ["", notes]
    return {"name": name, "body": "\n".join(lines) + "\n",
            "prerelease": experimental}


def ref_commit(repo, tag, getter):
    if (repo not in REPOS.values() or not isinstance(tag, str)
            or not tag.startswith("v") or not canonical(tag[1:])):
        raise UnsafeInventory("invalid ref lookup")
    ref = getter(f"repos/MegaMek/{repo}/git/ref/tags/{tag}")
    if not isinstance(ref, dict) or ref.get("ref") != f"refs/tags/{tag}":
        raise UnsafeInventory("missing exact tag ref")
    obj = ref.get("object")
    if not isinstance(obj, dict) or obj.get("type") not in ("commit", "tag") or not valid_sha(obj.get("sha")):
        raise UnsafeInventory("invalid tag ref object")
    # GitHub's commits endpoint peels annotated tags to the commit.
    commit = getter(f"repos/MegaMek/{repo}/commits/{tag}")
    sha = commit.get("sha") if isinstance(commit, dict) else None
    if not valid_sha(sha) or (obj["type"] == "commit" and sha != obj["sha"]):
        raise UnsafeInventory("tag commit mismatch")
    return sha


def require_tag_absent(repo, tag, getter):
    """Check every tag page just before a write; malformed pages fail closed."""
    page = 1
    while True:
        entries = getter(f"repos/MegaMek/{repo}/tags?per_page=100&page={page}")
        if (not isinstance(entries, list) or len(entries) > 100
                or any(not isinstance(entry, dict) or not isinstance(entry.get("name"), str)
                       for entry in entries)):
            raise UnsafeInventory("invalid tag page before publication")
        if any(entry["name"] == tag for entry in entries):
            raise UnsafeInventory("publication collision; rerun to allocate the next build")
        if len(entries) < 100:
            return
        page += 1


def exact_asset(item, details, repo, fetch, directory, getter):
    if ref_commit(repo, item["tag"], getter) != item["commit"]:
        raise UnsafeInventory(f"tag moved or wrong commit: {repo}")
    matches = [r for r in details if r.get("id") == item["releaseId"]]
    if len(matches) != 1 or matches[0].get("tag_name") != item["tag"] or matches[0].get("draft") is not False:
        raise UnsafeInventory(f"release identity mismatch: {repo}")
    assets = matches[0].get("assets")
    if not isinstance(assets, list):
        raise UnsafeInventory("missing release assets")
    expected = item["asset"]
    matches = [a for a in assets if a.get("id") == expected["assetId"]]
    if (len(matches) != 1 or any(matches[0].get(key) != expected[key]
                                 for key in ("name", "size"))
            or matches[0].get("state") != "uploaded"):
        raise UnsafeInventory(f"asset identity mismatch: {repo}")
    path = directory / expected["name"]
    fetch(repo, expected["assetId"], path)
    if sha_size(path) != (expected["sha256"], expected["size"]):
        raise UnsafeInventory(f"remote asset bytes mismatch: {repo}")


def publish(inventory, archives, *, getter=gh_get, fetch=download_asset,
            writes=None, attest=None, validate=None):
    """Publish after a successful pinned build, with no automatic retry or rollback.

    `validate` must run :megamek:validateSuiteRecord from the pinned MegaMek
    checkout. The caller is responsible for the pinned archive attestation and for
    providing a scoped App token only to writes.
    """
    if writes is None or attest is None or validate is None:
        raise UnsafeInventory("missing write, archive attestation or record validator")
    proposal = plan(inventory)
    if proposal["rehearsal"]:
        raise UnsafeInventory("a cut rehearsal never publishes")
    if proposal["noop"]:
        raise UnsafeInventory("nothing changed since the previous release; no publication")
    channel = proposal["channel"]
    previous = inventory["previous"]
    if set(archives) != set(REPOS):
        raise UnsafeInventory("three archives required")
    version = proposal["versionCandidate"]
    tag = "v" + version
    paths = {p: Path(archives[p]).resolve(strict=True) for p in REPOS}
    local = {}
    for product in REPOS:
        if paths[product].name != f"{product}-{version}.tar.gz":
            raise UnsafeInventory("wrong archive name")
        local[product] = sha_size(paths[product])
    provisional = {
        "schemaVersion": 2, "version": version, "channel": channel, "tag": tag,
        "products": {p: {
            "repository": f"https://github.com/MegaMek/{REPOS[p]}",
            "commit": inventory["commits"][REPOS[p]], "version": version,
            "tag": tag, "releaseId": 1,
            "asset": {"assetId": 1, "name": paths[p].name,
                      "sha256": local[p][0], "size": local[p][1]}}
            for p in REPOS},
        "mmData": {"repository": "https://github.com/MegaMek/mm-data",
                   "commit": inventory["commits"]["mm-data"]},
    }
    validate_record(provisional)
    # This callback must include the three independent external verifiers.
    attest(provisional, paths)
    fresh = release_inventory(getter)
    if any(fresh[key] != inventory[key] for key in ("tags", "releases")):
        raise UnsafeInventory("remote release inventory moved after build; no writes")
    details = release_details(getter)
    if latest(complete_records(details["megamek"], fetch), channel) != previous:
        raise UnsafeInventory(f"latest {channel} record moved after build")
    with tempfile.TemporaryDirectory() as temp:
        directory = Path(temp)
        preflight = directory / f"suite-record-{version}.json"
        preflight.write_text(json.dumps(provisional, indent=2) + "\n", encoding="utf-8")
        validate(preflight)
        # The suite tag must be absent everywhere, including draft releases.
        for repo in REPOS.values():
            if tag in fresh["tags"][repo] or any(
                    r.get("tag_name") == tag for r in details[repo]):
                raise UnsafeInventory("suite version collision")
        # Generated notes create nothing, so a failure here still leaves no trace.
        previous_tag = proposal["previousTag"]
        notes = {p: writes.notes(REPOS[p], tag, inventory["commits"][REPOS[p]], previous_tag)
                 for p in REPOS}
        # No writes until the entire local and remote preflight has passed.
        for product, repo in REPOS.items():
            item = provisional["products"][product]
            require_tag_absent(repo, tag, getter)
            if any(r.get("tag_name") == tag for r in release_details(getter)[repo]):
                raise UnsafeInventory("publication collision; rerun to allocate the next build")
            writes.tag(repo, tag, item["commit"])
            if ref_commit(repo, tag, getter) != item["commit"]:
                raise UnsafeInventory("created tag mismatch")
            item["releaseId"] = writes.release(
                repo, tag, item["commit"],
                **release_presentation(provisional, product, notes[product]))
            item["asset"]["assetId"] = writes.upload(repo, item["releaseId"], paths[product])
            exact_asset(item, release_details(getter)[repo], repo, fetch, directory, getter)
        # Re-verify all three again before record publication.
        current = release_details(getter)
        for p, repo in REPOS.items():
            exact_asset(provisional["products"][p], current[repo], repo, fetch, directory, getter)
        validate_record(provisional)
        record_path = directory / f"suite-record-{version}.json"
        record_path.write_text(json.dumps(provisional, indent=2) + "\n", encoding="utf-8")
        validate(record_path)
        mm_id = provisional["products"]["MegaMek"]["releaseId"]
        if ref_commit("megamek", tag, getter) != inventory["commits"]["megamek"]:
            raise UnsafeInventory("record host ref changed; no record written")
        record_asset_id = writes.upload("megamek", mm_id, record_path)  # LAST remote write.
        hosts = release_details(getter)["megamek"]
        matches = [r for r in hosts if r.get("id") == mm_id and
                   r.get("tag_name") == tag and r.get("draft") is False]
        if len(matches) != 1:
            raise UnsafeInventory("record host changed after upload; manual recovery required")
        assets = matches[0].get("assets")
        digest, size = sha_size(record_path)
        if not isinstance(assets, list) or len([a for a in assets if
                a.get("id") == record_asset_id and a.get("name") == record_path.name
                and a.get("size") == size and a.get("state") == "uploaded"]) != 1:
            raise UnsafeInventory("record upload not confirmed; manual recovery required")
        downloaded = directory / "downloaded-record.json"
        fetch("megamek", record_asset_id, downloaded)
        if sha_size(downloaded) != (digest, size):
            raise UnsafeInventory("record bytes not confirmed; manual recovery required")
    return provisional


def publish_approved(inventory, build_result, sources, *, getter=gh_get,
                     fetch=download_asset, writes=None):
    """Production binding: require a nonpublishing build result and pinned verifiers."""
    proposal = plan(inventory)
    version = proposal["versionCandidate"]
    if (not isinstance(build_result, dict)
            or build_result.get("status") != "OFFLINE_NONPUBLISHING"
            or build_result.get("versionCandidate") != version
            or set(build_result.get("archives", {})) != set(REPOS)
            or build_result.get("productVersions") != {p: version for p in REPOS}):
        raise UnsafeInventory("missing or mismatched successful pinned-build result")
    with tempfile.TemporaryDirectory(prefix="suite-publish-verifiers-") as temporary:
        worktrees = stage(sources, inventory["commits"], Path(temporary), subprocess.run,
                          branch=inventory["branch"])

        def attest(record, paths):
            attest_archives(record, paths, worktrees=worktrees)
            for repo in SOURCES:
                clean(Path(temporary) / repo, inventory["commits"][repo], subprocess.run)

        def validate(record_path):
            directory = worktrees["MegaMek"]
            executable = directory / ("gradlew.bat" if os.name == "nt" else "gradlew")
            run([str(executable), ":megamek:validateSuiteRecord", "--no-daemon",
                 f"-PsuiteRecordFile={record_path}"],
                cwd=directory)
            for repo in SOURCES:
                clean(Path(temporary) / repo, inventory["commits"][repo], subprocess.run)

        return publish(inventory, build_result["archives"], getter=getter,
                       fetch=fetch, writes=writes, attest=attest, validate=validate)
