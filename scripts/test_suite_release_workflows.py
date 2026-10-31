"""Safety invariants of the game suite release workflows, checked on their text (no YAML parser)."""

import re
import unittest
from pathlib import Path

WORKFLOWS = Path(__file__).resolve().parents[1] / ".github" / "workflows"


def read(name):
    return (WORKFLOWS / name).read_text(encoding="utf-8")


def steps(workflow):
    """Map each step's ``id`` (or ``name``) to its text."""
    found = {}
    for block in re.split(r"\n      - ", workflow)[1:]:
        key = re.search(r"^\s*id: (\S+)", block, re.M) or re.search(r"^name: (.+)", block)
        if key:
            found[key.group(1).strip()] = block
    return found


def run_scripts(workflow):
    """Bodies of every ``run:`` script."""
    scripts = []
    lines = workflow.splitlines()
    for index, line in enumerate(lines):
        match = re.match(r"^(\s*)run: ?(.*)$", line)
        if not match:
            continue
        if match.group(2) not in ("|", ""):
            scripts.append(match.group(2))
            continue
        indent = len(match.group(1))
        body = []
        for following in lines[index + 1:]:
            if following.strip() and len(following) - len(following.lstrip()) <= indent:
                break
            body.append(following)
        scripts.append("\n".join(body))
    return scripts


class EngineTests(unittest.TestCase):
    def setUp(self):
        self.workflow = read("game-suite-release-engine.yml")
        self.steps = steps(self.workflow)

    def position(self, text):
        self.assertIn(text, self.workflow)
        return self.workflow.index(text)

    def test_is_reusable_only_and_read_only_by_default(self):
        self.assertIn("on:\n  workflow_call:", self.workflow)
        for trigger in ("workflow_dispatch:", "schedule:", "pull_request:", "push:"):
            self.assertNotIn(trigger, self.workflow)
        # Callers own the global group; a nested group would deadlock.
        self.assertNotIn("concurrency:", self.workflow)
        self.assertIn("permissions:\n  contents: read\n", self.workflow)
        self.assertIn("persist-credentials: false", self.workflow)

    def test_live_gate_runs_before_checkout(self):
        gate = self.steps["Gate live operation before checkout"]
        self.assertIn('"$DRY_RUN" != "true" && "$ENABLED" != "true"', gate)
        self.assertIn("ENABLED: ${{ vars.SUITE_RELEASES_ENABLED }}", gate)
        self.assertIn("APP_KEY_PRESENT: ${{ secrets.SUITE_RELEASE_APP_PRIVATE_KEY != '' }}", gate)
        self.assertLess(self.position("Gate live operation"), self.position("actions/checkout"))

    def test_build_never_holds_a_write_token(self):
        prepare = self.steps["prepare"]
        self.assertIn("GH_TOKEN: ${{ github.token }}", prepare)
        self.assertNotIn("--publish", prepare)
        self.assertEqual(3, self.workflow.count("TMPDIR: ${{ runner.temp }}"))

    def test_publish_token_is_minted_after_the_build_for_three_repositories(self):
        app = self.steps["app"]
        self.assertLess(self.position("id: prepare"), self.position("id: app"))
        self.assertIn("repositories: megamek,megameklab,mekhq\n", app)
        self.assertIn("if: ${{ !inputs.dry_run && steps.prepare.outputs.noop == 'false' }}", app)
        self.assertEqual(1, self.workflow.count("--publish"))
        publish = self.steps["Recheck remote inventory and publish once, record last"]
        self.assertIn("--publish", publish)
        self.assertIn("GH_TOKEN: ${{ steps.app.outputs.token }}", publish)
        self.assertIn(app.split("if: ")[1].splitlines()[0], publish)
        finish = self.steps["Finish dry run or no-op without write token"]
        self.assertIn("GH_TOKEN: ${{ github.token }}", finish)
        self.assertNotIn("--publish", finish)

    def test_cut_tokens_are_scoped_to_their_step(self):
        cut_app = self.steps["cut_app"]
        self.assertIn("repositories: megamek,megameklab,mekhq,mm-data\n", cut_app)
        self.assertIn("inputs.cut_line != '' && !inputs.dry_run", cut_app)
        self.assertLess(self.position("id: cut_app"), self.position("id: prepare"))
        bump_app = self.steps["bump_app"]
        self.assertIn("repositories: megamek\n", bump_app)
        self.assertIn("inputs.cut_line != '' && !inputs.dry_run", bump_app)
        self.assertLess(self.position("--publish"), self.position("id: bump_app"))
        for step, token in (("Create the Core branches from main", "cut_app"),
                            ("Move main to the next line", "bump_app")):
            self.assertIn(f"inputs.dry_run && github.token || steps.{token}.outputs.token",
                          self.steps[step])
            self.assertIn("--dry-run", self.steps[step])
        self.assertEqual(3, self.workflow.count("actions/create-github-app-token@v3"))
        self.assertEqual(3, self.workflow.count("permission-contents: write"))

    def test_cut_dry_run_rehearses_main(self):
        prepare = self.steps["prepare"]
        self.assertIn('if [[ -n "$CUT_LINE" && "$DRY_RUN" == "true" ]]', prepare)
        self.assertIn("args+=(--rehearsal)", prepare)
        self.assertIn('args+=(--branch "releases/$CUT_LINE")', prepare)


class CallerTests(unittest.TestCase):
    CALLERS = ("game-suite-release-experimental.yml", "game-suite-release-core.yml",
               "game-suite-cut-core.yml")

    def test_callers_share_the_global_group_and_use_the_engine(self):
        for name in self.CALLERS:
            with self.subTest(workflow=name):
                workflow = read(name)
                self.assertIn("concurrency:\n  group: game-suite-release-global\n"
                              "  cancel-in-progress: false", workflow)
                self.assertIn("uses: ./.github/workflows/game-suite-release-engine.yml", workflow)
                self.assertIn("secrets: inherit", workflow)
                self.assertIn("permissions:\n  contents: read\n", workflow)
                self.assertIn("workflow_dispatch:", workflow)
                self.assertIn("'suite-release-dry-run' || 'suite-release-live'", workflow)

    def test_only_experimental_is_scheduled_and_only_it_reports_failures(self):
        experimental = read("game-suite-release-experimental.yml")
        self.assertIn('schedule:\n    - cron: "0 5 * * 2"', experimental)
        self.assertIn("channel: experimental", experimental)
        for required in (
                "github.event_name != 'schedule' || (github.repository == 'MegaMek/megamek' "
                "&& vars.SUITE_RELEASES_ENABLED == 'true')",
                "dry_run: ${{ github.event_name != 'schedule' && inputs.dry_run }}",
                "github.event_name == 'schedule' && 'suite-release-weekly' || "
                "(inputs.dry_run && 'suite-release-dry-run' || 'suite-release-live')"):
            self.assertIn(required, experimental)
        report = experimental[experimental.index("report-scheduled-failure:"):]
        self.assertIn("failure() && github.event_name == 'schedule'", report)
        self.assertIn("issues: write", report)
        self.assertNotIn("issues: write",
                         experimental[:experimental.index("report-scheduled-failure:")])
        for name in ("game-suite-release-core.yml", "game-suite-cut-core.yml"):
            with self.subTest(workflow=name):
                workflow = read(name)
                self.assertNotIn("schedule:", workflow)
                self.assertNotIn("suite-release-weekly", workflow)
                self.assertIn("channel: core", workflow)

    def test_cut_core_defaults_to_a_dry_run(self):
        cut = read("game-suite-cut-core.yml")
        self.assertIn("cut_line: ${{ inputs.line }}", cut)
        dry_run = cut[cut.index("      dry_run:"):]
        self.assertIn("default: true", dry_run.split("\n\n")[0])


class InjectionTests(unittest.TestCase):
    def test_run_scripts_take_inputs_only_through_the_environment(self):
        for path in sorted(WORKFLOWS.glob("game-suite-*.yml")):
            with self.subTest(workflow=path.name):
                for script in run_scripts(path.read_text(encoding="utf-8")):
                    self.assertNotIn("${{", script)


class AppCheckTests(unittest.TestCase):
    def test_app_check_is_manual_protected_and_read_only(self):
        workflow = read("game-suite-app-check.yml")
        for required in ("workflow_dispatch:", "contents: read", "suite-release-live",
                         "suite-release-weekly", "environment: ${{ inputs.environment }}",
                         "group: game-suite-release-global", "cancel-in-progress: false",
                         "timeout-minutes: 5", "actions/create-github-app-token@v3",
                         "repositories: megamek,megameklab,mekhq,mm-data",
                         "GH_TOKEN: ${{ steps.app.outputs.token }}",
                         "MegaMek/megamek,MegaMek/megameklab,MegaMek/mekhq,MegaMek/mm-data"):
            self.assertIn(required, workflow)
        for forbidden in ("schedule:", "pull_request:", "push:", "actions/checkout",
                          "SUITE_RELEASES_ENABLED", "--publish", "suite_release_coordinator",
                          "suite_release_cut", "gh release", "git push"):
            self.assertNotIn(forbidden, workflow)
        requests = [line.strip() for line in workflow.splitlines() if "gh api " in line]
        self.assertEqual(len(requests), 2)
        self.assertTrue(all("gh api --method GET " in line for line in requests))


if __name__ == "__main__":
    unittest.main()
