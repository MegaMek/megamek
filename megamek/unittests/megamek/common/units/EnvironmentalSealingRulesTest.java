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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Messages;
import megamek.common.equipment.Engine;
import megamek.common.game.Game;
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
        // A real vehicle's body carries no internal structure and reports zero armour even when undamaged; a bare
        // Tank does not, so say so explicitly or the fixture is not the thing being modelled.
        tank.initializeInternal(-1, Tank.LOC_BODY);
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
    void anUnpoweredTrailerIsToldItHasNoEngineRatherThanABreathingOne() throws LocationFullException {
        // The two canon sealed trailers, HMRV DeConAid and Galaport Ground, carry Environmental Sealing and no
        // engine at all. Naming an engine they do not have sends their owner looking for the wrong thing.
        SupportTank trailer = supportVehicle(Engine.NONE, true);

        String reason = EnvironmentalSealingRules.whyCannotOperateInVacuum(trailer);

        assertNotNull(reason, "A sealed trailer with no engine still cannot operate in vacuum");
        assertEquals(Messages.getString("EnvironmentalSealing.Vacuum.NoEngine"), reason,
              "It should be told it has no engine, not that its engine needs air");
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

    @Test
    void eachCauseOfVacuumDeathGivesItsOwnReason() throws LocationFullException {
        // The text itself is translatable and not worth pinning; what matters is that a player asking "why?" gets a
        // different answer for each of the three quite different causes, and none at all when the unit is fine.
        String noSealing = EnvironmentalSealingRules.whyCannotOperateInVacuum(
              combatVehicle(Engine.NORMAL_ENGINE, false));
        String engineNeedsAir = EnvironmentalSealingRules.whyCannotOperateInVacuum(
              combatVehicle(Engine.COMBUSTION_ENGINE, true));
        Tank sealedHovercraft = combatVehicle(Engine.NORMAL_ENGINE, true);
        sealedHovercraft.setMovementMode(EntityMovementMode.HOVER);
        String nothingToPushAgainst = EnvironmentalSealingRules.whyCannotOperateInVacuum(sealedHovercraft);

        assertNotNull(noSealing, "an unsealed vehicle should be told it has no sealing");
        assertNotNull(engineNeedsAir, "a sealed vehicle on an ICE should be told about its engine");
        assertNotNull(nothingToPushAgainst, "a hovercraft should be told it has nothing to push against");
        assertNotEquals(noSealing, engineNeedsAir, "missing sealing and a breathing engine are different problems");
        assertNotEquals(engineNeedsAir, nothingToPushAgainst, "the engine and the movement type are different too");
        assertNotEquals(noSealing, nothingToPushAgainst, "so are the sealing and the movement type");

        assertNull(EnvironmentalSealingRules.whyCannotOperateInVacuum(combatVehicle(Engine.NORMAL_ENGINE, true)),
              "a sealed, fusion-powered tracked vehicle has no reason to give");
    }

    @Test
    void drowningWaitsOneFullRoundBeforeItKills() throws LocationFullException {
        // "destroyed if they remain in a Depth 2 or greater water hex ... in the End Phase of the turn immediately
        // following the turn in which they entered it" (TW p.52, Movement Costs Table footnote 8). The delay is
        // carried by newRound copying one flag into the other, so a playtester has to sit through a whole extra
        // round before the unit dies - which reads as "it survived" if you only look at the turn it waded in.
        BipedMek industrialMek = industrialMek(Engine.NORMAL_ENGINE, false);
        industrialMek.setGame(new Game());
        industrialMek.setJustMovedIntoIndustrialKillingWater(true);

        assertFalse(industrialMek.shouldDieAtEndOfTurnBecauseOfWater(),
              "it survives the End Phase of the turn it entered the water");

        industrialMek.newRound(1);

        assertTrue(industrialMek.shouldDieAtEndOfTurnBecauseOfWater(),
              "it dies in the End Phase of the following turn");
    }

    @Test
    void aMekThatNeverEnteredKillingWaterIsNeverMarkedToDrown() throws LocationFullException {
        BipedMek industrialMek = industrialMek(Engine.NORMAL_ENGINE, true);
        industrialMek.setGame(new Game());

        industrialMek.newRound(1);

        assertFalse(industrialMek.shouldDieAtEndOfTurnBecauseOfWater(),
              "a sealed IndustrialMek on a fusion engine is never marked to drown");
    }

    @Test
    void aProneMekIsSubmergedInShallowerWaterThanAStandingOne() {
        // "a Depth 2 or greater water hex (or prone in a Depth 1 water hex)" (TW p.52). The Depth 2 clause has no
        // stance qualifier, so lying down in deep water counts too - it is the shallow case the parenthetical adds.
        assertFalse(EnvironmentalSealingRules.isMekCompletelySubmerged(false, 1),
              "a standing Mek wades through Depth 1 with its head out");
        assertTrue(EnvironmentalSealingRules.isMekCompletelySubmerged(true, 1),
              "lying down in Depth 1 puts it under");
        assertTrue(EnvironmentalSealingRules.isMekCompletelySubmerged(false, 2),
              "Depth 2 covers a standing Mek");
        assertTrue(EnvironmentalSealingRules.isMekCompletelySubmerged(true, 2),
              "a prone Mek in Depth 2 is submerged as well - it does not escape the rule by lying down");
        assertTrue(EnvironmentalSealingRules.isMekCompletelySubmerged(true, 5),
              "and deeper still");
        assertFalse(EnvironmentalSealingRules.isMekCompletelySubmerged(true, 0),
              "Depth 0 covers nobody");
    }

    @Test
    void aHealthyVehicleIsNotWarnedAboutWater() throws LocationFullException {
        Tank tank = combatVehicle(Engine.NORMAL_ENGINE, true);
        for (int location = 0; location < tank.locations(); location++) {
            tank.initializeArmor(10, location);
        }

        assertFalse(EnvironmentalSealingRules.wouldBeDestroyedByWaterBreach(tank),
              "a vehicle with its armour intact has nothing to fear from the water");
    }

    @Test
    void aVehicleWithAStrippedSideIsDestroyedByWater() throws LocationFullException {
        Tank tank = combatVehicle(Engine.NORMAL_ENGINE, true);
        for (int location = 0; location < tank.locations(); location++) {
            tank.initializeArmor(10, location);
        }
        tank.initializeArmor(0, Tank.LOC_RIGHT);

        assertTrue(EnvironmentalSealingRules.wouldBeDestroyedByWaterBreach(tank),
              "a location with no armour left breaches on contact with water, and that destroys a vehicle");
    }

    @Test
    void aVehicleBodyWithNoArmourIsNotMistakenForDamage() throws LocationFullException {
        // A vehicle body reports zero armour on a perfectly healthy unit, and has no internal structure. The breach
        // code skips it for that reason, and so must the warning - otherwise every vehicle is warned about water.
        Tank tank = combatVehicle(Engine.NORMAL_ENGINE, true);
        for (int location = 0; location < tank.locations(); location++) {
            tank.initializeArmor(10, location);
        }
        tank.initializeArmor(0, Tank.LOC_BODY);

        assertFalse(EnvironmentalSealingRules.wouldBeDestroyedByWaterBreach(tank),
              "the body is not a real location and never breaches");
    }

    @Test
    void aMekIsOnlyDoomedByWaterWhenTheFatalLocationsAreBare() throws LocationFullException {
        BipedMek mek = industrialMek(Engine.NORMAL_ENGINE, true);
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(10, location);
        }

        assertFalse(EnvironmentalSealingRules.wouldBeDestroyedByWaterBreach(mek), "intact, so no warning");

        mek.initializeArmor(0, Mek.LOC_LEFT_ARM);
        assertFalse(EnvironmentalSealingRules.wouldBeDestroyedByWaterBreach(mek),
              "a flooded arm is bad, but it does not sink the Mek");

        mek.initializeArmor(0, Mek.LOC_HEAD);
        assertTrue(EnvironmentalSealingRules.wouldBeDestroyedByWaterBreach(mek),
              "a breached head destroys the Mek and kills the MekWarrior");
    }
}
