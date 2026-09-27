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
package megamek.common.orders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvFighter;
import megamek.common.units.Dropship;
import megamek.common.units.FixedWingSupport;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.VTOL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OrderEligibilityTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void groundUnitsHelicoptersAndConventionalAircraftTakeOrders() {
        assertTrue(OrderEligibility.isOrderableKind(new BipedMek()));
        assertTrue(OrderEligibility.isOrderableKind(new VTOL()));
        assertTrue(OrderEligibility.isOrderableKind(new ConvFighter()));
        assertTrue(OrderEligibility.isOrderableKind(new FixedWingSupport()));
    }

    @Test
    void aerospaceFightersAndDropShipsFlyTheirOwnMissions() {
        // HammerGS: orders are a ground-map option; no aerospace fighters or DropShips
        assertFalse(OrderEligibility.isOrderableKind(new AeroSpaceFighter()));
        assertFalse(OrderEligibility.isOrderableKind(new Dropship()));
    }

    @Test
    void aLandAirMekTakesOrdersOnlyOutOfFighterMode() {
        LandAirMek landAirMek = new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD, LandAirMek.LAM_STANDARD);
        assertTrue(OrderEligibility.isOrderableKind(landAirMek));

        landAirMek.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);
        assertFalse(OrderEligibility.isOrderableKind(landAirMek));
    }
}
