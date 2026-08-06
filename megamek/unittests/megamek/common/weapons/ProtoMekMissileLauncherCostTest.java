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
package megamek.common.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for ProtoMek missile launcher costs (issue #8325).
 *
 * <p>Per TechManual (p. 291), ProtoMek-specific launchers are priced per tube (per missile in the rack):
 * LRM and SRM launchers cost 10,000 C-Bills per tube, and Streak SRM launchers cost 15,000 C-Bills per tube. These are
 * the dedicated ProtoMek launcher classes (the ones that strip the Mek/Tank/BA/Aero flags), which previously had a flat
 * 80,000 cost (SRM) or an unset cost of 0 (LRM and Streak SRM).</p>
 */
public class ProtoMekMissileLauncherCostTest {

    private static final double PROTOMEK_LRM_COST_PER_TUBE = 10_000;
    private static final double PROTOMEK_SRM_COST_PER_TUBE = 10_000;
    private static final double PROTOMEK_STREAK_SRM_COST_PER_TUBE = 15_000;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static void assertCostByTube(String internalName, int rackSize, double costPerTube) {
        EquipmentType equipmentType = EquipmentType.get(internalName);
        assertNotNull(equipmentType, internalName + " should be a registered equipment type");
        assertEquals(rackSize * costPerTube, equipmentType.getRawCost(),
              internalName + " should cost " + costPerTube + " per tube");
    }

    @Nested
    @DisplayName("ProtoMek SRM launchers cost 10,000 per tube")
    class ProtoMekSrmCosts {

        @Test
        void srm1Costs10000() {
            assertCostByTube("CLSRM1", 1, PROTOMEK_SRM_COST_PER_TUBE);
        }

        @Test
        void srm3Costs30000() {
            assertCostByTube("CLSRM3", 3, PROTOMEK_SRM_COST_PER_TUBE);
        }

        @Test
        void srm5Costs50000() {
            assertCostByTube("CLSRM5", 5, PROTOMEK_SRM_COST_PER_TUBE);
        }
    }

    @Nested
    @DisplayName("ProtoMek Streak SRM launchers cost 15,000 per tube")
    class ProtoMekStreakSrmCosts {

        @Test
        void streakSrm1Costs15000() {
            assertCostByTube("CLStreakSRM1", 1, PROTOMEK_STREAK_SRM_COST_PER_TUBE);
        }

        @Test
        void streakSrm3Costs45000() {
            assertCostByTube("CLStreakSRM3", 3, PROTOMEK_STREAK_SRM_COST_PER_TUBE);
        }

        @Test
        void streakSrm5Costs75000() {
            assertCostByTube("CLStreakSRM5", 5, PROTOMEK_STREAK_SRM_COST_PER_TUBE);
        }
    }

    @Nested
    @DisplayName("ProtoMek LRM launchers cost 10,000 per tube")
    class ProtoMekLrmCosts {

        @Test
        void allProtoMekLrmLaunchersCostPerTube() {
            int[] rackSizes = { 1, 2, 3, 4, 6, 7, 8, 9, 11, 12, 13, 14, 16, 17, 18, 19 };
            for (int rackSize : rackSizes) {
                assertCostByTube("CLLRM" + rackSize, rackSize, PROTOMEK_LRM_COST_PER_TUBE);
            }
        }
    }
}
