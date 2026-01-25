/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.MMConstants;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EMPMineEffectsTable} effect threshold lookups.
 */
class EMPMineEffectsTableTest {

    @Test
    void testBattleMekThresholds() {
        // BattleMek: 2-6 No Effect / 7-8 Interference / 9+ Shutdown
        Entity mek = mock(Entity.class);
        when(mek.isMek()).thenReturn(true);
        when(mek.isIndustrialMek()).thenReturn(false);

        EMPMineEffectsTable table = EMPMineEffectsTable.getTableFor(mek);

        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(2));
        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(6));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(7));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(8));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(9));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(12));
    }

    @Test
    void testIndustrialMekThresholds() {
        // IndustrialMek: 2-5 No Effect / 6-7 Interference / 8+ Shutdown
        Entity mek = mock(Entity.class);
        when(mek.isMek()).thenReturn(true);
        when(mek.isIndustrialMek()).thenReturn(true);

        EMPMineEffectsTable table = EMPMineEffectsTable.getTableFor(mek);

        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(2));
        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(5));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(6));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(7));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(8));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(12));
    }

    @Test
    void testProtoMekThresholds() {
        // ProtoMek: 2-5 No Effect / 6-8 Interference / 9+ Shutdown
        Entity protoMek = mock(Entity.class);
        when(protoMek.isMek()).thenReturn(false);
        when(protoMek.isProtoMek()).thenReturn(true);

        EMPMineEffectsTable table = EMPMineEffectsTable.getTableFor(protoMek);

        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(2));
        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(5));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(6));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(8));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(9));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(12));
    }

    @Test
    void testBattleArmorThresholds() {
        // Battle Armor: 2-5 No Effect / 6-7 Interference / 8+ Shutdown
        Entity battleArmor = mock(Entity.class);
        when(battleArmor.isMek()).thenReturn(false);
        when(battleArmor.isProtoMek()).thenReturn(false);
        when(battleArmor.isBattleArmor()).thenReturn(true);

        EMPMineEffectsTable table = EMPMineEffectsTable.getTableFor(battleArmor);

        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(2));
        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(5));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(6));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(7));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(8));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(12));
    }

    @Test
    void testCombatVehicleThresholds() {
        // Combat Vehicles: 2-5 No Effect / 6-7 Interference / 8+ Shutdown
        Entity vehicle = mock(Entity.class);
        when(vehicle.isMek()).thenReturn(false);
        when(vehicle.isProtoMek()).thenReturn(false);
        when(vehicle.isBattleArmor()).thenReturn(false);
        when(vehicle.isSupportVehicle()).thenReturn(false);
        when(vehicle.isVehicle()).thenReturn(true);

        EMPMineEffectsTable table = EMPMineEffectsTable.getTableFor(vehicle);

        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(2));
        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(5));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(6));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(7));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(8));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(12));
    }

    @Test
    void testSupportVehicleThresholds() {
        // Support Vehicles: 2-4 No Effect / 5-6 Interference / 7+ Shutdown
        Entity supportVehicle = mock(Entity.class);
        when(supportVehicle.isMek()).thenReturn(false);
        when(supportVehicle.isProtoMek()).thenReturn(false);
        when(supportVehicle.isBattleArmor()).thenReturn(false);
        when(supportVehicle.isSupportVehicle()).thenReturn(true);

        EMPMineEffectsTable table = EMPMineEffectsTable.getTableFor(supportVehicle);

        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(2));
        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(4));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(5));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(6));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(7));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(12));
    }

    @Test
    void testAerospaceThresholds() {
        // Aerospace/Small Craft: 2-6 No Effect / 7-8 Interference / 9+ Shutdown
        Entity aero = mock(Entity.class);
        when(aero.isMek()).thenReturn(false);
        when(aero.isProtoMek()).thenReturn(false);
        when(aero.isBattleArmor()).thenReturn(false);
        when(aero.isSupportVehicle()).thenReturn(false);
        when(aero.isVehicle()).thenReturn(false);
        when(aero.isConventionalFighter()).thenReturn(false);
        when(aero.isAero()).thenReturn(true);

        EMPMineEffectsTable table = EMPMineEffectsTable.getTableFor(aero);

        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(2));
        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(6));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(7));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(8));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(9));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(12));
    }

    @Test
    void testConventionalFighterThresholds() {
        // Conventional Fighters: 2-5 No Effect / 6-7 Interference / 8+ Shutdown
        Entity convFighter = mock(Entity.class);
        when(convFighter.isMek()).thenReturn(false);
        when(convFighter.isProtoMek()).thenReturn(false);
        when(convFighter.isBattleArmor()).thenReturn(false);
        when(convFighter.isSupportVehicle()).thenReturn(false);
        when(convFighter.isVehicle()).thenReturn(false);
        when(convFighter.isConventionalFighter()).thenReturn(true);

        EMPMineEffectsTable table = EMPMineEffectsTable.getTableFor(convFighter);

        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(2));
        assertEquals(MMConstants.EMP_EFFECT_NONE, table.determineEffect(5));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(6));
        assertEquals(MMConstants.EMP_EFFECT_INTERFERENCE, table.determineEffect(7));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(8));
        assertEquals(MMConstants.EMP_EFFECT_SHUTDOWN, table.determineEffect(12));
    }
}
