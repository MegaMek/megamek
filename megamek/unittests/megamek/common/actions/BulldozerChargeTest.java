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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for vehicle bulldozer charge mechanics per TacOps.
 *
 * <p>A charging bulldozer takes half the usual damage (rounded down); the half stacks multiplicatively with the
 * half-damage for charging through water. A front-mounted bulldozer doubles the charge damage dealt to a building
 * hex.</p>
 */
class BulldozerChargeTest {

    private Tank createTankWithBulldozer(boolean frontMounted) {
        Tank tank = mock(Tank.class);
        when(tank.hasWorkingBulldozer()).thenReturn(true);
        when(tank.hasFrontMountedBulldozer()).thenReturn(frontMounted);
        when(tank.getLocationStatus(1)).thenReturn(ILocationExposureStatus.NORMAL);
        return tank;
    }

    private Tank createTankWithoutBulldozer() {
        Tank tank = mock(Tank.class);
        when(tank.hasWorkingBulldozer()).thenReturn(false);
        when(tank.hasFrontMountedBulldozer()).thenReturn(false);
        when(tank.getLocationStatus(1)).thenReturn(ILocationExposureStatus.NORMAL);
        return tank;
    }

    @Nested
    @DisplayName("getDamageTakenBy() - bulldozer half self-damage")
    class HalfSelfDamageTests {

        @Test
        @DisplayName("Bulldozer attacker takes half the charge self-damage, rounded down")
        void bulldozerHalvesSelfDamage() {
            Tank attacker = createTankWithBulldozer(false);
            Mek target = mock(Mek.class);
            when(target.getWeight()).thenReturn(100.0);

            // Baseline (no bulldozer): ceil(100 / 10) = 10; bulldozer: floor(10 * 0.5) = 5.
            assertEquals(5, ChargeAttackAction.getDamageTakenBy(attacker, target, false, 1),
                  "A charging bulldozer takes half the usual self-damage (TacOps)");
        }

        @Test
        @DisplayName("Non-bulldozer attacker takes full charge self-damage")
        void nonBulldozerTakesFullDamage() {
            Tank attacker = createTankWithoutBulldozer();
            Mek target = mock(Mek.class);
            when(target.getWeight()).thenReturn(100.0);

            assertEquals(10, ChargeAttackAction.getDamageTakenBy(attacker, target, false, 1));
        }

        @Test
        @DisplayName("Bulldozer half-damage stacks multiplicatively with charging through water")
        void bulldozerStacksWithWater() {
            Tank attacker = createTankWithBulldozer(false);
            when(attacker.getLocationStatus(1)).thenReturn(ILocationExposureStatus.WET);
            Mek target = mock(Mek.class);
            when(target.getWeight()).thenReturn(100.0);

            // Water: ceil(100 / 10 * 0.5) = 5; bulldozer: floor(5 * 0.5) = 2.
            assertEquals(2, ChargeAttackAction.getDamageTakenBy(attacker, target, false, 1),
                  "Bulldozer and water half-damage stack multiplicatively, rounded down");
        }
    }

    @Nested
    @DisplayName("getBuildingChargeDamage() - front bulldozer doubles building damage")
    class BuildingChargeDamageTests {

        @Test
        @DisplayName("Front-mounted bulldozer doubles charge damage to a building")
        void frontBulldozerDoublesBuildingDamage() {
            Tank attacker = createTankWithBulldozer(true);
            assertEquals(20, ChargeAttackAction.getBuildingChargeDamage(attacker, 10),
                  "A front-mounted bulldozer doubles building charge damage (TacOps)");
        }

        @Test
        @DisplayName("Rear-mounted bulldozer does not double building charge damage")
        void rearBulldozerDoesNotDouble() {
            Tank attacker = createTankWithBulldozer(false);
            assertEquals(10, ChargeAttackAction.getBuildingChargeDamage(attacker, 10),
                  "Only a front-mounted bulldozer doubles building charge damage");
        }

        @Test
        @DisplayName("No bulldozer leaves building charge damage unchanged")
        void noBulldozerUnchanged() {
            Tank attacker = createTankWithoutBulldozer();
            assertEquals(10, ChargeAttackAction.getBuildingChargeDamage(attacker, 10));
        }
    }
}
