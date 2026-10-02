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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;

import megamek.common.GameBoardTestCase;
import megamek.common.Hex;
import megamek.common.MMRandom;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.IBuilding;
import megamek.common.units.SupportTank;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Airborne VTOL and WiGE crashes and sideslips (issue #9102, TW pp.67-68), tested through the {@link TWGameManager}
 * and {@link MovePathHandler} entry points other code uses, plus the rule calculations in
 * {@link AirborneVehicleCrashHandler}.
 */
class AirborneVehicleCrashHandlerTest extends GameBoardTestCase {

    private static final double WIGE_TONNAGE = 30.0;
    private static final int ARMOR_PER_LOCATION = 40;
    private static final int SOUTH = 3;

    static {
        initializeBoard("CLEAR", """
              size 1 1
              hex 0101 0 "" ""
              end""");

        initializeBoard("DEEP_WATER", """
              size 1 1
              hex 0101 0 "water:2" ""
              end""");

        // Open ground for a full move: south 4 hexes, turn, then fail the sideslip roll
        initializeBoard("OPEN_GROUND", """
              size 3 10
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              hex 0106 0 "" ""
              hex 0107 0 "" ""
              hex 0108 0 "" ""
              hex 0109 0 "" ""
              hex 0110 0 "" ""
              hex 0201 0 "" ""
              hex 0202 0 "" ""
              hex 0203 0 "" ""
              hex 0204 0 "" ""
              hex 0205 0 "" ""
              hex 0206 0 "" ""
              hex 0207 0 "" ""
              hex 0208 0 "" ""
              hex 0209 0 "" ""
              hex 0210 0 "" ""
              hex 0301 0 "" ""
              hex 0302 0 "" ""
              hex 0303 0 "" ""
              hex 0304 0 "" ""
              hex 0305 0 "" ""
              hex 0306 0 "" ""
              hex 0307 0 "" ""
              hex 0308 0 "" ""
              hex 0309 0 "" ""
              hex 0310 0 "" ""
              end""");

        initializeBoard("WOODS_AHEAD", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "woods:1;foliage_elev:2" ""
              end""");

        initializeBoard("BUILDING_AHEAD", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "bldg_elev:1;building:2:0;bldg_cf:40" ""
              end""");

        // A rough level 3 hill: a WiGE cannot land there after crashing into it
        initializeBoard("ROUGH_HILL_AHEAD", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 3 "rough:1" ""
              end""");

        // A level 3 ridge with a drop to level 0 beyond it
        initializeBoard("DROP_AHEAD", """
              size 1 2
              hex 0101 3 "" ""
              hex 0102 0 "" ""
              end""");

        initializeBoard("OPEN_ROW", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              end""");

        // Clear, clear, then a level 3 hill the sideslipping WiGE crashes into
        initializeBoard("HILL_AHEAD", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 3 "" ""
              end""");
    }

    private TWGameManager gameManager;

    /** Dice that always roll the middle: every d6 is a 4, so 2D6 is 8 and no critical or special result fires. */
    private static final class MiddleDice extends MMRandom {
        @Override
        public int randomInt(int maxValue) {
            return maxValue / 2;
        }

        @Override
        public float randomFloat() {
            return 0.5f;
        }
    }

    @AfterEach
    void restoreDice() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    @BeforeEach
    void setUp() throws IOException {
        Compute.setRNG(new MiddleDice());
        gameManager = new TWGameManager();
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        if (getGame().getPlayer(0) == null) {
            getGame().addPlayer(0, new Player(0, "Test"));
        }
    }

    /** A 30-ton combat WiGE with armor deep enough that crashes never kill it. Not yet in the game. */
    private SupportTank newWiGE(int walkMP) {
        SupportTank wige = new SupportTank();
        wige.setChassis("Test");
        wige.setModel("WiGE");
        wige.setMovementMode(EntityMovementMode.WIGE);
        wige.setWeight(WIGE_TONNAGE);
        wige.setOriginalWalkMP(walkMP);
        wige.autoSetInternal();
        for (int location = 0; location < wige.locations(); location++) {
            wige.initializeArmor(ARMOR_PER_LOCATION, location);
        }
        wige.setCrew(new Crew(CrewType.SINGLE));
        wige.setOwner(getGame().getPlayer(0));
        return wige;
    }

    /** The WiGE above, airborne at elevation 1 in hex 0101 facing south. */
    private SupportTank airborneWiGE() {
        SupportTank wige = newWiGE(8);
        wige.setId(5);
        getGame().addEntity(wige);
        wige.setPosition(new Coords(0, 0));
        wige.setFacing(SOUTH);
        wige.setElevation(1);
        return wige;
    }

    @Test
    void crashOverClearGroundLandsAndTakesFallingDamage() {
        setBoard("CLEAR");
        SupportTank wige = airborneWiGE();
        int armorBefore = wige.getTotalArmor();

        gameManager.crashVTOLorWiGE(wige);

        assertEquals(0, wige.getElevation(), "the crashed WiGE is on the ground");
        assertFalse(wige.isDoomed());
        // round(30 / 10) x (1 level fallen + 1)
        assertEquals(6, armorBefore - wige.getTotalArmor());
    }

    @Test
    void crashIntoDeepWaterLandsTheWiGEOnTheSurface() {
        setBoard("DEEP_WATER");
        SupportTank wige = airborneWiGE();

        gameManager.crashVTOLorWiGE(wige);

        assertFalse(wige.isDoomed(), "a WiGE floats and treats water as clear terrain (TW p.55)");
        assertEquals(0, wige.getElevation());
    }

    @Test
    void sideslipCrashDamageCountsEveryHexMovedThisTurn() {
        setBoard("HILL_AHEAD");
        SupportTank wige = airborneWiGE();
        // The WiGE entered 4 hexes before the sideslip began
        wige.delta_distance = 4;
        int armorBefore = wige.getTotalArmor();
        MoveStep step = mock(MoveStep.class);
        when(step.getFacing()).thenReturn(SOUTH);

        // Sideslip 2 hexes south: 0102 is clear, 0103 is a level 3 hill
        gameManager.processSkid(wige, new Coords(0, 0), 1, SOUTH, 2, step, EntityMovementType.MOVE_VTOL_RUN);

        assertEquals(0, wige.getElevation(), "the WiGE crashed and is on the ground");
        // 4 hexes before + 1 sideslipped + the crash hex = 6 hexes; 6 x 30 / 10 = 18
        assertEquals(18, armorBefore - wige.getTotalArmor());
    }

    @Test
    void failedSideslipFollowsTheTwCapAndCountsForTheTargetMovementModifier() throws Exception {
        setBoard("OPEN_GROUND");
        TWGameManager spiedManager = spy(gameManager);
        ServerFactory.createServer(spiedManager);
        // Walk 4, flank 6: four hexes south, a facing change, then the step that triggers the sideslip roll
        SupportTank wige = newWiGE(4);
        MovePath path = getMovePathFor(wige, 1, EntityMovementMode.WIGE,
              MoveStepType.FORWARDS, MoveStepType.FORWARDS, MoveStepType.FORWARDS, MoveStepType.FORWARDS,
              MoveStepType.TURN_LEFT, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(), "the test path should be legal");
        // Fail the sideslip roll by 4
        doReturn(4).when(spiedManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(), any(Coords.class),
              any(Coords.class), any(PilotingRollData.class), anyBoolean());

        new MovePathHandler(spiedManager, wige, path, null).processMovement();

        // 4 hexes entered before the slip, so the cap is 3 (TW p.67): from 0105 it slips south to 0108
        assertEquals(new Coords(0, 7), wige.getPosition(), "the WiGE sideslips 3 hexes south");
        assertEquals(7, wige.delta_distance, "the 3 sideslipped hexes count with the 4 hexes moved");
    }

    private MoveStep southFacingStep() {
        MoveStep step = mock(MoveStep.class);
        when(step.getFacing()).thenReturn(SOUTH);
        return step;
    }

    private boolean reported(int messageId) {
        for (Report report : gameManager.getMainPhaseReport()) {
            if (report.messageId == messageId) {
                return true;
            }
        }
        return false;
    }

    @Test
    void wigeSideslippingIntoWoodsCrashesAndIsDestroyed() {
        setBoard("WOODS_AHEAD");
        SupportTank wige = airborneWiGE();

        gameManager.processSkid(wige, new Coords(0, 0), 1, SOUTH, 1, southFacingStep(),
              EntityMovementType.MOVE_VTOL_RUN);

        assertTrue(reported(2053), "the crash report names the trees");
        assertTrue(wige.isDoomed(), "a WiGE cannot land in woods, so the crash destroys it (TW p.68)");
    }

    @Test
    void sideslipIntoABuildingChargesItAndCrashesBeforeIt() {
        setBoard("BUILDING_AHEAD");
        SupportTank wige = airborneWiGE();
        wige.delta_distance = 4;
        Coords buildingHex = new Coords(0, 1);
        IBuilding building = getGame().getBoard().getBuildingAt(buildingHex);
        int buildingCfBefore = building.getCurrentCF(buildingHex);

        gameManager.processSkid(wige, new Coords(0, 0), 1, SOUTH, 1, southFacingStep(),
              EntityMovementType.MOVE_VTOL_RUN);

        assertTrue(reported(2051), "the WiGE crashes into the building");
        assertTrue(building.getCurrentCF(buildingHex) < buildingCfBefore, "the building takes the charge");
        assertEquals(new Coords(0, 0), wige.getPosition(), "the WiGE crashes in the hex before the building");
        assertEquals(0, wige.getElevation(), "the WiGE is on the ground after the crash");
    }

    @Test
    void sideslipIntoInfantryDropsAndCrashes() {
        setBoard("OPEN_ROW");
        SupportTank wige = airborneWiGE();
        ConvInfantry infantry = new ConvInfantry();
        infantry.setOwner(getGame().getPlayer(0));
        infantry.setId(6);
        infantry.setPosition(new Coords(0, 1));
        infantry.setDeployed(true);
        getGame().addEntity(infantry);

        gameManager.processSkid(wige, new Coords(0, 0), 1, SOUTH, 2, southFacingStep(),
              EntityMovementType.MOVE_VTOL_RUN);

        assertTrue(reported(2052), "the WiGE drops onto the infantry and crashes");
        assertEquals(new Coords(0, 0), wige.getPosition(), "the WiGE crashes in the hex before the infantry");
        assertEquals(0, wige.getElevation());
    }

    @Test
    void sideslipChargesAGroundVehicleOneLevelTall() {
        setBoard("OPEN_ROW");
        SupportTank wige = airborneWiGE();
        SupportTank groundVehicle = newWiGE(4);
        groundVehicle.setMovementMode(EntityMovementMode.TRACKED);
        groundVehicle.setId(6);
        groundVehicle.setPosition(new Coords(0, 1));
        groundVehicle.setElevation(0);
        groundVehicle.setDeployed(true);
        // A unit that has already moved cannot try to dodge the sliding WiGE
        groundVehicle.setDone(true);
        getGame().addEntity(groundVehicle);

        gameManager.processSkid(wige, new Coords(0, 0), 1, SOUTH, 2, southFacingStep(),
              EntityMovementType.MOVE_VTOL_RUN);

        // TW counts the vehicle as 1 level tall, which reaches a WiGE at elevation 1 (TW p.68, p.99)
        assertTrue(reported(2050), "the WiGE runs into the vehicle instead of passing over it");
    }

    @Test
    void collisionLevelsUseTwUnitHeights() {
        Hex levelTwo = new Hex(2);
        SupportTank vehicle = newWiGE(4);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setElevation(0);
        assertEquals(3, AirborneVehicleCrashHandler.sideslipCollisionLevel(levelTwo, vehicle),
              "a vehicle on a level 2 hex reaches level 3");

        Hex building = new Hex(0, "bldg_elev:2;building:2:0;bldg_cf:40", "", new Coords(0, 0));
        assertTrue(AirborneVehicleCrashHandler.isBuildingInSideslipPath(building, 2), "roof level with the unit");
        assertFalse(AirborneVehicleCrashHandler.isBuildingInSideslipPath(building, 3), "the unit flies over");
    }

    @Test
    void wigeCannotLandAfterCrashingIntoRoughTerrain() {
        setBoard("ROUGH_HILL_AHEAD");
        SupportTank wige = airborneWiGE();

        gameManager.processSkid(wige, new Coords(0, 0), 1, SOUTH, 2, southFacingStep(),
              EntityMovementType.MOVE_VTOL_RUN);

        assertTrue(wige.isDoomed(), "a WiGE can only land in clear, paved or water hexes (TW p.55, p.68)");
    }

    @Test
    void crashedVehicleCannotAttackThatTurn() {
        setBoard("HILL_AHEAD");
        SupportTank wige = airborneWiGE();

        gameManager.processSkid(wige, new Coords(0, 0), 1, SOUTH, 2, southFacingStep(),
              EntityMovementType.MOVE_VTOL_RUN);

        assertFalse(wige.isDoomed(), "the WiGE lands on the clear hilltop");
        assertTrue(wige.hasCrashedThisTurn(), "a vehicle that crashes after a sideslip may not attack (TW p.68)");
        wige.newRound(2);
        assertFalse(wige.hasCrashedThisTurn(), "the next turn it may attack again");
    }

    @Test
    void crashOntoAGroundUnitIsAnAccidentalFallFromAbove() {
        setBoard("BUILDING_AHEAD");
        SupportTank wige = airborneWiGE();
        SupportTank groundVehicle = newWiGE(4);
        groundVehicle.setMovementMode(EntityMovementMode.TRACKED);
        groundVehicle.setId(6);
        groundVehicle.setPosition(new Coords(0, 0));
        groundVehicle.setElevation(0);
        groundVehicle.setDeployed(true);
        getGame().addEntity(groundVehicle);
        int armorBefore = groundVehicle.getTotalArmor();

        // The WiGE slips into the building and crashes back in 0101, on top of the ground vehicle
        gameManager.processSkid(wige, new Coords(0, 0), 1, SOUTH, 1, southFacingStep(),
              EntityMovementType.MOVE_VTOL_RUN);

        assertTrue(reported(2054), "the WiGE crashes down onto the ground vehicle");
        // A vehicle is hit automatically; 30 tons / 10 x 1 level fallen
        assertEquals(3, armorBefore - groundVehicle.getTotalArmor());
    }

    @Test
    void wigeWithMpLeftAvoidsFallingOffADrop() {
        setBoard("DROP_AHEAD");
        SupportTank wige = airborneWiGE();
        // Following the terrain (Keep Elevation off), as a WiGE starts each turn
        wige.setClimbMode(false);
        wige.mpUsed = 0;

        gameManager.processSkid(wige, new Coords(0, 0), 1, SOUTH, 1, southFacingStep(),
              EntityMovementType.MOVE_VTOL_RUN);

        assertEquals(new Coords(0, 1), wige.getPosition(), "the WiGE keeps sliding over the drop");
        assertTrue(wige.getElevation() > 0, "it stays airborne instead of falling (TW p.68)");
    }

    @Test
    void wigeWithoutMpLeftFallsOffADrop() {
        setBoard("DROP_AHEAD");
        SupportTank wige = airborneWiGE();
        wige.setClimbMode(false);
        wige.mpUsed = wige.getRunMP() - 1;

        gameManager.processSkid(wige, new Coords(0, 0), 1, SOUTH, 1, southFacingStep(),
              EntityMovementType.MOVE_VTOL_RUN);

        assertEquals(0, wige.getElevation(), "with less than 2 MP left the WiGE falls (TW p.68, p.55)");
    }

    @Test
    void crashSiteLandingFollowsEachVehicleType() {
        SupportTank wige = newWiGE(4);
        SupportTank vtol = newWiGE(4);
        vtol.setMovementMode(EntityMovementMode.VTOL);
        Hex clear = new Hex(0);
        Hex water = new Hex(0, "water:2", "", new Coords(0, 0));
        Hex rough = new Hex(0, "rough:1", "", new Coords(0, 0));

        assertTrue(AirborneVehicleCrashHandler.canLandAfterSideslipCrash(wige, clear));
        assertTrue(AirborneVehicleCrashHandler.canLandAfterSideslipCrash(wige, water), "WiGEs treat water as clear");
        assertFalse(AirborneVehicleCrashHandler.canLandAfterSideslipCrash(wige, rough));
        assertTrue(AirborneVehicleCrashHandler.canLandAfterSideslipCrash(vtol, clear));
        assertFalse(AirborneVehicleCrashHandler.canLandAfterSideslipCrash(vtol, water), "a VTOL cannot land on water");
    }

    @Test
    void wigeNeedsTwoMpToAvoidAFall() {
        SupportTank wige = newWiGE(8);
        wige.mpUsed = wige.getRunMP() - 2;
        assertTrue(AirborneVehicleCrashHandler.canAvoidSideslipFall(wige));
        wige.mpUsed = wige.getRunMP() - 1;
        assertFalse(AirborneVehicleCrashHandler.canAvoidSideslipFall(wige));
    }

    @Test
    void sideslipDistanceIsCappedAtOneLessThanTheHexesEntered() {
        assertEquals(2, AirborneVehicleCrashHandler.sideslipDistanceCap(4, 3), "TW p.67 example");
        assertEquals(1, AirborneVehicleCrashHandler.sideslipDistanceCap(1, 5), "a small margin of failure");
        assertEquals(0, AirborneVehicleCrashHandler.sideslipDistanceCap(3, 1), "one hex entered: no sideslip");
        assertEquals(0, AirborneVehicleCrashHandler.sideslipDistanceCap(3, 0), "never negative");
    }

    @Test
    void sideslipCrashDamageIsHexesTimesTonnageOverTenRoundedUp() {
        assertEquals(15, AirborneVehicleCrashHandler.sideslipCrashDamage(30.0, 5));
        assertEquals(8, AirborneVehicleCrashHandler.sideslipCrashDamage(25.0, 3), "7.5 rounds up to 8");
        assertEquals(0, AirborneVehicleCrashHandler.sideslipCrashDamage(30.0, 0));
    }
}
