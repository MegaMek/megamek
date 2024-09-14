/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import megamek.common.*;
import megamek.common.InfantryBay.PlatoonType;

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

        assertEquals(cargoBay.getWeight(), weight, 0.01);
    }

    @Test
    void testRefrigeratedCargoBayMultiplier() {
        final double weight = 2.0;
        Bay cargoBay = BayData.REFRIGERATED_CARGO.newBay(weight, 0);

        assertEquals(cargoBay.getWeight(), weight, 0.01);
    }

    @Test
    void testInsulatedCargoBayMultiplier() {
        final double weight = 2.0;
        Bay cargoBay = BayData.INSULATED_CARGO.newBay(weight, 0);

        assertEquals(cargoBay.getWeight(), weight, 0.01);
    }

    @Test
    void testLivestockCargoBayMultiplier() {
        final double weight = 2.0;
        Bay cargoBay = BayData.LIVESTOCK_CARGO.newBay(weight, 0);

        assertEquals(cargoBay.getWeight(), weight, 0.01);
    }

    @Test
    void identifyMekBay() {
        Bay bay = new MekBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.MEK);
    }

    @Test
    void identifyProtoMekBay() {
        Bay bay = new ProtoMekBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.PROTOMEK);
    }

    @Test
    void identifyHeavyVehicleBay() {
        Bay bay = new HeavyVehicleBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.VEHICLE_HEAVY);
    }

    @Test
    void identifyLightVehicleBay() {
        Bay bay = new LightVehicleBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.VEHICLE_LIGHT);
    }

    @Test
    void identifySuperHeavyVehicleBay() {
        Bay bay = new SuperHeavyVehicleBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.VEHICLE_SH);
    }

    @Test
    void identifyFootInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, PlatoonType.FOOT);

        assertEquals(BayData.getBayType(bay), BayData.INFANTRY_FOOT);
    }

    @Test
    void identifyJumpInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, PlatoonType.JUMP);

        assertEquals(BayData.getBayType(bay), BayData.INFANTRY_JUMP);
    }

    @Test
    void identifyMotorizedInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, PlatoonType.MOTORIZED);

        assertEquals(BayData.getBayType(bay), BayData.INFANTRY_MOTORIZED);
    }

    @Test
    void identifyMechanizedInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, PlatoonType.MECHANIZED);

        assertEquals(BayData.getBayType(bay), BayData.INFANTRY_MECHANIZED);
    }

    @Test
    void identifyISBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, false, false);

        assertEquals(BayData.getBayType(bay), BayData.IS_BATTLE_ARMOR);
    }

    @Test
    void identifyClanBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, true, false);

        assertEquals(BayData.getBayType(bay), BayData.CLAN_BATTLE_ARMOR);
    }

    @Test
    void identifyCSBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, false, true);

        assertEquals(BayData.getBayType(bay), BayData.CS_BATTLE_ARMOR);
    }

    @Test
    void identifyFighterBay() {
        Bay bay = new ASFBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.FIGHTER);
    }

    @Test
    void identifySmallCraftBay() {
        Bay bay = new SmallCraftBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.SMALL_CRAFT);
    }

    @Test
    void identifyCargoBay() {
        Bay bay = new CargoBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.CARGO);
    }

    @Test
    void identifyLiquidCargoBay() {
        Bay bay = new LiquidCargoBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.LIQUID_CARGO);
    }

    @Test
    void identifyRefrigeratedCargoBay() {
        Bay bay = new RefrigeratedCargoBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.REFRIGERATED_CARGO);
    }

    @Test
    void identifyInsulatedCargoBay() {
        Bay bay = new InsulatedCargoBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.INSULATED_CARGO);
    }

    @Test
    void identifyLivestockCargoBay() {
        Bay bay = new LivestockCargoBay(1, 1, 0);

        assertEquals(BayData.getBayType(bay), BayData.LIVESTOCK_CARGO);
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
