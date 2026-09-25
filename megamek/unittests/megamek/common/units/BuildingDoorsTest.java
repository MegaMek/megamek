// Copyright (C) 2026 The MegaMek Team
// SPDX-License-Identifier: GPL-3.0-or-later

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import megamek.common.board.CubeCoords;
import org.junit.jupiter.api.Test;

class BuildingDoorsTest {
    @Test
    void allocatesDistinctPositiveGroupsWithoutOverflowingImportedIds() {
        var doors = new ArrayList<BuildingDesign.Door>();
        int[] groups = { Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 0 };
        for (int i = 0; i < groups.length; i++) {
            var hex = new CubeCoords(0, 3 * i, -3 * i);
            doors.add(door(hex, 0, 0, 1, groups[i]));
            doors.add(door(hex, 0, 1, 1, groups[i]));
        }
        BuildingDoors.link(doors, doors.get(6), doors.get(7));
        assertEquals(List.of(2, 2, 2, 2), BuildingDoors.groups(doors).stream().map(List::size).toList());
        assertEquals(4, doors.stream().map(BuildingDesign.Door::linkGroup).distinct().count());
        assertTrue(doors.stream().allMatch(door -> door.linkGroup() > 0));
        assertEquals(Integer.MAX_VALUE, doors.getFirst().linkGroup());
        assertEquals(1, doors.get(4).linkGroup());
    }

    @Test
    void clipsTemplateSectionLinesWithoutChangingTheOpeningsArrowDirections() {
        var south = new CubeCoords(0, 1, -1);
        var doors = List.of(door(CubeCoords.ZERO, 0, 1, 1, 1), door(CubeCoords.ZERO, 0, 2, 1, 1), door(south, 0, 1, 1, 1));
        var full = BuildingDoors.geometry(doors);
        var top = BuildingDoors.geometry(doors, door -> door.position().hex().equals(CubeCoords.ZERO));
        assertEquals(2, top.size());
        assertEquals(full.get(doors.get(1)).arrow(), top.get(doors.get(1)).arrow());
        assertEquals(2, top.get(doors.get(1)).line().size());
        var bottom = BuildingDoors.geometry(doors, door -> door.position().hex().equals(south));
        assertEquals(full.get(doors.get(2)).arrow(), bottom.get(doors.get(2)).arrow());
        assertEquals(1, bottom.get(doors.get(2)).line().size());
    }

    @Test
    void separatesDisconnectedGroupsAndLevelsWithoutLinkingSingletons() {
        var doors = new ArrayList<>(List.of(
              door(CubeCoords.ZERO, 0, 0, 1, 7), door(CubeCoords.ZERO, 0, 1, 1, 7),
              door(CubeCoords.ZERO, 1, 0, 1, 7), door(CubeCoords.ZERO, 1, 1, 1, 7),
              door(new CubeCoords(-10, -10, 20), 0, 0, 1, 7)));
        BuildingDoors.normalize(doors);
        int separated = doors.get(2).linkGroup();
        assertTrue(separated > 0);
        assertNotEquals(7, separated);
        assertEquals(List.of(7, 7, separated, separated, 0), doors.stream().map(BuildingDesign.Door::linkGroup).toList());
        BuildingDoors.unlink(doors, doors.getFirst());
        assertEquals(List.of(0, 0, separated, separated, 0), doors.stream().map(BuildingDesign.Door::linkGroup).toList());
        assertEquals(1, BuildingDoors.groups(doors).size());
    }

    @Test
    void linksOnlyMatchingLevelsHeightsAndSharedCorners() {
        var a = door(CubeCoords.ZERO, 0, 0, 1, 0);
        var adjacent = door(CubeCoords.ZERO, 0, 1, 1, 0);
        assertTrue(BuildingDoors.touch(a, adjacent));
        assertFalse(BuildingDoors.touch(a, door(CubeCoords.ZERO, 1, 1, 1, 0)));
        assertFalse(BuildingDoors.touch(a, door(CubeCoords.ZERO, 0, 1, 2, 0)));
        assertFalse(BuildingDoors.touch(a, door(CubeCoords.ZERO, 0, 3, 1, 0)));
        assertFalse(BuildingDoors.touch(a, door(new CubeCoords(0, -1, 1), 0, 3, 1, 0)));
        var doors = new ArrayList<>(List.of(a, adjacent));
        BuildingDoors.link(doors, a, adjacent);
        assertEquals(2, BuildingDoors.groups(doors).getFirst().size());
    }

    @Test
    void keepsArrowsCenteredAndOutwardThroughRotationsAndNegativeCoordinates() {
        List<BuildingDesign.Door> doors = List.of(
              door(new CubeCoords(-11, -10, 21), 0, 1, 2, 1),
              door(new CubeCoords(-11, -10, 21), 0, 2, 2, 1),
              door(new CubeCoords(-11, -9, 20), 0, 1, 2, 1));
        for (int rotation = 0; rotation < 6; rotation++) {
            var geometry = BuildingDoors.geometry(doors);
            assertEquals(3, geometry.size());
            for (var door : doors) {
                var shape = geometry.get(door);
                double angle = -Math.PI / 2 + door.facing() * Math.PI / 3;
                double x = Math.sqrt(3) / 2 * Math.cos(angle), y = Math.sqrt(3) / 2 * Math.sin(angle);
                assertEquals(x, shape.arrow().stream().mapToDouble(BuildingDoors.Point::x).average().orElseThrow(), .0001);
                assertEquals(y, shape.arrow().stream().mapToDouble(BuildingDoors.Point::y).average().orElseThrow(), .0001);
                var tip = shape.arrow().getFirst();
                assertEquals(.26, Math.hypot(tip.x() - x, tip.y() - y), .0001);
                assertTrue((tip.x() - x) * x + (tip.y() - y) * y > 0);
                assertEquals(door == doors.get(1) ? 3 : 2, shape.line().size());
            }
            doors = doors.stream().map(door -> {
                var hex = door.position().hex();
                return door(new CubeCoords(-hex.r(), -hex.s(), -hex.q()), 0, (door.facing() + 1) % 6, 2, 1);
            }).toList();
        }
    }

    private BuildingDesign.Door door(CubeCoords hex, int level, int facing, int height, int group) {
        return new BuildingDesign.Door(new BuildingDesign.Position(hex, level), facing, height, group);
    }
}
