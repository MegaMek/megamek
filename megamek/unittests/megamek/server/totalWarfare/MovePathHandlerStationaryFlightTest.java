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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.ConvInfantry;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.InfantryMount;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the TW p.54 rule that VTOL-capable creatures must spend 1 MP per turn even if remaining stationary: an
 * airborne beast-mounted platoon (e.g. Branth) that holds its hex is converted from MOVE_NONE to MOVE_VTOL_WALK at the
 * end of movement processing. Ground beasts, grounded flying beasts and unmounted VTOL infantry (microlites) are not
 * affected. Part of the fix for MegaMek issue #8707.
 */
class MovePathHandlerStationaryFlightTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private ConvInfantry createBranthInfantry() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setMount(InfantryMount.BRANTH);
        return infantry;
    }

    @Test
    @DisplayName("Airborne Branth platoon that did not move must spend its stationary flight MP")
    void airborneBranthHoldingStillMustSpend() {
        ConvInfantry branth = createBranthInfantry();
        branth.setElevation(2);
        branth.moved = EntityMovementType.MOVE_NONE;

        assertTrue(MovePathHandler.mustSpendStationaryFlightMP(branth),
              "An airborne Branth platoon that holds its hex must still spend 1 MP (TW p.54)");
    }

    @Test
    @DisplayName("Grounded Branth platoon that did not move spends nothing")
    void groundedBranthHoldingStillSpendsNothing() {
        ConvInfantry branth = createBranthInfantry();
        branth.setElevation(0);
        branth.moved = EntityMovementType.MOVE_NONE;

        assertFalse(MovePathHandler.mustSpendStationaryFlightMP(branth),
              "A Branth platoon on the ground is not flying and spends no MP while stationary");
    }

    @Test
    @DisplayName("Airborne Branth platoon that already moved is left alone")
    void airborneBranthThatMovedIsLeftAlone() {
        ConvInfantry branth = createBranthInfantry();
        branth.setElevation(2);
        branth.moved = EntityMovementType.MOVE_VTOL_WALK;

        assertFalse(MovePathHandler.mustSpendStationaryFlightMP(branth),
              "A Branth platoon that already moved needs no stationary MP conversion");
    }

    @Test
    @DisplayName("Ground beast mount is not affected")
    void groundBeastNotAffected() {
        ConvInfantry camel = new ConvInfantry();
        camel.setMount(InfantryMount.CAMEL);
        camel.moved = EntityMovementType.MOVE_NONE;

        assertFalse(MovePathHandler.mustSpendStationaryFlightMP(camel),
              "A ground beast mount is not a VTOL-capable creature");
    }

    @Test
    @DisplayName("Unmounted VTOL infantry (microlite) is not affected")
    void microliteNotAffected() {
        ConvInfantry microlite = new ConvInfantry();
        microlite.setMicrolite(true);
        microlite.setMovementMode(EntityMovementMode.VTOL);
        microlite.setElevation(2);
        microlite.moved = EntityMovementType.MOVE_NONE;

        assertFalse(MovePathHandler.mustSpendStationaryFlightMP(microlite),
              "The TW p.54 stationary MP rule applies to VTOL-capable creatures, not microlite platoons");
    }
}
