/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common;

import static megamek.common.ToHitData.HIT_KICK;
import static megamek.common.ToHitData.HIT_NORMAL;
import static megamek.common.ToHitData.HIT_PUNCH;
import static megamek.common.ToHitData.SIDE_LEFT;
import static megamek.common.ToHitData.SIDE_RIGHT;
import static megamek.common.units.Mek.LOC_CENTER_LEG;
import static megamek.common.units.Mek.LOC_CENTER_TORSO;
import static megamek.common.units.Mek.LOC_HEAD;
import static megamek.common.units.Mek.LOC_LEFT_ARM;
import static megamek.common.units.Mek.LOC_LEFT_TORSO;
import static megamek.common.units.Mek.LOC_RIGHT_ARM;
import static megamek.common.units.Mek.LOC_RIGHT_LEG;
import static megamek.common.units.Mek.LOC_RIGHT_TORSO;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.TripodMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PlaytestTests {
    @BeforeAll
    static void setUpAll() {
        // Need equipment initialized
        EquipmentType.initializeTypes();
    }

    @Test
    void testBipedSideTable() {
        Mek mek = new BipedMek();
        for (int i = 0; i < 100; i++) {
            var location = mek.getPlaytestSideLocation(HIT_NORMAL, SIDE_LEFT, LosEffects.COVER_NONE).getLocation();
            assertNotEquals(LOC_RIGHT_ARM, location);
            assertNotEquals(LOC_RIGHT_LEG, location);
            assertNotEquals(LOC_RIGHT_TORSO, location);
        }
    }

    @Test
    void testTripodKickSideTable() {
        Mek mek = new TripodMek();
        for (int i = 0; i < 100; i++) {
            var location = mek.getPlaytestSideLocation(HIT_KICK, SIDE_RIGHT, LosEffects.COVER_NONE).getLocation();
            assertTrue(location == LOC_RIGHT_LEG || location == LOC_CENTER_LEG);
        }
    }

    @Test
    void testQuadPunchSideTable() {
        Mek mek = new QuadMek();
        for (int i = 0; i < 100; i++) {
            var location = mek.getPlaytestSideLocation(HIT_PUNCH, SIDE_LEFT, LosEffects.COVER_NONE).getLocation();
            assertTrue(location == LOC_LEFT_ARM || location == LOC_LEFT_TORSO || location == LOC_HEAD || location == LOC_CENTER_TORSO);
        }
    }
}
