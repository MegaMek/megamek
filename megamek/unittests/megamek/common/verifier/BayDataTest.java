/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.bays.*;
import megamek.common.units.Entity;
import megamek.common.units.PlatoonType;
import org.junit.jupiter.api.Test;

class BayDataTest {

    private Entity createEntity(long etype) {
        Entity entity = mock(Entity.class);
        when(entity.hasETypeFlag(anyLong())).thenAnswer(inv -> ((Long) inv.getArguments()[0] & etype) != 0);
        return entity;
    }

    @Test
    void testCargoBayMultiplier() {
        final double size = 2.0;
        Bay cargoBay = BayData.CARGO.newBay(size, 0);

        assertEquals(cargoBay.getWeight(), BayData.CARGO.getWeight() * size, 0.01);
    }

    @Test
    void testLiquidCargoBayMultiplier() {
        final double weight = 2.0;
        Bay cargoBay = BayData.LIQUID_CARGO.newBay(weight, 0);

        assertEquals(weight, cargoBay.getWeight(), 0.01);
    }

    @Test
    void testRefrigeratedCargoBayMultiplier() {
        final double weight = 2.0;
        Bay cargoBay = BayData.REFRIGERATED_CARGO.newBay(weight, 0);

        assertEquals(weight, cargoBay.getWeight(), 0.01);
    }

    @Test
    void testInsulatedCargoBayMultiplier() {
        final double weight = 2.0;
        Bay cargoBay = BayData.INSULATED_CARGO.newBay(weight, 0);

        assertEquals(weight, cargoBay.getWeight(), 0.01);
    }

    @Test
    void testLivestockCargoBayMultiplier() {
        final double weight = 2.0;
        Bay cargoBay = BayData.LIVESTOCK_CARGO.newBay(weight, 0);

        assertEquals(weight, cargoBay.getWeight(), 0.01);
    }

    @Test
    void identifyMekBay() {
        Bay bay = new MekBay(1, 1, 0);

        assertEquals(BayData.MEK, BayData.getBayType(bay));
    }

    @Test
    void identifyProtoMekBay() {
        Bay bay = new ProtoMekBay(1, 1, 0);

        assertEquals(BayData.PROTOMEK, BayData.getBayType(bay));
    }

    @Test
    void identifyHeavyVehicleBay() {
        Bay bay = new HeavyVehicleBay(1, 1, 0);

        assertEquals(BayData.VEHICLE_HEAVY, BayData.getBayType(bay));
    }

    @Test
    void identifyLightVehicleBay() {
        Bay bay = new LightVehicleBay(1, 1, 0);

        assertEquals(BayData.VEHICLE_LIGHT, BayData.getBayType(bay));
    }

    @Test
    void identifySuperHeavyVehicleBay() {
        Bay bay = new SuperHeavyVehicleBay(1, 1, 0);

        assertEquals(BayData.VEHICLE_SH, BayData.getBayType(bay));
    }

    @Test
    void identifyFootInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, PlatoonType.FOOT);

        assertEquals(BayData.INFANTRY_FOOT, BayData.getBayType(bay));
    }

    @Test
    void identifyJumpInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, PlatoonType.JUMP);

        assertEquals(BayData.INFANTRY_JUMP, BayData.getBayType(bay));
    }

    @Test
    void identifyMotorizedInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, PlatoonType.MOTORIZED);

        assertEquals(BayData.INFANTRY_MOTORIZED, BayData.getBayType(bay));
    }

    @Test
    void identifyMechanizedInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, PlatoonType.MECHANIZED);

        assertEquals(BayData.INFANTRY_MECHANIZED, BayData.getBayType(bay));
    }

    @Test
    void identifyISBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, false, false);

        assertEquals(BayData.IS_BATTLE_ARMOR, BayData.getBayType(bay));
    }

    @Test
    void identifyClanBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, true, false);

        assertEquals(BayData.CLAN_BATTLE_ARMOR, BayData.getBayType(bay));
    }

    @Test
    void identifyCSBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, false, true);

        assertEquals(BayData.CS_BATTLE_ARMOR, BayData.getBayType(bay));
    }

    @Test
    void identifyFighterBay() {
        Bay bay = new ASFBay(1, 1, 0);

        assertEquals(BayData.FIGHTER, BayData.getBayType(bay));
    }

    @Test
    void identifySmallCraftBay() {
        Bay bay = new SmallCraftBay(1, 1, 0);

        assertEquals(BayData.SMALL_CRAFT, BayData.getBayType(bay));
    }

    @Test
    void identifyCargoBay() {
        Bay bay = new CargoBay(1, 1, 0);

        assertEquals(BayData.CARGO, BayData.getBayType(bay));
    }

    @Test
    void identifyLiquidCargoBay() {
        Bay bay = new LiquidCargoBay(1, 1, 0);

        assertEquals(BayData.LIQUID_CARGO, BayData.getBayType(bay));
    }

    @Test
    void identifyRefrigeratedCargoBay() {
        Bay bay = new RefrigeratedCargoBay(1, 1, 0);

        assertEquals(BayData.REFRIGERATED_CARGO, BayData.getBayType(bay));
    }

    @Test
    void identifyInsulatedCargoBay() {
        Bay bay = new InsulatedCargoBay(1, 1, 0);

        assertEquals(BayData.INSULATED_CARGO, BayData.getBayType(bay));
    }

    @Test
    void identifyLivestockCargoBay() {
        Bay bay = new LivestockCargoBay(1, 1, 0);

        assertEquals(BayData.LIVESTOCK_CARGO, BayData.getBayType(bay));
    }

    @Test
    void cargoBayLegalForMek() {
        Entity entity = createEntity(Entity.ETYPE_MEK);

        assertTrue(BayData.CARGO.isLegalFor(entity));
    }

    @Test
    void livestockBayIllegalForMek() {
        Entity entity = createEntity(Entity.ETYPE_MEK);

        assertFalse(BayData.LIVESTOCK_CARGO.isLegalFor(entity));
    }

    @Test
    void cargoBayLegalForTank() {
        Entity entity = createEntity(Entity.ETYPE_TANK);

        assertTrue(BayData.CARGO.isLegalFor(entity));
    }

    @Test
    void livestockBayLegalForTank() {
        Entity entity = createEntity(Entity.ETYPE_TANK);

        assertTrue(BayData.LIVESTOCK_CARGO.isLegalFor(entity));
    }

    @Test
    void bayIllegalForInfantry() {
        Entity entity = createEntity(Entity.ETYPE_INFANTRY);

        assertFalse(BayData.CARGO.isLegalFor(entity));
    }
}
