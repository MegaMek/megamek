/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Linked structural openings and their unit-radius geometry, shared by editors and record sheets. */
public final class BuildingDoors {
    private BuildingDoors() { }

    public record Point(double x, double y) {
        public Point midpoint(Point other) {
            return new Point((x + other.x) / 2, (y + other.y) / 2);
        }
    }
    public record Geometry(List<Point> line, List<Point> arrow) { }
    private static final int[][] VERTICES = { { 2, 0 }, { 1, 1 }, { -1, 1 }, { -2, 0 }, { -1, -1 }, { 1, -1 } };

    /** Integer lattice vertices avoid rounding in shared-corner detection. */
    public static List<Point> vertices(BuildingDesign.Door door) {
        List<Point> result = new ArrayList<>();
        for (int offset : new int[] { 4, 5 }) {
            int[] vertex = VERTICES[Math.floorMod(door.facing() + offset, 6)];
            var hex = door.position().hex();
            result.add(new Point(3 * hex.q() + vertex[0], 2 * hex.r() + hex.q() + vertex[1]));
        }
        return result;
    }

    public static boolean touch(BuildingDesign.Door a, BuildingDesign.Door b) {
        return !a.equals(b) && a.position().level() == b.position().level() && a.height() == b.height()
              && vertices(a).stream().filter(vertices(b)::contains).count() == 1;
    }

    public static List<List<BuildingDesign.Door>> groups(List<BuildingDesign.Door> doors) {
        var pending = new LinkedHashSet<>(doors.stream().filter(door -> door.linkGroup() > 0).toList());
        List<List<BuildingDesign.Door>> groups = new ArrayList<>();
        while (!pending.isEmpty()) {
            var group = new ArrayList<BuildingDesign.Door>();
            group.add(pending.iterator().next());
            pending.remove(group.getFirst());
            for (int i = 0; i < group.size(); i++) {
                var door = group.get(i);
                for (var other : List.copyOf(pending)) {
                    if (door.linkGroup() == other.linkGroup() && touch(door, other)) {
                        group.add(other);
                        pending.remove(other);
                    }
                }
            }
            if (group.size() > 1) {
                groups.add(group);
            }
        }
        return groups;
    }

    public static void link(List<BuildingDesign.Door> doors, BuildingDesign.Door a, BuildingDesign.Door b) {
        if (!touch(a, b)) {
            return;
        }
        int group = a.linkGroup() > 0 ? a.linkGroup() : b.linkGroup() > 0 ? b.linkGroup()
              : reserveLinkGroup(doors.stream().map(BuildingDesign.Door::linkGroup).collect(Collectors.toSet()));
        doors.replaceAll(door -> door.equals(a) || door.equals(b) || (a.linkGroup() > 0 && door.linkGroup() == a.linkGroup())
              || (b.linkGroup() > 0 && door.linkGroup() == b.linkGroup())
              ? new BuildingDesign.Door(door.position(), door.facing(), door.height(), group) : door);
        normalize(doors);
    }

    /** The connected physical opening containing this segment, or the independent door itself. */
    public static List<BuildingDesign.Door> opening(List<BuildingDesign.Door> doors, BuildingDesign.Door door) {
        return door.linkGroup() > 0 ? groups(doors).stream().filter(group -> group.contains(door)).findFirst().orElse(List.of(door))
              : List.of(door);
    }

    public static void unlink(List<BuildingDesign.Door> doors, BuildingDesign.Door target) {
        doors.replaceAll(door -> door.equals(target) ? new BuildingDesign.Door(door.position(), door.facing(), door.height()) : door);
        normalize(doors);
    }

    public static void normalize(List<BuildingDesign.Door> doors) {
        var reserved = doors.stream().map(BuildingDesign.Door::linkGroup).collect(Collectors.toSet());
        var used = new HashSet<Integer>();
        Map<BuildingDesign.Door, Integer> assignments = new HashMap<>();
        for (var group : groups(doors)) {
            int id = used.contains(group.getFirst().linkGroup()) ? reserveLinkGroup(reserved) : group.getFirst().linkGroup();
            used.add(id);
            group.forEach(door -> assignments.put(door, id));
        }
        doors.replaceAll(door -> new BuildingDesign.Door(door.position(), door.facing(), door.height(), assignments.getOrDefault(door, 0)));
    }

    /** Fill an unused positive ID instead of incrementing a possibly maximum imported ID. */
    private static int reserveLinkGroup(Set<Integer> reserved) {
        int id = 1;
        while (reserved.contains(id)) { id++; }
        reserved.add(id);
        return id;
    }

    public static Map<BuildingDesign.Door, Geometry> geometry(List<BuildingDesign.Door> doors) {
        return geometry(doors, door -> true);
    }

    /** Keep the opening's arrow directions, but end lines at the last segment visible on a template section. */
    public static Map<BuildingDesign.Door, Geometry> geometry(List<BuildingDesign.Door> doors, Predicate<BuildingDesign.Door> visible) {
        Map<BuildingDesign.Door, Geometry> result = new HashMap<>();
        for (var group : groups(doors)) {
            Map<BuildingDesign.Door, Point> centers = new HashMap<>();
            for (var door : group) {
                var vertex = vertices(door);
                centers.put(door, new Point((vertex.getFirst().x() + vertex.getLast().x()) / 4,
                      (vertex.getFirst().y() + vertex.getLast().y()) * Math.sqrt(3) / 4));
            }
            for (var door : group) {
                var c = centers.get(door);
                if (!visible.test(door)) { continue; }
                var neighbors = group.stream().filter(other -> touch(door, other)).toList();
                var p = centers.get(neighbors.getFirst());
                var q = neighbors.size() > 1 ? centers.get(neighbors.get(1)) : new Point(2 * c.x() - p.x(), 2 * c.y() - p.y());
                double nx = -(q.y() - p.y()), ny = q.x() - p.x();
                double length = Math.hypot(nx, ny);
                if (length == 0) {
                    continue;
                }
                nx /= length;
                ny /= length;
                var hex = door.position().hex();
                double hx = 1.5 * hex.q(), hy = Math.sqrt(3) * (hex.r() + hex.q() / 2.0);
                if (nx * (c.x() - hx) + ny * (c.y() - hy) < 0) {
                    nx = -nx;
                    ny = -ny;
                }
                List<Point> visibleLine = new ArrayList<>();
                if (visible.test(neighbors.getFirst())) { visibleLine.add(c.midpoint(p)); }
                visibleLine.add(c);
                if (neighbors.size() > 1 && visible.test(neighbors.get(1))) { visibleLine.add(c.midpoint(q)); }
                List<Point> line = visibleLine.stream().map(point -> new Point(point.x() - hx, point.y() - hy)).toList();
                result.put(door, new Geometry(line, List.of(
                      new Point(c.x() - hx + nx * .26, c.y() - hy + ny * .26),
                      new Point(c.x() - hx - nx * .13 - ny * .18, c.y() - hy - ny * .13 + nx * .18),
                      new Point(c.x() - hx - nx * .13 + ny * .18, c.y() - hy - ny * .13 - nx * .18))));
            }
        }
        return result;
    }
}
