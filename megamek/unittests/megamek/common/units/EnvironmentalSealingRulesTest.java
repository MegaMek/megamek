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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.Engine;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.exceptions.LocationFullException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the Environmental Sealing rules, TM p.216 and TO:AUE p.115.
 * <p>
 * BattleMeks are sealed by their basic construction and survive vacuum. IndustrialMeks are not: without the sealing
 * they die on an airless world, and the sealing only helps if the engine also runs with no air to breathe, which
 * rules out an internal combustion engine both in vacuum and fully submerged.
 */
class EnvironmentalSealingRulesTest {

    private static final String MEK_ENVIRONMENTAL_SEALING = "Environmental Sealing (Mech)";
    private static final String CV_ENVIRONMENTAL_SEALING = "Environmental Sealed Chassis";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static BipedMek battleMek(int engineType) {
        BipedMek mek = new BipedMek();
        mek.setWeight(50.0);
        mek.setEngine(new Engine(200, engineType, 0));
        return mek;
    }

    private static BipedMek industrialMek(int engineType, boolean sealed) throws LocationFullException {
        BipedMek mek = new BipedMek();
        mek.setWeight(50.0);
        mek.setStructureType(EquipmentType.T_STRUCTURE_INDUSTRIAL);
        mek.setEngine(new Engine(200, engineType, 0));
        if (sealed) {
            EquipmentType sealing = EquipmentType.get(MEK_ENVIRONMENTAL_SEALING);
            assertNotNull(sealing, "The IndustrialMek Environmental Sealing equipment must exist");
            mek.addEquipment(sealing, Mek.LOC_CENTER_TORSO);
        }
        return mek;
    }

    @Test
    void battleMekSurvivesVacuumWithoutInstallingSealing() {
        BipedMek battleMek = battleMek(Engine.NORMAL_ENGINE);

        assertFalse(battleMek.hasEnvironmentalSealing(),
              "A BattleMek carries no Environmental Sealing equipment - it may not install any");
        assertTrue(EnvironmentalSealingRules.isSealedAgainstAtmosphere(battleMek),
              "A BattleMek is sealed as part of its basic construction");
        assertFalse(battleMek.doomedInVacuum(), "A BattleMek operates normally in vacuum");
    }

    @Test
    void unsealedIndustrialMekIsDoomedInVacuum() throws LocationFullException {
        BipedMek industrialMek = industrialMek(Engine.NORMAL_ENGINE, false);

        assertFalse(EnvironmentalSealingRules.isSealedAgainstAtmosphere(industrialMek),
              "An IndustrialMek is not sealed unless it buys the sealing");
        assertTrue(industrialMek.doomedInVacuum(),
              "An IndustrialMek without Environmental Sealing does not survive vacuum");
    }

    @Test
    void sealedIndustrialMekSurvivesVacuumOnAFusionEngine() throws LocationFullException {
        BipedMek industrialMek = industrialMek(Engine.NORMAL_ENGINE, true);

        assertTrue(industrialMek.hasEnvironmentalSealing(), "The sealing was installed");
        assertFalse(industrialMek.doomedInVacuum(),
              "Sealing plus a fusion engine lets an IndustrialMek operate in vacuum");
    }

    @Test
    void sealedIndustrialMekOnAnIceEngineIsStillDoomedInVacuum() throws LocationFullException {
        BipedMek industrialMek = industrialMek(Engine.COMBUSTION_ENGINE, true);

        assertTrue(industrialMek.hasEnvironmentalSealing(), "The sealing was installed");
        assertTrue(industrialMek.doomedInVacuum(),
              "An internal combustion engine has no air to burn fuel with, sealed or not");
    }

    @Test
    void fuelCellAndFissionEnginesCountAsSealedOperationEngines() throws LocationFullException {
        assertTrue(EnvironmentalSealingRules.hasSealedOperationEngine(industrialMek(Engine.FUEL_CELL, true)),
              "A fuel cell runs sealed off from the outside air");
        assertTrue(EnvironmentalSealingRules.hasSealedOperationEngine(industrialMek(Engine.FISSION, true)),
              "A fission plant runs sealed off from the outside air");
        assertFalse(EnvironmentalSealingRules.hasSealedOperationEngine(industrialMek(Engine.COMBUSTION_ENGINE, true)),
              "An internal combustion engine has to breathe");
    }

    @Test
    void aSealedIceIndustrialMekMayNotBeFullySubmerged() throws LocationFullException {
        BipedMek industrialMek = industrialMek(Engine.COMBUSTION_ENGINE, true);

        assertFalse(EnvironmentalSealingRules.canOperateFullySubmerged(industrialMek),
              "Full submersion needs the sealing AND a fission, fusion or fuel cell engine (TM p.216)");
    }

    @Test
    void nullAndEngineLessUnitsAreHandled() {
        assertFalse(EnvironmentalSealingRules.hasSealedOperationEngine(null),
              "A null unit has no engine to run sealed");
        assertFalse(EnvironmentalSealingRules.isSealedAgainstAtmosphere(null),
              "A null unit is not sealed");
    }

    private static Tank combatVehicle(int engineType, boolean sealed) throws LocationFullException {
        Tank tank = new Tank();
        tank.setWeight(50.0);
        tank.setEngine(new Engine(200, engineType, Engine.TANK_ENGINE));
        if (sealed) {
            EquipmentType sealing = EquipmentType.get(CV_ENVIRONMENTAL_SEALING);
            assertNotNull(sealing, "The Combat Vehicle Environmental Sealing chassis mod must exist");
            tank.addEquipment(sealing, Tank.LOC_BODY);
        }
        return tank;
    }

    private static SupportTank supportVehicle(int engineType, boolean sealed) throws LocationFullException {
        SupportTank supportTank = new SupportTank();
        supportTank.setWeight(20.0);
        supportTank.setEngine(new Engine(100, engineType, Engine.SUPPORT_VEE_ENGINE));
        if (sealed) {
            EquipmentType sealing = EquipmentType.get(EquipmentTypeLookup.SV_ENVIRONMENTAL_SEALING_CHASSIS_MOD);
            assertNotNull(sealing, "The Support Vehicle Environmental Sealing chassis mod must exist");
            supportTank.addEquipment(sealing, Tank.LOC_BODY);
        }
        return supportTank;
    }

    @Test
    void sealedCombatVehicleSurvivesVacuumOnAFusionEngine() throws LocationFullException {
        assertFalse(combatVehicle(Engine.NORMAL_ENGINE, true).doomedInVacuum(),
              "Sealing plus a fusion engine lets a Combat Vehicle operate in vacuum");
    }

    @Test
    void unsealedCombatVehicleIsDoomedInVacuum() throws LocationFullException {
        assertTrue(combatVehicle(Engine.NORMAL_ENGINE, false).doomedInVacuum(),
              "A Combat Vehicle without the sealing chassis mod does not survive vacuum");
    }

    @Test
    void sealedCombatVehicleOnAnIceEngineIsDoomedInVacuum() throws LocationFullException {
        assertTrue(combatVehicle(Engine.COMBUSTION_ENGINE, true).doomedInVacuum(),
              "An internal combustion engine has no air to burn fuel with, sealed or not");
    }

    @Test
    void everyKindOfElectricPowerPlantCountsAsSealedOperation() throws LocationFullException {
        // "any kind of electric power plant - including external, battery, fuel cell and solar - or a fission
        // power plant" (Tactical Operations errata, rewriting the Vacuum exceptions).
        for (int electricEngine : new int[] { Engine.BATTERY, Engine.SOLAR, Engine.EXTERNAL, Engine.FUEL_CELL }) {
            assertTrue(EnvironmentalSealingRules.hasSealedOperationEngine(supportVehicle(electricEngine, true)),
                  "Engine type " + electricEngine + " is an electric power plant and needs no air");
        }
        assertFalse(EnvironmentalSealingRules.hasSealedOperationEngine(supportVehicle(Engine.STEAM, true)),
              "A steam plant still has to burn something, so it needs air");
        assertFalse(EnvironmentalSealingRules.hasSealedOperationEngine(supportVehicle(Engine.COMBUSTION_ENGINE, true)),
              "An internal combustion engine needs air");
    }

    @Test
    void sealedElectricSupportVehicleSurvivesVacuum() throws LocationFullException {
        SupportTank electricSupportVehicle = supportVehicle(Engine.BATTERY, true);

        assertTrue(electricSupportVehicle.isSupportVehicle(), "The test unit is a Support Vehicle");
        assertFalse(electricSupportVehicle.doomedInVacuum(),
              "Fission, fusion and electric Support Vehicles operate normally in vacuum (TM p.122); "
                    + "MegaMek's Electric engine is the battery");
    }

    @Test
    void unsealedElectricSupportVehicleIsStillDoomedInVacuum() throws LocationFullException {
        assertTrue(supportVehicle(Engine.BATTERY, false).doomedInVacuum(),
              "The engine alone is not enough - the Support Vehicle still has to be sealed");
    }

    @Test
    void submarineVehiclesAreSealedByConstruction() throws LocationFullException {
        Tank submarine = combatVehicle(Engine.NORMAL_ENGINE, false);
        submarine.setMovementMode(EntityMovementMode.SUBMARINE);

        assertTrue(submarine.hasEnvironmentalSealing(),
              "A submarine receives Environmental Sealing automatically and may not install it");
        assertFalse(submarine.doomedInVacuum(), "Its automatic sealing counts in vacuum too");
    }

    @Test
    void supportAerospaceUnitsAreNotSealedByConstruction() {
        FixedWingSupport fixedWingSupport = new FixedWingSupport();

        assertTrue(fixedWingSupport.isSupportVehicle(), "A fixed-wing Support Vehicle is a Support Vehicle");
        assertFalse(EnvironmentalSealingRules.isSealedAgainstAtmosphere(fixedWingSupport),
              "Support Vehicles buy the sealing even when they are aerospace units by class");
    }

    @Test
    void uncrewedCarriedObjectsAreNeverTurnedAwayByTheAir() {
        HandheldWeapon handheldWeapon = new HandheldWeapon();

        assertTrue(EnvironmentalSealingRules.isSealedAgainstAtmosphere(handheldWeapon),
              "A handheld weapon has no crew to poison, so no atmosphere rule should reach it");
    }

    @Test
    void anUnsealedIndustrialMekMayNotBeFullySubmergedEvenOnAFusionEngine() throws LocationFullException {
        BipedMek industrialMek = industrialMek(Engine.NORMAL_ENGINE, false);

        assertFalse(EnvironmentalSealingRules.canOperateFullySubmerged(industrialMek),
              "An IndustrialMek needs BOTH the sealing and the power plant to be submerged "
                    + "(TW p.52, Movement Costs Table footnote 8)");
    }

    @Test
    void aSealedVehicleNeedsANonBreathingEngineToDriveSubmerged() throws LocationFullException {
        assertFalse(EnvironmentalSealingRules.canOperateFullySubmerged(combatVehicle(Engine.COMBUSTION_ENGINE, true)),
              "An internal combustion engine will not run completely submerged, however well sealed (TM p.216)");
        assertTrue(EnvironmentalSealingRules.canOperateFullySubmerged(combatVehicle(Engine.NORMAL_ENGINE, true)),
              "Sealing plus a fusion engine puts the vehicle on the lake bed");
        assertFalse(EnvironmentalSealingRules.canOperateFullySubmerged(combatVehicle(Engine.NORMAL_ENGINE, false)),
              "The engine alone is not enough - the vehicle still has to be sealed");
    }

    @Test
    void airBreathingMovementModesAreDoomedInVacuumHoweverWellSealed() throws LocationFullException {
        for (EntityMovementMode airPushingMode : new EntityMovementMode[] {
              EntityMovementMode.HOVER, EntityMovementMode.WIGE, EntityMovementMode.VTOL }) {
            Tank sealedFlyer = combatVehicle(Engine.NORMAL_ENGINE, true);
            sealedFlyer.setMovementMode(airPushingMode);

            assertTrue(EnvironmentalSealingRules.canOperateInVacuum(sealedFlyer),
                  "The sealing rule alone says yes - it only knows about sealing and the engine");
            assertTrue(sealedFlyer.doomedInVacuum(),
                  airPushingMode + " has no air to push against, however well sealed (TO:AR p.35, footnote 31)");
        }
    }
}
