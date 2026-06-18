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

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the base VTOL movement points of mechanized VTOL conventional infantry: Microlite platoons get 6 VTOL MP and
 * Micro-Copter platoons get 5 VTOL MP (TO:AUE p.136). The MP must come out correct regardless of whether the microlite flag
 * is set before or after the movement mode, because the two construction paths set them in different orders (the .blk
 * loader sets microlite first; the MegaMekLab structure tab sets the mode first). Regression test for MegaMek issue #8354.
 */
class ConvInfantryVtolMovementTest {

    private static final int MICROLITE_VTOL_MP = 6;
    private static final int MICRO_COPTER_VTOL_MP = 5;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    @DisplayName("Microlite gets 6 VTOL MP when the flag is set before the movement mode (.blk loader order)")
    void microliteFlagBeforeMode() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setMicrolite(true);
        infantry.setMovementMode(EntityMovementMode.VTOL);

        assertEquals(MICROLITE_VTOL_MP, infantry.getOriginalJumpMP());
    }

    @Test
    @DisplayName("Microlite gets 6 VTOL MP when the flag is set after the movement mode (MegaMekLab order)")
    void microliteFlagAfterMode() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setMovementMode(EntityMovementMode.VTOL);
        infantry.setMicrolite(true);

        assertEquals(MICROLITE_VTOL_MP, infantry.getOriginalJumpMP());
    }

    @Test
    @DisplayName("Micro-Copter gets 5 VTOL MP when the flag is set before the movement mode (.blk loader order)")
    void microCopterFlagBeforeMode() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setMicrolite(false);
        infantry.setMovementMode(EntityMovementMode.VTOL);

        assertEquals(MICRO_COPTER_VTOL_MP, infantry.getOriginalJumpMP());
    }

    @Test
    @DisplayName("Micro-Copter gets 5 VTOL MP when switching from Microlite while already in VTOL mode")
    void switchMicroliteToMicroCopterInVtolMode() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setMicrolite(true);
        infantry.setMovementMode(EntityMovementMode.VTOL);

        // Switching the platoon type without re-selecting the movement mode must still correct the MP.
        infantry.setMicrolite(false);

        assertEquals(MICRO_COPTER_VTOL_MP, infantry.getOriginalJumpMP());
    }
}
