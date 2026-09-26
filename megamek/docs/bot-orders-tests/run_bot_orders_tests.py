#!/usr/bin/env python3
# Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
#
# This file is part of MegaMek.
#
# MegaMek is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
# License (GPL), version 3 or (at your option) any later version, as published by the Free Software Foundation.
#
# MegaMek is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
# warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
# A copy of the GPL should have been included with this project; if not, see <https://www.gnu.org/licenses/>.
"""
Run bot orders test scenarios headless and render every game.

Each game runs in its own JVM (AIMatchRunner with one repetition): the second game of a batch in one JVM hung at
its start with these pre-deployed scenarios, with or without an orders script. After each game the results CSV and
the orders trace are copied into OUTDIR/<scenario>/ with the game number fixed up, then everything is rendered with
render_bot_orders.py.

Usage (after ./gradlew :megamek:stageDataFiles :megamek:classes in the worktree that is tested):

    py run_bot_orders_tests.py --out OUTDIR --label fix --games 3 --rounds 14 --timeout 25 \
        [--project PATH_TO_megamek_PROJECT] [--classpath-file FILE] SCENARIO.mms [SCENARIO.mms ...]

--project defaults to the megamek project this script sits in; scenario paths are resolved against the current
directory. The classpath is read from `gradlew :megamek:printAiMatchClasspath` unless --classpath-file is given.
"""

import argparse
import glob
import os
import shutil
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_PROJECT = os.path.normpath(os.path.join(HERE, "..", ".."))
JVM_OPTIONS = ["-Xmx4096m", "--add-opens", "java.base/java.util=ALL-UNNAMED", "--add-opens",
               "java.base/java.util.concurrent=ALL-UNNAMED", "-Dsun.awt.disablegrab=true"]


def read_classpath(classpath_file, project):
    if classpath_file:
        with open(classpath_file, encoding="utf-8") as handle:
            return handle.read().strip()
    root = os.path.dirname(project)
    gradlew = os.path.join(root, "gradlew.bat" if os.name == "nt" else "gradlew")
    output = subprocess.run([gradlew, ":megamek:printAiMatchClasspath", "--console=plain", "-q"], cwd=root,
                            capture_output=True, text=True, check=True).stdout
    lines = [line for line in output.splitlines() if line.strip()]
    return lines[-1].strip()


def copy_with_game_number(source, destination, game_number):
    """Copies a trace or results file, replacing the game number AIMatchRunner wrote (always 1 here)."""
    with open(source, encoding="utf-8", errors="replace") as handle:
        lines = handle.read().splitlines()
    separator = "\t" if source.endswith(".tsv") else ","
    fixed = []
    header_seen = False
    for line in lines:
        if line.startswith("# ") or not line:
            fixed.append(line)
            continue
        if not header_seen:
            header_seen = True
            fixed.append(line)
            continue
        fields = line.split(separator)
        fields[0] = str(game_number)
        fixed.append(separator.join(fields))
    with open(destination, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(fixed) + "\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", required=True)
    parser.add_argument("--label", default="")
    parser.add_argument("--games", type=int, default=3)
    parser.add_argument("--rounds", type=int, default=14)
    parser.add_argument("--timeout", type=int, default=25, help="minutes per game")
    parser.add_argument("--project", default=DEFAULT_PROJECT, help="the megamek project folder to run in")
    parser.add_argument("--classpath-file", default="")
    parser.add_argument("scenarios", nargs="+")
    arguments = parser.parse_args()

    project = os.path.abspath(arguments.project)
    classpath = read_classpath(arguments.classpath_file, project)
    logs_dir = os.path.join(project, "logs")
    os.makedirs(arguments.out, exist_ok=True)
    all_traces = []
    for scenario in arguments.scenarios:
        scenario_path = os.path.abspath(scenario)
        scenario_name = os.path.splitext(os.path.basename(scenario_path))[0]
        target_dir = os.path.join(arguments.out, scenario_name)
        os.makedirs(target_dir, exist_ok=True)
        for game_number in range(1, arguments.games + 1):
            before = set(glob.glob(os.path.join(logs_dir, "ai_match_results_*")))
            started = time.time()
            command = ["java"] + JVM_OPTIONS + ["-cp", classpath, "megamek.utilities.AIMatchRunner",
                                                os.path.relpath(scenario_path, project), "1",
                                                str(arguments.rounds), str(arguments.timeout)]
            print("running %s game %d/%d (%d rounds)" % (scenario_name, game_number, arguments.games,
                                                          arguments.rounds), flush=True)
            console_path = os.path.join(target_dir, "%s_game%d_console.log" % (scenario_name, game_number))
            with open(console_path, "w", encoding="utf-8") as console:
                subprocess.run(command, cwd=project, stdout=console, stderr=subprocess.STDOUT, check=False)
            new_files = sorted(set(glob.glob(os.path.join(logs_dir, "ai_match_results_*"))) - before)
            for new_file in new_files:
                base_name = os.path.basename(new_file).replace("_game1", "")
                if base_name.endswith("_orders.tsv"):
                    destination = os.path.join(target_dir, "%s_game%d_orders.tsv" % (scenario_name, game_number))
                    all_traces.append(destination)
                else:
                    destination = os.path.join(target_dir, "%s_game%d_results.csv" % (scenario_name, game_number))
                copy_with_game_number(new_file, destination, game_number)
            print("  finished in %d s, %d files" % (time.time() - started, len(new_files)), flush=True)
    if not all_traces:
        print("no traces produced", file=sys.stderr)
        sys.exit(1)
    renderer = os.path.join(HERE, "render_bot_orders.py")
    subprocess.run([sys.executable, renderer, "--out", arguments.out, "--label", arguments.label] + all_traces,
                   check=True)


if __name__ == "__main__":
    main()
