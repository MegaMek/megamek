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
package megamek.common.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GhostTargetActionTest {

    @Test
    void testConstructorAndGetters() {
        GhostTargetAction action = new GhostTargetAction(1, 5, 2, true);

        assertEquals(1, action.getEntityId());
        assertEquals(5, action.getEquipmentId());
        assertEquals(2, action.getTargetEntityId());
        assertTrue(action.isTargetFriendly());
    }

    @Test
    void testEnemyTarget() {
        GhostTargetAction action = new GhostTargetAction(10, 3, 20, false);

        assertEquals(10, action.getEntityId());
        assertEquals(3, action.getEquipmentId());
        assertEquals(20, action.getTargetEntityId());
        assertFalse(action.isTargetFriendly());
    }

    @Test
    void testCCCSentinelEquipmentId() {
        assertEquals(-2, GhostTargetAction.CCC_EQUIPMENT_ID);

        GhostTargetAction action = new GhostTargetAction(1, GhostTargetAction.CCC_EQUIPMENT_ID, 2, true);
        assertEquals(GhostTargetAction.CCC_EQUIPMENT_ID, action.getEquipmentId());
    }

    @Test
    void testToStringFriendly() {
        GhostTargetAction action = new GhostTargetAction(1, 5, 2, true);
        String result = action.toString();

        assertTrue(result.contains("GhostTargetAction"));
        assertTrue(result.contains("protecting"));
        assertTrue(result.contains("target 2"));
    }

    @Test
    void testToStringEnemy() {
        GhostTargetAction action = new GhostTargetAction(1, 5, 2, false);
        String result = action.toString();

        assertTrue(result.contains("GhostTargetAction"));
        assertTrue(result.contains("jamming"));
        assertTrue(result.contains("target 2"));
    }
}
