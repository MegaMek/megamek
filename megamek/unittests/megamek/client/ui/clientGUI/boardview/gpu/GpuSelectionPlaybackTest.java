/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Test;

class GpuSelectionPlaybackTest {
    @Test
    void selectionRingTravelsWithTheModelInsteadOfAppearingAtItsDestination() {
        var start = UnitPlaybackTest.unit(1, 0);
        var end = UnitPlaybackTest.unit(1, 4);
        var motion = new UnitMotion(start.location());
        motion.append(List.of(start.location(), end.location()), EntityMovementType.MOVE_WALK, 0);
        motion.advance(motion.remainingSeconds() * .4, 1);
        var position = motion.position().cpy();
        var ring = new UnitFootprint.Pose(end, position, motion.facing());
        var center = center(ring, end.location().coords());
        assertTrue(center.epsilonEquals(position.cpy().add(0, 0, .5f), .001f));
        assertTrue(center.dst(motion.destination()) > BoardGeometry.HEIGHT);
        position.setZero();
        assertTrue(center.epsilonEquals(center(ring, end.location().coords()), .001f), "The frame pose owns its position");
    }

    @Test
    void multihexSelectionTurnsWithTheSameModelPoseAndPreservesCapturedOccupancy() {
        var origin = new Coords(4, 4);
        var front = origin.translated(0);
        var footprint = List.of(origin, front);
        var unit = new BoardScene.Unit(1, -1, "Large unit", new BoardScene.Waypoint(origin, 0, 0), null, false,
              null, 3, false, null, 0, footprint);
        var position = new Vector3(300, 400, 50);
        var ring = new UnitFootprint.Pose(unit, position, 90);
        assertTrue(center(ring, origin).epsilonEquals(new Vector3(300, 400, 50.5f), .001f));
        assertTrue(center(ring, front).epsilonEquals(new Vector3(300 + BoardGeometry.HEIGHT, 400, 50.5f), .001f));
        assertEquals(footprint, unit.footprint());
        assertEquals(origin, unit.location().coords());
        assertEquals(new Vector3(300, 400, 50), position);
    }

    private static Vector3 center(UnitFootprint.Pose ring, Coords occupied) {
        Vector3 sum = new Vector3();
        for (int corner = 0; corner < 6; corner++) {
            sum.add(ring.outlinePoint(occupied, corner));
        }
        return sum.scl(1f / 6);
    }
}
