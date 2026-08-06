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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for vehicle saw charge mechanics per TM pp.241-243.
 *
 * <p>A front-mounted chainsaw or dual saw on a vehicle can be used in a modified
 * charge attack with +1 to-hit, flat damage (5 for chainsaw, 7 for dual saw), and altered hit tables.</p>
 */
class SawChargeTest {

    /**
     * Creates a mock Tank with a front-mounted chainsaw.
     */
    private Tank createTankWithFrontChainsaw() {
        Tank tank = mock(Tank.class);
        when(tank.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_CHAINSAW, Tank.LOC_FRONT)).thenReturn(true);
        when(tank.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_DUAL_SAW, Tank.LOC_FRONT)).thenReturn(false);
        when(tank.hasFrontMountedSaw()).thenReturn(true);
        return tank;
    }

    /**
     * Creates a mock Tank with a front-mounted dual saw.
     */
    private Tank createTankWithFrontDualSaw() {
        Tank tank = mock(Tank.class);
        when(tank.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_CHAINSAW, Tank.LOC_FRONT)).thenReturn(false);
        when(tank.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_DUAL_SAW, Tank.LOC_FRONT)).thenReturn(true);
        when(tank.hasFrontMountedSaw()).thenReturn(true);
        return tank;
    }

    /**
     * Creates a mock Tank with both front-mounted chainsaw and dual saw.
     */
    private Tank createTankWithBothSaws() {
        Tank tank = mock(Tank.class);
        when(tank.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_CHAINSAW, Tank.LOC_FRONT)).thenReturn(true);
        when(tank.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_DUAL_SAW, Tank.LOC_FRONT)).thenReturn(true);
        when(tank.hasFrontMountedSaw()).thenReturn(true);
        return tank;
    }

    /**
     * Creates a mock Tank with no front-mounted saws.
     */
    private Tank createTankWithNoSaw() {
        Tank tank = mock(Tank.class);
        when(tank.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_CHAINSAW, Tank.LOC_FRONT)).thenReturn(false);
        when(tank.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_DUAL_SAW, Tank.LOC_FRONT)).thenReturn(false);
        when(tank.hasFrontMountedSaw()).thenReturn(false);
        return tank;
    }

    @Nested
    @DisplayName("hasFrontMountedSaw()")
    class HasFrontMountedSawTests {

        @Test
        @DisplayName("Tank with front chainsaw returns true")
        void tankWithFrontChainsaw() {
            Tank tank = createTankWithFrontChainsaw();
            assertTrue(ChargeAttackAction.hasFrontMountedSaw(tank));
        }

        @Test
        @DisplayName("Tank with front dual saw returns true")
        void tankWithFrontDualSaw() {
            Tank tank = createTankWithFrontDualSaw();
            assertTrue(ChargeAttackAction.hasFrontMountedSaw(tank));
        }

        @Test
        @DisplayName("Tank with no front saw returns false")
        void tankWithNoSaw() {
            Tank tank = createTankWithNoSaw();
            assertFalse(ChargeAttackAction.hasFrontMountedSaw(tank));
        }

        @Test
        @DisplayName("Mek is not eligible for saw charge")
        void mekNotEligible() {
            Mek mek = mock(Mek.class);
            assertFalse(ChargeAttackAction.hasFrontMountedSaw(mek));
        }

        @Test
        @DisplayName("Non-tank Entity is not eligible for saw charge")
        void nonTankNotEligible() {
            Entity entity = mock(Entity.class);
            assertFalse(ChargeAttackAction.hasFrontMountedSaw(entity));
        }
    }

    @Nested
    @DisplayName("getSawChargeDamage()")
    class GetSawChargeDamageTests {

        @Test
        @DisplayName("Chainsaw deals 5 damage vs Mek")
        void chainsawVsMek() {
            Tank attacker = createTankWithFrontChainsaw();
            Mek target = mock(Mek.class);
            when(target.isConventionalInfantry()).thenReturn(false);

            assertEquals(5, ChargeAttackAction.getSawChargeDamage(attacker, target),
                  "Chainsaw charge should deal 5 damage to Mek (TM p.241)");
        }

        @Test
        @DisplayName("Dual saw deals 7 damage vs Mek")
        void dualSawVsMek() {
            Tank attacker = createTankWithFrontDualSaw();
            Mek target = mock(Mek.class);
            when(target.isConventionalInfantry()).thenReturn(false);

            assertEquals(7, ChargeAttackAction.getSawChargeDamage(attacker, target),
                  "Dual saw charge should deal 7 damage to Mek (TM p.243)");
        }

        @Test
        @DisplayName("Dual saw takes priority over chainsaw when both present")
        void dualSawPriorityOverChainsaw() {
            Tank attacker = createTankWithBothSaws();
            Mek target = mock(Mek.class);
            when(target.isConventionalInfantry()).thenReturn(false);

            assertEquals(7, ChargeAttackAction.getSawChargeDamage(attacker, target),
                  "Dual saw should take priority over chainsaw (7 > 5)");
        }

        @Test
        @DisplayName("Chainsaw deals 5 damage vs Tank")
        void chainsawVsTank() {
            Tank attacker = createTankWithFrontChainsaw();
            Tank target = createTankWithNoSaw();
            when(target.isConventionalInfantry()).thenReturn(false);

            assertEquals(5, ChargeAttackAction.getSawChargeDamage(attacker, target),
                  "Chainsaw charge should deal 5 damage to vehicles (TM p.241)");
        }

        @Test
        @DisplayName("Chainsaw deals 1d6 vs conventional infantry")
        void chainsawVsInfantry() {
            Tank attacker = createTankWithFrontChainsaw();
            Infantry target = mock(Infantry.class);
            when(target.isConventionalInfantry()).thenReturn(true);

            int damage = ChargeAttackAction.getSawChargeDamage(attacker, target);
            assertTrue(damage >= 1 && damage <= 6,
                  "Chainsaw charge should deal 1d6 (1-6) damage to infantry, got: " + damage);
        }

        @Test
        @DisplayName("Dual saw deals 1d6 vs conventional infantry")
        void dualSawVsInfantry() {
            Tank attacker = createTankWithFrontDualSaw();
            Infantry target = mock(Infantry.class);
            when(target.isConventionalInfantry()).thenReturn(true);

            int damage = ChargeAttackAction.getSawChargeDamage(attacker, target);
            assertTrue(damage >= 1 && damage <= 6,
                  "Dual saw charge should deal 1d6 (1-6) damage to infantry, got: " + damage);
        }

        @Test
        @DisplayName("No front saw returns 0 damage")
        void noSawReturnsZero() {
            Tank attacker = createTankWithNoSaw();
            Mek target = mock(Mek.class);
            when(target.isConventionalInfantry()).thenReturn(false);

            assertEquals(0, ChargeAttackAction.getSawChargeDamage(attacker, target),
                  "No front-mounted saw should return 0 damage");
        }
    }

    @Nested
    @DisplayName("getMaxSawChargeDamage()")
    class GetMaxSawChargeDamageTests {

        @Test
        @DisplayName("Chainsaw max damage is 5 vs non-infantry")
        void chainsawMaxDamage() {
            Tank attacker = createTankWithFrontChainsaw();
            Mek target = mock(Mek.class);
            when(target.isConventionalInfantry()).thenReturn(false);

            assertEquals(5, ChargeAttackAction.getMaxSawChargeDamage(attacker, target));
        }

        @Test
        @DisplayName("Dual saw max damage is 7 vs non-infantry")
        void dualSawMaxDamage() {
            Tank attacker = createTankWithFrontDualSaw();
            Mek target = mock(Mek.class);
            when(target.isConventionalInfantry()).thenReturn(false);

            assertEquals(7, ChargeAttackAction.getMaxSawChargeDamage(attacker, target));
        }

        @Test
        @DisplayName("Max damage vs infantry is 6 (max of 1d6)")
        void maxDamageVsInfantry() {
            Tank attacker = createTankWithFrontChainsaw();
            Infantry target = mock(Infantry.class);
            when(target.isConventionalInfantry()).thenReturn(true);

            assertEquals(6, ChargeAttackAction.getMaxSawChargeDamage(attacker, target),
                  "Max saw charge damage vs infantry should be 6 (max of 1d6)");
        }
    }
}
