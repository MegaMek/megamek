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
Render bot orders traces (the *_orders.tsv files written by AIMatchRunner when a scenario has a .orders script)
into one self-contained HTML page per game: the board, what each bot unit was ordered to do, and what it did.

Standard library only. Usage:

    py render_bot_orders.py --out OUTDIR [--label NAME] [--boards DIR ...] TRACE.tsv [TRACE.tsv ...]
    py render_bot_orders.py --master OUTDIR SETDIR [SETDIR ...]

The first form writes game pages plus index.html and summary.json into OUTDIR. The second writes a top-level
index.html in OUTDIR linking several rendered sets (for example "fix" and "baseline").
"""

import argparse
import html
import json
import math
import os
import re
import sys
from collections import OrderedDict, defaultdict

SQRT3 = math.sqrt(3.0)
FACING_NAMES = ["N", "NE", "SE", "S", "SW", "NW"]
FACING_VECTORS = [(0.0, -1.0), (0.866, -0.5), (0.866, 0.5), (0.0, 1.0), (-0.866, 0.5), (-0.866, -0.5)]
DEFAULT_BOARD_DIRS = [
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "data", "boards"),
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "..", "mm-data", "data", "boards"),
    r"D:\MegaMek Projects\mekhq\mm-data\data\boards",
]
UNIT_COLOURS = ["#e6194b", "#3cb44b", "#4363d8", "#f58231", "#911eb4", "#42d4f4", "#f032e6", "#9a6324",
                "#469990", "#800000", "#808000", "#000075", "#e6beff", "#aaffc3", "#ffd8b1", "#a9a9a9"]


# ----------------------------------------------------------------------------------------------------------------
# Board
# ----------------------------------------------------------------------------------------------------------------

class Board:
    """A parsed .board file. Hexes are filled in file order (the loader ignores the hex numbers)."""

    def __init__(self, path):
        self.path = path
        with open(path, encoding="utf-8", errors="replace") as board_file:
            text = board_file.read()
        size = re.search(r"^size (\d+) (\d+)", text, re.M)
        if not size:
            raise ValueError("no size line in " + path)
        self.width = int(size.group(1))
        self.height = int(size.group(2))
        self.hexes = {}
        index = 0
        for match in re.finditer(r'^hex \S+ (-?\d+) "([^"]*)"', text, re.M):
            column = index % self.width
            row = index // self.width
            index += 1
            if row >= self.height:
                break
            self.hexes[(column, row)] = (int(match.group(1)), parse_terrain(match.group(2)))

    def terrain(self, column, row):
        return self.hexes.get((column, row), (0, {}))


def parse_terrain(terrain_text):
    terrain = {}
    for part in terrain_text.split(";"):
        if not part:
            continue
        pieces = part.split(":")
        name = pieces[0]
        level = pieces[1] if len(pieces) > 1 else "1"
        try:
            terrain[name] = int(level)
        except ValueError:
            terrain[name] = 1
    return terrain


def hex_class(elevation, terrain):
    """CSS class for a hex's base fill."""
    if "water" in terrain:
        depth = terrain["water"]
        if depth >= 2:
            return "t-deep"
        if depth == 1:
            return "t-shallow"
        return "t-puddle"
    if "building" in terrain or "fuel_tank" in terrain:
        return "t-building"
    if "magma" in terrain:
        return "t-magma"
    if "woods" in terrain or "jungle" in terrain:
        return "t-heavywoods" if max(terrain.get("woods", 0), terrain.get("jungle", 0)) >= 2 else "t-woods"
    if "rough" in terrain or "rubble" in terrain:
        return "t-rough"
    if "swamp" in terrain:
        return "t-swamp"
    if "ice" in terrain or "snow" in terrain:
        return "t-snow"
    if "pavement" in terrain:
        return "t-road"
    return "t-clear"


def find_board(scenario_path, board_name, extra_dirs):
    candidates = []
    if scenario_path:
        candidates.append(os.path.join(os.path.dirname(scenario_path), board_name))
        scenario_root = os.path.dirname(os.path.dirname(os.path.dirname(scenario_path)))
        candidates.append(os.path.join(scenario_root, "data", "boards", board_name))
    for directory in list(extra_dirs) + DEFAULT_BOARD_DIRS:
        candidates.append(os.path.join(directory, board_name))
    for candidate in candidates:
        if os.path.isfile(candidate):
            return os.path.normpath(candidate)
    raise FileNotFoundError("board not found: " + board_name + " (looked in " + ", ".join(candidates) + ")")


def board_name_from_scenario(scenario_path):
    """The first board of a scenario: 'map: file' or 'map:' followed by 'file: ...' or a 'boards:' list."""
    with open(scenario_path, encoding="utf-8", errors="replace") as scenario_file:
        lines = scenario_file.read().splitlines()
    for index, line in enumerate(lines):
        match = re.match(r"^(map|maps):\s*(.*)$", line)
        if not match:
            continue
        value = match.group(2).split("#")[0].strip()
        if value:
            return value.strip("'\"")
        for following in lines[index + 1:index + 12]:
            file_match = re.match(r"^\s+-?\s*(file:)?\s*(.+\.board)\s*$", following)
            if file_match:
                return file_match.group(2).strip().strip("'\"")
    raise ValueError("no map in " + scenario_path)


# ----------------------------------------------------------------------------------------------------------------
# Hex geometry: flat-top hexes, MegaMek offset columns (0-based odd columns sit half a hex lower)
# ----------------------------------------------------------------------------------------------------------------

def centre(column, row, size):
    """Pixel centre of a 0-based hex."""
    x = size + column * 1.5 * size
    y = SQRT3 * size * (row + 0.5 * (column & 1)) + SQRT3 * size / 2.0
    return x, y


def hex_points(column, row, size):
    centre_x, centre_y = centre(column, row, size)
    points = []
    for corner in range(6):
        angle = math.radians(60 * corner)
        points.append("%.1f,%.1f" % (centre_x + size * math.cos(angle), centre_y + size * math.sin(angle)))
    return " ".join(points)


def to_cube(column, row):
    cube_x = column
    cube_z = row - (column - (column & 1)) // 2
    return cube_x, -cube_x - cube_z, cube_z


def hex_distance(first, second):
    ax, ay, az = to_cube(*first)
    bx, by, bz = to_cube(*second)
    return max(abs(ax - bx), abs(ay - by), abs(az - bz))


def hex_number(column, row):
    """MegaMek hex number from 0-based coordinates."""
    if column >= 99 or row >= 99:
        return "%03d%03d" % (column + 1, row + 1)
    return "%02d%02d" % (column + 1, row + 1)


def parse_hex_number(text):
    text = text.strip()
    if not re.match(r"^(\d{4}|\d{6})$", text):
        return None
    half = len(text) // 2
    return int(text[:half]) - 1, int(text[half:]) - 1


def distance_to_edge(position, edge, width, height):
    column, row = position
    if edge == "NORTH":
        return row
    if edge == "SOUTH":
        return height - 1 - row
    if edge == "WEST":
        return column
    if edge == "EAST":
        return width - 1 - column
    return None


# ----------------------------------------------------------------------------------------------------------------
# Trace
# ----------------------------------------------------------------------------------------------------------------

class UnitTrack:
    def __init__(self, unit_id):
        self.unit_id = unit_id
        self.name = ""
        self.owner = ""
        self.starts = OrderedDict()   # round -> row dict
        self.ends = OrderedDict()     # round -> row dict
        self.paths = defaultdict(list)  # round -> [(column, row, facing)]
        self.orders = []              # row dicts
        self.gone = None              # row dict
        self.colour = None

    @property
    def ordered(self):
        return bool(self.orders)


class Trace:
    def __init__(self, path):
        self.path = path
        self.header = OrderedDict()
        self.rows = []
        with open(path, encoding="utf-8", errors="replace") as trace_file:
            column_names = None
            for line in trace_file:
                line = line.rstrip("\r\n")
                if not line:
                    continue
                if line.startswith("# "):
                    key, _, value = line[2:].partition("\t")
                    self.header[key] = value
                    continue
                fields = line.split("\t")
                if column_names is None:
                    column_names = fields
                    continue
                fields += [""] * (len(column_names) - len(fields))
                self.rows.append(dict(zip(column_names, fields)))
        self.units = OrderedDict()
        self.bot_orders = []  # flee orders and other bot-wide rows
        self.game = self.rows[0]["game"] if self.rows else "?"
        for row in self.rows:
            stage = row["stage"]
            unit_id = row["unitId"]
            if stage == "order" and not unit_id:
                self.bot_orders.append(row)
                continue
            if not unit_id:
                continue
            track = self.units.setdefault(unit_id, UnitTrack(unit_id))
            if row.get("name"):
                track.name = row["name"]
            if row.get("owner"):
                track.owner = row["owner"]
            round_number = int(row["round"])
            if stage == "start":
                track.starts.setdefault(round_number, row)
            elif stage == "end":
                track.ends[round_number] = row
            elif stage == "path":
                steps = []
                for step in row["detail"].split(";"):
                    parts = step.split(",")
                    if len(parts) >= 3:
                        steps.append((int(parts[0]) - 1, int(parts[1]) - 1, int(parts[2])))
                track.paths[round_number].extend(steps)
            elif stage == "order":
                track.orders.append(row)
            elif stage == "gone":
                track.gone = row
        # bot-wide flee orders count as orders for every unit of that bot
        for bot_row in self.bot_orders:
            for track in self.units.values():
                if track.owner == bot_row["owner"]:
                    track.orders.append(bot_row)
        for track in self.units.values():
            track.orders.sort(key=lambda order_row: int(order_row["round"]))
        rounds = set()
        for row in self.rows:
            if row["stage"] in ("start", "end"):
                rounds.add(int(row["round"]))
        self.rounds = sorted(rounds)


def position_of(row):
    if not row or not row.get("col") or not row.get("row"):
        return None
    return int(row["col"]) - 1, int(row["row"]) - 1


# ----------------------------------------------------------------------------------------------------------------
# Order analysis: did the unit follow what it was told this round?
# ----------------------------------------------------------------------------------------------------------------

def analyse_unit(track, width, height):
    """Walk the unit's rounds, tracking its standing orders, and judge each end-of-movement row."""
    route = []          # current waypoint list (0-based coords)
    route_history = []  # (round, [coords]) each time the route was set/extended
    flee_edge = ""
    order_index = 0
    orders = track.orders
    verdicts = OrderedDict()
    rule_counts = defaultdict(int)
    rounds = sorted(set(list(track.starts.keys()) + list(track.ends.keys())))
    last_waypoint_ordered = None
    for round_number in rounds:
        while order_index < len(orders) and int(orders[order_index]["round"]) <= round_number:
            order = orders[order_index]
            order_index += 1
            action = order["orderAction"]
            arguments = order["orderArgs"].split()
            if action in ("WAYPOINTS", "ADD_WAYPOINTS"):
                hexes = [parse_hex_number(argument) for argument in arguments]
                hexes = [coords for coords in hexes if coords is not None]
                route = hexes if action == "WAYPOINTS" else route + hexes
                route_history.append((int(order["round"]), list(route), position_of(order)))
                if route:
                    last_waypoint_ordered = route[-1]
            elif action == "CLEAR":
                route = []
            elif action == "FLEE":
                flee_edge = "" if (arguments and arguments[0].upper() == "NONE") else (arguments[0].upper()
                                                                                        if arguments else "")
        end_row = track.ends.get(round_number)
        start_row = track.starts.get(round_number)
        if not end_row:
            continue
        rule = end_row.get("rule", "")
        detail = end_row.get("detail", "")
        behaviour = end_row.get("behaviour", "")
        head = parse_hex_number(end_row.get("headWaypoint", "")) if end_row.get("headWaypoint") else None
        start_head = (parse_hex_number(start_row.get("headWaypoint", ""))
                      if start_row and start_row.get("headWaypoint") else None)
        home_edge = (start_row.get("homeEdge") if start_row else "") or end_row.get("homeEdge", "")
        effective_flee = end_row.get("fleeEdge", "") or flee_edge
        withdrawing = end_row.get("withdrawing") == "true"
        rule_label = rule if rule else derived_rule(behaviour, effective_flee, start_head or head, withdrawing)
        rule_counts[rule_label] += 1

        expected = ""
        followed = None
        reason = ""
        if effective_flee:
            expected = "FLEE " + effective_flee
            heading = edge_in_detail(detail) or ((home_edge or "") if behaviour in ("ForcedWithdrawal",
                                                                                    "MoveToDestination") else "")
            followed = (heading == effective_flee) and behaviour in ("ForcedWithdrawal", "MoveToDestination")
            if not followed:
                reason = describe(rule_label, detail, behaviour, home_edge)
        elif (start_head or head) and route:
            target = start_head or head
            expected = "WAYPOINT " + hex_number(*target)
            if rule:
                followed = rule == "PLAYER_WAYPOINT"
            else:
                followed = behaviour == "MoveToDestination" and not withdrawing
            if not followed:
                reason = describe(rule_label, detail, behaviour, home_edge)
        verdicts[round_number] = {
            "expected": expected, "followed": followed, "rule": rule_label, "detail": detail,
            "behaviour": behaviour, "reason": reason, "homeEdge": home_edge, "withdrawing": withdrawing,
            "crippled": end_row.get("crippled") == "true", "head": head, "position": position_of(end_row),
            "facing": int(end_row["facing"]) if end_row.get("facing") else None,
        }
    final_row = track.ends[next(reversed(track.ends))] if track.ends else None
    final_position = position_of(final_row)
    summary = {
        "route_history": route_history,
        "flee_edge": flee_edge,
        "final_position": final_position,
        "final_waypoint": last_waypoint_ordered,
        "final_distance": (hex_distance(final_position, last_waypoint_ordered)
                           if final_position and last_waypoint_ordered else None),
        "flee_distance": (distance_to_edge(final_position, flee_edge, width, height)
                          if final_position and flee_edge else None),
        "rule_counts": dict(rule_counts),
        "followed_rounds": sum(1 for verdict in verdicts.values() if verdict["followed"] is True),
        "overridden_rounds": sum(1 for verdict in verdicts.values() if verdict["followed"] is False),
        "ordered_rounds": sum(1 for verdict in verdicts.values() if verdict["expected"]),
    }
    return verdicts, summary


def derived_rule(behaviour, flee_edge, head, withdrawing):
    """The rule a build without [BotOrders] log lines followed, worked out from the bot's cached behaviour."""
    if behaviour == "ForcedWithdrawal":
        return "FORCED_WITHDRAWAL*"
    if behaviour == "MoveToDestination":
        if flee_edge:
            return "FLEE_ORDER*"
        if head:
            return "PLAYER_WAYPOINT*"
        return "MOVE_TO_DESTINATION*"
    if behaviour == "Engaged":
        return "ENGAGED*"
    if behaviour == "MoveToContact":
        return "MOVE_TO_CONTACT*"
    if behaviour == "NoPathToDestination":
        return "NO_PATH*"
    return (behaviour or "UNKNOWN") + "*"


def edge_in_detail(detail):
    match = re.search(r"toward the (NORTH|SOUTH|EAST|WEST) edge", detail or "")
    return match.group(1) if match else ""


def describe(rule_label, detail, behaviour, home_edge):
    if detail:
        return rule_label + " - " + detail
    text = rule_label + " (behaviour " + (behaviour or "?") + ")"
    if behaviour in ("ForcedWithdrawal", "MoveToDestination") and home_edge:
        text += ", heading for the " + home_edge + " edge"
    return text


# ----------------------------------------------------------------------------------------------------------------
# HTML
# ----------------------------------------------------------------------------------------------------------------

STYLE = """
:root {
  --bg: #f7f7f4; --fg: #1d1f21; --muted: #5d6166; --panel: #ffffff; --line: #d0d3d6; --accent: #2458c6;
  --bad: #c62828; --good: #2e7d32; --warn: #b26a00;
  --t-clear: #eceedf; --t-woods: #a9d18e; --t-heavywoods: #6da457; --t-shallow: #a9d2f0; --t-deep: #5a95d0;
  --t-puddle: #c8e0f2; --t-building: #b39c80; --t-rough: #cdbd9c; --t-swamp: #a9b98f; --t-snow: #f6f8fa;
  --t-magma: #e08a5a; --t-road: #d6ccb8; --road: #8a7a60; --elev: #3a3020; --grid: rgba(0,0,0,0.12);
  --hexlabel: rgba(0,0,0,0.35); --enemy: #6d6d6d;
}
:root[data-theme="dark"] {
  --bg: #16181b; --fg: #e4e6e8; --muted: #a0a6ad; --panel: #1f2226; --line: #3a3f45; --accent: #7aa7ff;
  --bad: #ff6b6b; --good: #69c46d; --warn: #ffb74d;
  --t-clear: #2c302a; --t-woods: #36582d; --t-heavywoods: #1f4119; --t-shallow: #2d5a7c; --t-deep: #1a3a60;
  --t-puddle: #36586f; --t-building: #6e5e48; --t-rough: #57493a; --t-swamp: #3e4a33; --t-snow: #6c7680;
  --t-magma: #7a3d20; --t-road: #4b4538; --road: #b8a883; --elev: #f0e6c8; --grid: rgba(255,255,255,0.08);
  --hexlabel: rgba(255,255,255,0.35); --enemy: #9a9a9a;
}
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    --bg: #16181b; --fg: #e4e6e8; --muted: #a0a6ad; --panel: #1f2226; --line: #3a3f45; --accent: #7aa7ff;
    --bad: #ff6b6b; --good: #69c46d; --warn: #ffb74d;
    --t-clear: #2c302a; --t-woods: #36582d; --t-heavywoods: #1f4119; --t-shallow: #2d5a7c; --t-deep: #1a3a60;
    --t-puddle: #36586f; --t-building: #6e5e48; --t-rough: #57493a; --t-swamp: #3e4a33; --t-snow: #6c7680;
    --t-magma: #7a3d20; --t-road: #4b4538; --road: #b8a883; --elev: #f0e6c8; --grid: rgba(255,255,255,0.08);
    --hexlabel: rgba(255,255,255,0.35); --enemy: #9a9a9a;
  }
}
* { box-sizing: border-box; }
body { margin: 0; padding: 16px; background: var(--bg); color: var(--fg);
  font: 14px/1.45 system-ui, -apple-system, "Segoe UI", sans-serif; }
h1 { font-size: 20px; margin: 0 0 4px; } h2 { font-size: 16px; margin: 20px 0 8px; }
.meta { color: var(--muted); margin-bottom: 12px; }
.layout { display: flex; flex-wrap: wrap; gap: 16px; align-items: flex-start; }
.mapbox { background: var(--panel); border: 1px solid var(--line); border-radius: 6px; padding: 8px;
  overflow: auto; max-width: 100%; }
.side { flex: 1 1 360px; min-width: 300px; }
table { border-collapse: collapse; width: 100%; background: var(--panel); font-size: 13px; }
th, td { border: 1px solid var(--line); padding: 4px 6px; text-align: left; vertical-align: top; }
th { background: color-mix(in srgb, var(--panel) 80%, var(--line)); }
.bad { color: var(--bad); font-weight: 600; } .good { color: var(--good); } .warn { color: var(--warn); }
.swatch { display: inline-block; width: 12px; height: 12px; border-radius: 2px; vertical-align: -1px;
  margin-right: 4px; border: 1px solid var(--line); }
.legend { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 4px 12px;
  background: var(--panel); border: 1px solid var(--line); border-radius: 6px; padding: 8px; font-size: 12px; }
.legend svg { vertical-align: middle; margin-right: 4px; }
.t-clear { fill: var(--t-clear); } .t-woods { fill: var(--t-woods); } .t-heavywoods { fill: var(--t-heavywoods); }
.t-shallow { fill: var(--t-shallow); } .t-deep { fill: var(--t-deep); } .t-puddle { fill: var(--t-puddle); }
.t-building { fill: var(--t-building); } .t-rough { fill: var(--t-rough); } .t-swamp { fill: var(--t-swamp); }
.t-snow { fill: var(--t-snow); } .t-magma { fill: var(--t-magma); } .t-road { fill: var(--t-road); }
.hex { stroke: var(--grid); stroke-width: 0.6; }
.elev { fill: var(--elev); font-size: 7px; text-anchor: middle; opacity: 0.55; pointer-events: none; }
.hexno { fill: var(--hexlabel); font-size: 5.5px; text-anchor: middle; pointer-events: none; display: none; }
svg.numbers .hexno { display: inline; }
.roadline { stroke: var(--road); stroke-width: 1.5; stroke-linecap: round; fill: none; opacity: 0.55; }
.roundlabel { font-size: 8px; font-weight: 700; paint-order: stroke; stroke: var(--panel); stroke-width: 2.2px; }
.mismatch { fill: none; stroke: var(--bad); stroke-width: 2.4; }
.toolbar { margin: 6px 0 10px; } .toolbar button { font: inherit; padding: 2px 8px; }
a { color: var(--accent); }
.small { font-size: 12px; color: var(--muted); }
code { font-size: 12px; }
"""

THEME_SCRIPT = """
<script>
function toggleTheme() {
  var root = document.documentElement;
  var current = root.getAttribute('data-theme');
  var dark = current ? current === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches;
  root.setAttribute('data-theme', dark ? 'light' : 'dark');
}
function toggleEnemies() {
  var nodes = document.querySelectorAll('.enemy-layer');
  for (var i = 0; i < nodes.length; i++) {
    nodes[i].style.display = nodes[i].style.display === 'none' ? '' : 'none';
  }
}
function toggleHexNumbers() {
  var map = document.getElementById('board');
  map.classList.toggle('numbers');
}
</script>
"""


def escape(text):
    return html.escape(str(text), quote=True)


def svg_board(board, size, show_numbers):
    parts = []
    road_lines = []
    for row in range(board.height):
        for column in range(board.width):
            elevation, terrain = board.terrain(column, row)
            css = hex_class(elevation, terrain)
            title = hex_number(column, row) + " elev " + str(elevation) + " " + ";".join(
                "%s:%d" % item for item in sorted(terrain.items()))
            parts.append('<polygon class="hex %s" points="%s"><title>%s</title></polygon>'
                         % (css, hex_points(column, row, size), escape(title)))
            centre_x, centre_y = centre(column, row, size)
            if elevation != 0:
                parts.append('<text class="elev" x="%.1f" y="%.1f">%d</text>'
                             % (centre_x, centre_y + size * 0.62, elevation))
            if show_numbers:
                parts.append('<text class="hexno" x="%.1f" y="%.1f">%s</text>'
                             % (centre_x, centre_y - size * 0.55, hex_number(column, row)))
            if "road" in terrain or "bridge" in terrain:
                road_lines.append((column, row))
    # roads: connect to neighbouring road hexes
    road_set = set(road_lines)
    for column, row in road_lines:
        start_x, start_y = centre(column, row, size)
        for neighbour in neighbours(column, row):
            if neighbour in road_set and neighbour > (column, row):
                end_x, end_y = centre(neighbour[0], neighbour[1], size)
                parts.append('<line class="roadline" x1="%.1f" y1="%.1f" x2="%.1f" y2="%.1f"/>'
                             % (start_x, start_y, end_x, end_y))
    return "\n".join(parts)


def neighbours(column, row):
    odd = column & 1
    if odd:
        offsets = [(0, -1), (1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0)]
    else:
        offsets = [(0, -1), (1, -1), (1, 0), (0, 1), (-1, 0), (-1, -1)]
    return [(column + delta_column, row + delta_row) for delta_column, delta_row in offsets]


def facing_tick(column, row, facing, size, colour):
    centre_x, centre_y = centre(column, row, size)
    vector_x, vector_y = FACING_VECTORS[facing % 6]
    end_x = centre_x + vector_x * size * 0.85
    end_y = centre_y + vector_y * size * 0.85
    return ('<line x1="%.1f" y1="%.1f" x2="%.1f" y2="%.1f" stroke="%s" stroke-width="2.4" stroke-linecap="round"/>'
            % (centre_x, centre_y, end_x, end_y, colour))


def svg_unit(track, verdicts, summary, size, width, height, is_enemy):
    colour = "var(--enemy)" if is_enemy else track.colour
    parts = []
    # actual path: start of first round, then every step, rounds in order
    points = []
    first_start = None
    for round_number in sorted(set(list(track.starts.keys()) + list(track.ends.keys()))):
        start_row = track.starts.get(round_number)
        if start_row and position_of(start_row):
            if first_start is None:
                first_start = position_of(start_row)
            if not points or points[-1] != position_of(start_row):
                points.append(position_of(start_row))
        for step in track.paths.get(round_number, []):
            coords = (step[0], step[1])
            if not points or points[-1] != coords:
                points.append(coords)
        end_row = track.ends.get(round_number)
        if end_row and position_of(end_row) and (not points or points[-1] != position_of(end_row)):
            points.append(position_of(end_row))
    if len(points) > 1:
        path_points = " ".join("%.1f,%.1f" % centre(column, row, size) for column, row in points)
        parts.append('<polyline points="%s" fill="none" stroke="%s" stroke-width="%s" stroke-linejoin="round" '
                     'stroke-linecap="round" opacity="%s"/>' % (path_points, colour, "1.6" if is_enemy else "2.6",
                                                                 "0.7" if is_enemy else "0.95"))
    if first_start:
        start_x, start_y = centre(first_start[0], first_start[1], size)
        parts.append('<rect x="%.1f" y="%.1f" width="%.1f" height="%.1f" fill="%s" stroke="var(--panel)" '
                     'stroke-width="1"><title>%s start</title></rect>'
                     % (start_x - size * 0.35, start_y - size * 0.35, size * 0.7, size * 0.7, colour,
                        escape(track.name)))
    if not is_enemy:
        # ordered routes: dashed line from where the unit stood when ordered through each waypoint
        for route_index, (order_round, route, ordered_from) in enumerate(summary["route_history"]):
            if not route:
                continue
            origin = ordered_from or first_start
            route_points = ([origin] if origin else []) + route
            coordinates = " ".join("%.1f,%.1f" % centre(column, row, size) for column, row in route_points)
            parts.append('<polyline points="%s" fill="none" stroke="%s" stroke-width="1.8" stroke-dasharray="6 4" '
                         'opacity="0.9"><title>%s ordered route (round %d)</title></polyline>'
                         % (coordinates, colour, escape(track.name), order_round))
            for waypoint_index, (column, row) in enumerate(route):
                centre_x, centre_y = centre(column, row, size)
                parts.append('<circle cx="%.1f" cy="%.1f" r="%.1f" fill="var(--panel)" stroke="%s" '
                             'stroke-width="2"><title>%s waypoint %d: %s (ordered round %d)</title></circle>'
                             % (centre_x, centre_y, size * 0.55, colour, escape(track.name), waypoint_index + 1,
                                hex_number(column, row), order_round))
                parts.append('<text x="%.1f" y="%.1f" font-size="%.1f" text-anchor="middle" fill="%s" '
                             'font-weight="700">%d</text>' % (centre_x, centre_y + size * 0.25, size * 0.7, colour,
                                                             waypoint_index + 1))
        # flee edge arrow
        if summary["flee_edge"]:
            flee_rounds = [int(order["round"]) for order in track.orders if order["orderAction"] == "FLEE"]
            origin = None
            if flee_rounds and track.starts.get(flee_rounds[-1]):
                origin = position_of(track.starts.get(flee_rounds[-1]))
            if origin:
                target = edge_point(origin, summary["flee_edge"], width, height)
                start_x, start_y = centre(origin[0], origin[1], size)
                end_x, end_y = centre(target[0], target[1], size)
                parts.append('<line x1="%.1f" y1="%.1f" x2="%.1f" y2="%.1f" stroke="%s" stroke-width="1.6" '
                             'stroke-dasharray="2 4" marker-end="url(#arrow)"><title>%s ordered to flee %s'
                             '</title></line>' % (start_x, start_y, end_x, end_y, colour, escape(track.name),
                                                  summary["flee_edge"]))
    # end-of-round markers
    for round_number, end_row in track.ends.items():
        position = position_of(end_row)
        if not position:
            continue
        centre_x, centre_y = centre(position[0], position[1], size)
        verdict = verdicts.get(round_number, {})
        if end_row.get("facing"):
            parts.append(facing_tick(position[0], position[1], int(end_row["facing"]), size, colour))
        parts.append('<circle cx="%.1f" cy="%.1f" r="%.1f" fill="%s" stroke="var(--panel)" stroke-width="0.8">'
                     '<title>%s round %d at %s facing %s: %s</title></circle>'
                     % (centre_x, centre_y, size * 0.28, colour, escape(track.name), round_number,
                        hex_number(*position), FACING_NAMES[int(end_row["facing"]) % 6] if end_row.get("facing")
                        else "?", escape(verdict.get("rule", "") + " " + verdict.get("detail", ""))))
        if not is_enemy:
            parts.append('<text class="roundlabel" x="%.1f" y="%.1f" fill="%s">%d</text>'
                         % (centre_x + size * 0.35, centre_y - size * 0.3, colour, round_number))
            if verdict.get("followed") is False:
                parts.append('<circle class="mismatch" cx="%.1f" cy="%.1f" r="%.1f"><title>Round %d: ordered %s, '
                             'but %s</title></circle>' % (centre_x, centre_y, size * 0.8, round_number,
                                                          escape(verdict["expected"]), escape(verdict["reason"])))
    if track.gone and track.ends:
        last = position_of(track.ends[next(reversed(track.ends))])
        if last:
            centre_x, centre_y = centre(last[0], last[1], size)
            parts.append('<text x="%.1f" y="%.1f" font-size="%.1f" text-anchor="middle" fill="%s" font-weight="700">'
                         'X<title>%s: %s</title></text>' % (centre_x, centre_y + size * 1.3, size, colour,
                                                            escape(track.name), escape(track.gone.get("note", ""))))
    layer_class = "enemy-layer" if is_enemy else "unit-layer"
    return '<g class="%s">%s</g>' % (layer_class, "\n".join(parts))


def edge_point(origin, edge, width, height):
    column, row = origin
    if edge == "NORTH":
        return column, 0
    if edge == "SOUTH":
        return column, height - 1
    if edge == "WEST":
        return 0, row
    if edge == "EAST":
        return width - 1, row
    return origin


def legend_html():
    items = [
        ('<svg width="30" height="10"><line x1="0" y1="5" x2="30" y2="5" stroke="#4363d8" stroke-width="2.6"/></svg>',
         "Actual path (hex by hex)"),
        ('<svg width="30" height="10"><line x1="0" y1="5" x2="30" y2="5" stroke="#4363d8" stroke-width="1.8" '
         'stroke-dasharray="6 4"/></svg>', "Ordered route (from where the order was given)"),
        ('<svg width="16" height="16"><circle cx="8" cy="8" r="6" fill="none" stroke="#4363d8" stroke-width="2"/>'
         '<text x="8" y="11" font-size="8" text-anchor="middle" fill="#4363d8">1</text></svg>', "Waypoint, in order"),
        ('<svg width="16" height="16"><rect x="3" y="3" width="10" height="10" fill="#4363d8"/></svg>',
         "Starting hex"),
        ('<svg width="30" height="16"><circle cx="8" cy="8" r="3" fill="#4363d8"/><line x1="8" y1="8" x2="8" y2="1" '
         'stroke="#4363d8" stroke-width="2"/><text x="14" y="8" font-size="8" fill="#4363d8">3</text></svg>',
         "End of round N, tick = facing"),
        ('<svg width="16" height="16"><circle cx="8" cy="8" r="6" class="mismatch"/></svg>',
         "Round where the unit did not follow its order"),
        ('<svg width="30" height="10"><line x1="0" y1="5" x2="26" y2="5" stroke="#4363d8" stroke-width="1.6" '
         'stroke-dasharray="2 4"/></svg>', "Flee order toward an edge"),
        ('<svg width="30" height="10"><line x1="0" y1="5" x2="30" y2="5" stroke="var(--enemy)" '
         'stroke-width="1.6"/></svg>', "Units without orders (grey)"),
        ('<span class="swatch" style="background:var(--t-woods)"></span>', "Light woods"),
        ('<span class="swatch" style="background:var(--t-heavywoods)"></span>', "Heavy woods"),
        ('<span class="swatch" style="background:var(--t-shallow)"></span>', "Water depth 1"),
        ('<span class="swatch" style="background:var(--t-deep)"></span>', "Water depth 2+"),
        ('<span class="swatch" style="background:var(--t-building)"></span>', "Building"),
        ('<span class="swatch" style="background:var(--t-rough)"></span>', "Rough / rubble"),
        ('<svg width="30" height="10"><line x1="0" y1="5" x2="30" y2="5" class="roadline"/></svg>', "Road"),
        ('<span class="elev" style="font-size:11px">2</span>', "Number in hex = elevation"),
    ]
    return '<div class="legend">' + "".join("<div>%s%s</div>" % (icon, escape(text)) for icon, text in items) + "</div>"


def render_game(trace, board, output_path, label):
    size = 17.0 if max(board.width, board.height) <= 34 else 12.0
    svg_width = size * 1.5 * board.width + size * 0.6
    svg_height = SQRT3 * size * (board.height + 0.5) + 2
    palette_index = 0
    analyses = OrderedDict()
    for track in trace.units.values():
        verdicts, summary = analyse_unit(track, board.width, board.height)
        analyses[track.unit_id] = (verdicts, summary)
        if track.ordered:
            track.colour = UNIT_COLOURS[palette_index % len(UNIT_COLOURS)]
            palette_index += 1
    unit_layers = []
    enemy_layers = []
    for track in trace.units.values():
        verdicts, summary = analyses[track.unit_id]
        layer = svg_unit(track, verdicts, summary, size, board.width, board.height, not track.ordered)
        (unit_layers if track.ordered else enemy_layers).append(layer)
    show_numbers = True
    svg = ('<svg id="board" xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %.1f %.1f" '
           'role="img" aria-label="Board with ordered and actual unit paths">'
           '<defs><marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" '
           'orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="var(--fg)"/></marker></defs>'
           '<g>%s</g>%s%s</svg>') % (svg_width, svg_height, svg_width, svg_height,
                                      svg_board(board, size, show_numbers), "".join(enemy_layers),
                                      "".join(unit_layers))

    # tables
    unit_rows = []
    decision_rows = []
    total_overridden = 0
    ordered_units = 0
    for track in trace.units.values():
        if not track.ordered:
            continue
        ordered_units += 1
        verdicts, summary = analyses[track.unit_id]
        total_overridden += summary["overridden_rounds"]
        order_text = "<br>".join("r%s: %s %s" % (escape(order["round"]), escape(order["orderAction"]),
                                                  escape(order["orderArgs"])) for order in track.orders)
        final_position = summary["final_position"]
        final_text = hex_number(*final_position) if final_position else "-"
        if track.gone:
            final_text += " (" + escape(track.gone.get("note", "")) + " r" + escape(track.gone.get("round", "")) + ")"
        distance_text = "-"
        if summary["final_distance"] is not None:
            distance_text = "%d hexes to %s" % (summary["final_distance"], hex_number(*summary["final_waypoint"]))
        if summary["flee_distance"] is not None:
            distance_text += "; %d to %s edge" % (summary["flee_distance"], summary["flee_edge"])
        rules = ", ".join("%s x%d" % (rule, count) for rule, count in sorted(summary["rule_counts"].items()))
        verdict_class = "bad" if summary["overridden_rounds"] else "good"
        unit_rows.append(
            "<tr><td><span class='swatch' style='background:%s'></span>%s<br><span class='small'>ID %s, %s</span></td>"
            "<td>%s</td><td>%s</td><td>%s</td><td class='%s'>%d of %d</td><td>%s</td></tr>"
            % (track.colour, escape(track.name), escape(track.unit_id), escape(track.owner), order_text, final_text,
               distance_text, verdict_class, summary["followed_rounds"], summary["ordered_rounds"], escape(rules)))
        for round_number, verdict in verdicts.items():
            status = "-"
            status_class = ""
            if verdict["followed"] is True:
                status, status_class = "followed", "good"
            elif verdict["followed"] is False:
                status, status_class = "NOT followed", "bad"
            flags = []
            if verdict["crippled"]:
                flags.append("crippled")
            if verdict["withdrawing"]:
                flags.append("withdrawing")
            decision_rows.append(
                "<tr><td><span class='swatch' style='background:%s'></span>%s</td><td>%d</td><td>%s</td><td>%s</td>"
                "<td>%s</td><td>%s</td><td class='%s'>%s</td><td>%s</td></tr>"
                % (track.colour, escape(track.name), round_number,
                   hex_number(*verdict["position"]) if verdict["position"] else "-",
                   escape(", ".join(flags)), escape(verdict["expected"] or "-"),
                   escape(verdict["rule"]), status_class, status,
                   escape(verdict["detail"] or (verdict["reason"] if verdict["followed"] is False else ""))))

    rounds_played = (trace.rounds[-1] - trace.rounds[0] + 1) if trace.rounds else 0
    title = "%s - game %s" % (trace.header.get("scenarioName", "Bot orders"), trace.game)
    derived_note = ""
    if not any(row.get("rule") for row in trace.rows if row["stage"] == "end"):
        derived_note = ("<p class='warn'>This build writes no [BotOrders] decision lines, so every rule here is "
                        "derived from the bot's cached behaviour (marked *).</p>")
    page = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>%(title)s</title><style>%(style)s</style>%(script)s</head>
<body>
<h1>%(title)s</h1>
<div class="meta">%(label)s Board %(board)s (%(width)dx%(height)d). Rounds %(first)s-%(last)s (%(rounds)d).
Orders: <code>%(orders)s</code>. Trace: <code>%(trace)s</code>.</div>
<div class="toolbar"><button onclick="toggleTheme()">Light / dark</button>
<button onclick="toggleEnemies()">Show / hide units without orders</button>
<button onclick="toggleHexNumbers()">Show / hide hex numbers</button>
<a href="index.html">All games</a></div>
%(derived)s
<div class="layout"><div class="mapbox">%(svg)s</div>
<div class="side"><h2>Ordered units</h2>
<table><tr><th>Unit</th><th>Orders (round: action)</th><th>Final hex</th><th>Distance at end</th>
<th>Rounds following orders</th><th>Rules seen</th></tr>%(units)s</table>
<h2>Legend</h2>%(legend)s</div></div>
<h2>Round by round (ordered units)</h2>
<p class="small">Rule comes from the bot's [BotOrders] log line; a * means it was derived from the bot's cached behaviour
because the build does not log one. "Expected" is the order in force that round.</p>
<table><tr><th>Unit</th><th>Round</th><th>End hex</th><th>State</th><th>Expected</th><th>Rule</th><th>Result</th>
<th>Logged reason</th></tr>%(decisions)s</table>
</body></html>
""" % {
        "title": escape(title), "style": STYLE, "script": THEME_SCRIPT,
        "label": (escape(label) + ".") if label else "", "board": escape(os.path.basename(board.path)),
        "width": board.width, "height": board.height,
        "first": trace.rounds[0] if trace.rounds else "-", "last": trace.rounds[-1] if trace.rounds else "-",
        "rounds": rounds_played, "orders": escape(os.path.basename(trace.header.get("orders", ""))),
        "trace": escape(os.path.basename(trace.path)), "derived": derived_note, "svg": svg,
        "units": "".join(unit_rows), "legend": legend_html(), "decisions": "".join(decision_rows),
    }
    with open(output_path, "w", encoding="utf-8") as output_file:
        output_file.write(page)
    unit_summaries = []
    for track in trace.units.values():
        if not track.ordered:
            continue
        verdicts, summary = analyses[track.unit_id]
        unit_summaries.append({
            "id": track.unit_id, "name": track.name, "owner": track.owner,
            "followed": summary["followed_rounds"], "ordered": summary["ordered_rounds"],
            "overridden": summary["overridden_rounds"], "rules": summary["rule_counts"],
            "final_hex": hex_number(*summary["final_position"]) if summary["final_position"] else "",
            "final_waypoint": hex_number(*summary["final_waypoint"]) if summary["final_waypoint"] else "",
            "final_distance": summary["final_distance"], "flee_edge": summary["flee_edge"],
            "flee_distance": summary["flee_distance"], "gone": track.gone.get("note", "") if track.gone else "",
            "overrides": [{"round": round_number, "expected": verdict["expected"], "reason": verdict["reason"]}
                          for round_number, verdict in verdicts.items() if verdict["followed"] is False],
        })
    return {
        "page": os.path.basename(output_path), "title": title, "scenario": trace.header.get("scenarioName", ""),
        "game": trace.game, "rounds": rounds_played, "ordered_units": ordered_units,
        "overridden_rounds": total_overridden, "units": unit_summaries, "trace": os.path.basename(trace.path),
    }


def render_index(summaries, output_dir, label):
    rows = []
    for summary in summaries:
        reached = sum(1 for unit in summary["units"]
                      if unit["final_distance"] is not None and unit["final_distance"] <= 3)
        with_waypoint = sum(1 for unit in summary["units"] if unit["final_waypoint"])
        overridden_units = [unit["name"] + " (r" + ",".join(str(item["round"]) for item in unit["overrides"]) + ")"
                            for unit in summary["units"] if unit["overrides"]]
        line = "%d ordered units; %d/%d ended within 3 hexes of their last waypoint; %d rounds not following orders" % (
            summary["ordered_units"], reached, with_waypoint, summary["overridden_rounds"])
        rows.append("<tr><td><a href='%s'>%s</a></td><td>%s</td><td>%d</td><td class='%s'>%s</td><td>%s</td></tr>"
                    % (escape(summary["page"]), escape(summary["title"]), escape(summary["game"]), summary["rounds"],
                       "bad" if summary["overridden_rounds"] else "good", escape(line),
                       escape("; ".join(overridden_units)) or "-"))
    page = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Bot orders games</title><style>%s</style>%s</head><body>
<h1>Bot orders test games%s</h1>
<div class="toolbar"><button onclick="toggleTheme()">Light / dark</button></div>
<table><tr><th>Game</th><th>#</th><th>Rounds</th><th>Summary</th><th>Units that did not follow orders (rounds)</th></tr>
%s</table></body></html>
""" % (STYLE, THEME_SCRIPT, (" - " + escape(label)) if label else "", "".join(rows))
    with open(os.path.join(output_dir, "index.html"), "w", encoding="utf-8") as output_file:
        output_file.write(page)


def render_master(output_dir, set_dirs):
    sections = []
    for set_dir in set_dirs:
        summary_path = os.path.join(set_dir, "summary.json")
        if not os.path.isfile(summary_path):
            print("skipping %s: no summary.json" % set_dir, file=sys.stderr)
            continue
        with open(summary_path, encoding="utf-8") as summary_file:
            data = json.load(summary_file)
        relative = os.path.relpath(set_dir, output_dir).replace("\\", "/")
        rows = []
        for game in data["games"]:
            rows.append("<tr><td><a href='%s/%s'>%s</a></td><td>%d</td><td class='%s'>%d</td></tr>"
                        % (escape(relative), escape(game["page"]), escape(game["title"]), game["rounds"],
                           "bad" if game["overridden_rounds"] else "good", game["overridden_rounds"]))
        sections.append("<h2><a href='%s/index.html'>%s</a></h2><table><tr><th>Game</th><th>Rounds</th>"
                        "<th>Rounds not following orders</th></tr>%s</table>"
                        % (escape(relative), escape(data.get("label") or relative), "".join(rows)))
    page = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Bot orders test runs</title><style>%s</style>%s</head><body>
<h1>Bot orders test runs</h1>
<div class="toolbar"><button onclick="toggleTheme()">Light / dark</button></div>
%s</body></html>
""" % (STYLE, THEME_SCRIPT, "".join(sections))
    with open(os.path.join(output_dir, "index.html"), "w", encoding="utf-8") as output_file:
        output_file.write(page)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", required=True, help="output directory")
    parser.add_argument("--label", default="", help="name of this set of games, for example 'fix' or 'baseline'")
    parser.add_argument("--boards", action="append", default=[], help="extra board directory to search")
    parser.add_argument("--master", action="store_true", help="write a top index over rendered set directories")
    parser.add_argument("inputs", nargs="+", help="trace TSV files, or set directories with --master")
    arguments = parser.parse_args()
    os.makedirs(arguments.out, exist_ok=True)
    if arguments.master:
        render_master(arguments.out, arguments.inputs)
        print("wrote " + os.path.join(arguments.out, "index.html"))
        return
    summaries = []
    for trace_path in arguments.inputs:
        trace = Trace(trace_path)
        scenario_path = trace.header.get("scenario", "")
        board_name = board_name_from_scenario(scenario_path)
        board = Board(find_board(scenario_path, board_name, arguments.boards))
        page_name = os.path.splitext(os.path.basename(trace_path))[0] + ".html"
        summary = render_game(trace, board, os.path.join(arguments.out, page_name), arguments.label)
        summaries.append(summary)
        print("rendered %s -> %s (%d ordered units, %d rounds not following orders)"
              % (os.path.basename(trace_path), page_name, summary["ordered_units"], summary["overridden_rounds"]))
    render_index(summaries, arguments.out, arguments.label)
    with open(os.path.join(arguments.out, "summary.json"), "w", encoding="utf-8") as summary_file:
        json.dump({"label": arguments.label, "games": summaries}, summary_file, indent=1)


if __name__ == "__main__":
    main()
