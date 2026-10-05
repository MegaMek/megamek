#!/usr/bin/env python3
"""Guarded, single-attempt write phase. Not a workflow entry point.

The caller must supply an approved, attested inventory and the three retained
archives from the pinned nonpublishing build. A failure may leave product artifacts,
but never a partial suite record; recovery requires a new human review.
"""

import hashlib
import json
import os
import subprocess
import tempfile
from pathlib import Path

from suite_release_plan import (
    REPOS, SOURCES, UnsafeInventory, canonical, check_product_assets, complete_record,
    download_asset, failure_detail,
    gh_get, plan, release_details, release_inventory, validate_previous, valid_sha,
)

from suite_archive_attestation import attest_archives
from suite_pinned_build import clean, run, stage


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


def api(command, *, input_file=None, runner=subprocess.run):
    """Only fixed GitHub API write shapes; never retry a possibly successful POST."""
    try:
        args = ["gh", "api", *command]
        if input_file is not None:
            args += ["--input", str(input_file)]
        result = runner(args, capture_output=True, check=True)
        from suite_release_plan import parse_json
        return parse_json(result.stdout)
    except (OSError, subprocess.CalledProcessError) as error:
        raise UnsafeInventory(f"write failed (do not retry): {failure_detail(error)}") from None


class GitHubWrites:
    def tag(self, repo, tag, sha):
        result = api(["--method", "POST", f"repos/MegaMek/{repo}/git/refs",
                      "-f", f"ref=refs/tags/{tag}", "-f", f"sha={sha}"])
        if result.get("ref") != f"refs/tags/{tag}" or result.get("object", {}).get("sha") != sha:
            raise UnsafeInventory("tag creation response mismatch")

    def release(self, repo, tag, sha, *, name, body):
        result = api(["--method", "POST", f"repos/MegaMek/{repo}/releases",
                      "-f", f"tag_name={tag}", "-f", f"target_commitish={sha}",
                      "-F", "draft=false", "-F", "prerelease=false",
                      "-f", f"name={name}", "-f", f"body={body}"])
        if (result.get("tag_name") != tag or result.get("draft") is not False
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


def release_presentation(record, product=None):
    channel = record["membership"].capitalize()
    version = record["version"]
    suite_name = f"{channel} suite {version}"
    name = suite_name if product in (None, "MegaMek") else (
        f"{product} {record['products'][product]['version']} - {suite_name}")
    record_url = (f"https://github.com/MegaMek/megamek/releases/download/"
                  f"{record['tag']}/suite-record-{version}.json")
    lines = [
        f"**Channel: {channel}**",
        "",
        f"**Suite: {version}**",
        "",
        f"The [complete suite record]({record_url}) is the authority for channel membership "
        "and exact product downloads.",
        "This suite is complete only when that record is available. It is uploaded last, "
        "after all product archives have been verified.",
        "",
        "## Included products",
        "",
        "| Product | Version | Download | Frozen source |",
        "| --- | --- | --- | --- |",
    ]
    for included, item in record["products"].items():
        url = (f"{item['repository']}/releases/download/{item['tag']}/"
               f"{item['asset']['name']}")
        source = f"{item['repository']}/commit/{item['commit']}"
        lines.append(f"| {included} | {item['version']} | [Download]({url}) | "
                     f"[`{item['commit'][:12]}`]({source}) |")
    data = record["mmData"]
    lines += [
        "",
        f"Frozen mm-data: [`{data['commit'][:12]}`]({data['repository']}/commit/{data['commit']}).",
        "",
        "Product archives may be reused by later suites, including other channels. "
        "Use each suite's complete record, not an archive's original release title, "
        "to determine membership.",
    ]
    return {"name": name, "body": "\n".join(lines) + "\n"}


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
            raise UnsafeInventory("publication collision; manual recovery required")
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
            writes=None, attest=None, validate=None, bootstrap=False):
    """Publish after successful pinned build, with no automatic retry or rollback.

    `validate` must run :megamek:validateSuiteRecord from the pinned MegaMek
    checkout. The caller is responsible for establishing the approved pinned
    archive attestation and for providing a scoped App token only to writes.
    """
    if writes is None or attest is None or validate is None:
        raise UnsafeInventory("missing write, archive attestation or record validator")
    proposal = plan(inventory)
    previous = inventory["previous"]
    changed = proposal["buildCandidates"]
    if bootstrap != (previous is None) or (bootstrap and set(changed) != set(REPOS)):
        raise UnsafeInventory("invalid bootstrap or prior record")
    if set(archives) != set(REPOS):
        raise UnsafeInventory("three retained archives required")
    version = proposal["versionCandidate"]
    if not changed and proposal["weeklyNoopCandidate"]:
        raise UnsafeInventory("weekly no-op: no publication; verify remotely")
    paths = {p: Path(archives[p]).resolve(strict=True) for p in REPOS}
    versions = {p: version if p in changed else previous["products"][p]["version"]
                for p in REPOS}
    local = {}
    for product in REPOS:
        if paths[product].name != f"{product}-{versions[product]}.tar.gz":
            raise UnsafeInventory("wrong retained archive name")
        digest, size = sha_size(paths[product])
        if product not in changed and (digest, size) != (
                previous["products"][product]["asset"]["sha256"],
                previous["products"][product]["asset"]["size"]):
            raise UnsafeInventory("reused archive differs")
        local[product] = (digest, size)
    # This callback must include the three independent external verifiers.
    provisional = {
        "schemaVersion": 1, "version": version, "tag": "v" + version,
        "membership": inventory["membership"],
        "mmData": {"repository": "https://github.com/MegaMek/mm-data",
                   "commit": inventory["commits"]["mm-data"]},
        "products": {p: (previous["products"][p] if p not in changed else {
            "repository": f"https://github.com/MegaMek/{REPOS[p]}",
            "commit": inventory["commits"][REPOS[p]], "version": version,
            "tag": "v" + version, "releaseId": 1,
            "asset": {"assetId": 1, "name": paths[p].name,
                      "sha256": local[p][0], "size": local[p][1]}})
            for p in REPOS},
    }
    validate_previous(provisional)
    attest(provisional, paths)
    fresh = release_inventory(getter)
    if any(fresh[key] != inventory[key] for key in ("tags", "releases")):
        raise UnsafeInventory("remote release inventory moved after build; no writes")
    details = release_details(getter)
    remote_previous = complete_record(details["megamek"], fetch, allow_missing=bootstrap)
    if remote_previous != previous:
        raise UnsafeInventory("latest complete record moved after build")
    with tempfile.TemporaryDirectory() as temp:
        directory = Path(temp)
        preflight = directory / f"suite-record-{version}.json"
        preflight.write_text(json.dumps(provisional, indent=2) + "\n", encoding="utf-8")
        validate(preflight)
        if previous is not None:
            check_product_assets(previous, details, fetch)
            for p, repo in REPOS.items():
                exact_asset(previous["products"][p], details[repo], repo, fetch, directory, getter)
        # A new suite tag must be absent everywhere, including draft releases.
        for repo in REPOS.values():
            if "v" + version in fresh["tags"][repo] or any(
                    r.get("tag_name") == "v" + version for r in details[repo]):
                raise UnsafeInventory("suite version collision")
        # No writes until the entire local and remote preflight has passed.
        for product in changed:
            repo = REPOS[product]
            item = provisional["products"][product]
            require_tag_absent(repo, item["tag"], getter)
            if any(r.get("tag_name") == item["tag"] for r in release_details(getter)[repo]):
                raise UnsafeInventory("publication collision; manual recovery required")
            writes.tag(repo, item["tag"], item["commit"])
            if ref_commit(repo, item["tag"], getter) != item["commit"]:
                raise UnsafeInventory("created tag mismatch")
            item["releaseId"] = writes.release(
                repo, item["tag"], item["commit"], **release_presentation(provisional, product))
            item["asset"]["assetId"] = writes.upload(repo, item["releaseId"], paths[product])
            current = release_details(getter)
            exact_asset(item, current[repo], repo, fetch, directory, getter)
        # Re-verify all three again before record publication.
        current = release_details(getter)
        for p, repo in REPOS.items():
            exact_asset(provisional["products"][p], current[repo], repo, fetch, directory, getter)
        validate_previous(provisional)
        record_path = directory / f"suite-record-{version}.json"
        record_path.write_text(json.dumps(provisional, indent=2) + "\n", encoding="utf-8")
        validate(record_path)
        mm_id = provisional["products"]["MegaMek"]["releaseId"]
        # A reused MegaMek product keeps its old release ID. A separate suite
        # release at this version hosts only the complete record.
        if "MegaMek" not in changed:
            require_tag_absent("megamek", "v" + version, getter)
            if any(r.get("tag_name") == "v" + version for r in
                   release_details(getter)["megamek"]):
                raise UnsafeInventory("suite host collision; manual recovery required")
            writes.tag("megamek", "v" + version, inventory["commits"]["megamek"])
            if ref_commit("megamek", "v" + version, getter) != inventory["commits"]["megamek"]:
                raise UnsafeInventory("suite host tag mismatch")
            mm_id = writes.release(
                "megamek", "v" + version, inventory["commits"]["megamek"],
                **release_presentation(provisional))
            hosts = release_details(getter)["megamek"]
            if len([r for r in hosts if r.get("id") == mm_id and
                    r.get("tag_name") == "v" + version and r.get("draft") is False]) != 1:
                raise UnsafeInventory("suite host release mismatch")
        if ref_commit("megamek", "v" + version, getter) != inventory["commits"]["megamek"]:
            raise UnsafeInventory("record host ref changed; no record written")
        record_asset_id = writes.upload("megamek", mm_id, record_path)  # LAST remote write.
        hosts = release_details(getter)["megamek"]
        matches = [r for r in hosts if r.get("id") == mm_id and
                   r.get("tag_name") == "v" + version and r.get("draft") is False]
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


def publish_approved(inventory, build_result, sources, *, bootstrap=False,
                     getter=gh_get, fetch=download_asset, writes=None):
    """Production binding: require a nonpublishing build result and pinned verifiers.

    Approval of `inventory` and of the build inputs is external to this
    function. The workflow must not call this on an unapproved plan.
    """
    proposal = plan(inventory)
    if (not isinstance(build_result, dict)
            or build_result.get("status") != "OFFLINE_NONPUBLISHING"
            or build_result.get("versionCandidate") != proposal["versionCandidate"]
            or set(build_result.get("archives", {})) != set(REPOS)
            or build_result.get("productVersions") != {
                p: (proposal["versionCandidate"] if p in proposal["buildCandidates"]
                    else inventory["previous"]["products"][p]["version"])
                for p in REPOS}):
        raise UnsafeInventory("missing or mismatched successful pinned-build result")
    with tempfile.TemporaryDirectory(prefix="suite-publish-verifiers-") as temporary:
        worktrees = stage(sources, inventory["commits"], Path(temporary), subprocess.run)

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
                       fetch=fetch, writes=writes, attest=attest,
                       validate=validate, bootstrap=bootstrap)
