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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the Large Vehicle exclusion from the vehicle hull-down rules (TO:AUE): Large Support Vehicles and super-heavy
 * combat vehicles cannot use infantry-built (fortified) hexes for cover.
 *
 * <p>Both {@link Tank#canGoHullDown()} and the {@code HULL_DOWN} step legality check in
 * {@code MoveStep} delegate the "is this a Large Vehicle?" decision to {@link Tank#isLargeVehicleForHullDown()}, so
 * verifying that predicate covers the RAW gate at its single source of truth.</p>
 */
class HullDownLargeVehicleTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    @DisplayName("A standard combat vehicle is not a Large Vehicle and may use fortified hexes for hull-down")
    void standardTankIsNotLargeVehicle() {
        Tank tank = new Tank();

        assertFalse(tank.isLargeVehicleForHullDown(),
              "A standard combat vehicle should not be treated as a Large Vehicle for hull-down");
    }

    @Test
    @DisplayName("A super-heavy combat vehicle is a Large Vehicle and is excluded from hull-down")
    void superHeavyTankIsLargeVehicle() {
        SuperHeavyTank tank = new SuperHeavyTank();

        assertTrue(tank.isLargeVehicleForHullDown(),
              "A super-heavy combat vehicle should be treated as a Large Vehicle for hull-down");
    }

    @Test
    @DisplayName("A Large Support Vehicle is a Large Vehicle and is excluded from hull-down")
    void largeSupportTankIsLargeVehicle() {
        LargeSupportTank tank = new LargeSupportTank();

        assertTrue(tank.isLargeVehicleForHullDown(),
              "A Large Support Vehicle should be treated as a Large Vehicle for hull-down");
    }

    @Test
    @DisplayName("A standard ground combat vehicle is hull-down capable")
    void standardTankIsHullDownCapable() {
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.TRACKED);

        assertTrue(tank.isHullDownCapable(), "A tracked combat vehicle should be hull-down capable");
    }

    @Test
    @DisplayName("Naval, hydrofoil, and submarine vehicles are not hull-down capable")
    void waterVehiclesAreNotHullDownCapable() {
        for (EntityMovementMode waterMode : new EntityMovementMode[] {
              EntityMovementMode.NAVAL, EntityMovementMode.HYDROFOIL, EntityMovementMode.SUBMARINE }) {
            Tank tank = new Tank();
            tank.setMovementMode(waterMode);

            assertFalse(tank.isHullDownCapable(),
                  "A " + waterMode + " vehicle cannot hull down (no fortified land hex)");
        }
    }

    @Test
    @DisplayName("A Large Vehicle is not hull-down capable regardless of movement mode")
    void largeVehicleIsNotHullDownCapable() {
        SuperHeavyTank tank = new SuperHeavyTank();
        tank.setMovementMode(EntityMovementMode.TRACKED);

        assertFalse(tank.isHullDownCapable(), "A Large Vehicle should never be hull-down capable");
    }
}
