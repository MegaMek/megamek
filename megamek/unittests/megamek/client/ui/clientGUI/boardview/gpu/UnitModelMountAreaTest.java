/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UnitModelMountAreaTest {
    @Test
    void duplicateWeaponsMoveWithinTheAreaWithoutOverlapping() {
        var area = new UnitModelMountArea();
        var first = area.place(2, 3, 4, 4, 14, 14, .4f);
        var next = area.place(2, 3, 4, 4, 14, 14, .4f);
        assertNotNull(first);
        assertNotNull(next);
        assertEquals(1, first.scale());
        assertEquals(1, next.scale());
        assertTrue(Math.abs(first.x() - next.x()) >= 4.4f || Math.abs(first.z() - next.z()) >= 4.4f);
        assertTrue(Math.abs(next.x() - 2) + 2 <= 7);
        assertTrue(Math.abs(next.z() - 3) + 2 <= 7);
    }

    @Test
    void mirroredFacesPackTheirSecondWeaponAsAMirrorImage() {
        // Two weapons on one socket, once on each side of the centre line, each told to step inward.
        var right = new UnitModelMountArea();
        right.place(9, 0, 2, 2, 7, 16, .4f, -1);
        var rightSecond = right.place(9, 0, 2, 2, 7, 16, .4f, -1);
        var left = new UnitModelMountArea();
        left.place(-9, 0, 2, 2, 7, 16, .4f, 1);
        var leftSecond = left.place(-9, 0, 2, 2, 7, 16, .4f, 1);
        assertNotNull(rightSecond);
        assertNotNull(leftSecond);
        assertEquals(-rightSecond.x(), leftSecond.x(), .001f, "the left torso must mirror the right, not copy it");
        assertEquals(rightSecond.z(), leftSecond.z(), .001f);
    }

    @Test
    void theDefaultTieBreakIsUnchangedForExistingCallers() {
        var legacy = new UnitModelMountArea();
        legacy.place(0, 0, 2, 2, 7, 16, .4f);
        var explicit = new UnitModelMountArea();
        explicit.place(0, 0, 2, 2, 7, 16, .4f, -1);
        var legacySecond = legacy.place(0, 0, 2, 2, 7, 16, .4f);
        var explicitSecond = explicit.place(0, 0, 2, 2, 7, 16, .4f, -1);
        assertEquals(legacySecond.x(), explicitSecond.x(), .001f);
        assertEquals(legacySecond.z(), explicitSecond.z(), .001f);
    }

    @Test
    void crowdedAreasFailExplicitlyAndFailedAttemptsDoNotReserveSpace() {
        var area = new UnitModelMountArea();
        assertNull(area.place(0, 0, 100, 100, 6, 6, .4f));
        assertNotNull(area.place(0, 0, 6, 6, 6, 6, 1));
        assertNull(area.place(0, 0, 1, 1, 6, 6, 1));
    }
}
