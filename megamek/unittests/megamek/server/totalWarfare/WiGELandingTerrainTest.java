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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import megamek.client.bot.princess.BasicPathRanker;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.common.GameBoardTestCase;
import megamek.common.MMRandom;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.LargeSupportTank;
import megamek.common.units.SupportTank;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * TW p.55: a WiGE vehicle may only land in a clear, paved or water hex. Landing anywhere else, by choice or because it
 * moved fewer than 5 hexes, is a crash (TW p.68), and a crash in a hex it cannot land in destroys it. Landing in the
 * hex of a grounded DropShip or Large Support Vehicle charges that unit. Issue #9103.
 */
class WiGELandingTerrainTest extends GameBoardTestCase {

    private static final double WIGE_TONNAGE = 30.0;
    private static final int ARMOR_PER_LOCATION = 40;
    private static final int WIGE_ID = 5;
    private static final int LARGE_UNIT_ID = 6;
    private static final int SECOND_WIGE_ID = 7;
    private static final Coords LANDING_HEX = new Coords(0, 2);

    static {
        // A unit starts at 0101 facing south, so two FORWARDS steps end in 0103.
        initializeBoard("ROUGH_AHEAD", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "rough:1" ""
              hex 0104 0 "" ""
              end""");
        initializeBoard("CLEAR_AHEAD", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              hex 0104 0 "" ""
              end""");
        initializeBoard("WATER_AHEAD", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "water:1" ""
              hex 0104 0 "" ""
              end""");
        initializeBoard("ROAD_THROUGH_ROUGH_AHEAD", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "rough:1;road:1" ""
              hex 0104 0 "" ""
              end""");
        initializeBoard("SAND_AHEAD", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "sand:1" ""
              hex 0104 0 "" ""
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

    /** A 30-ton WiGE support vehicle with armor deep enough that crash damage alone never kills it. */
    private SupportTank newWiGE() {
        return newWiGE(WIGE_ID);
    }

    private SupportTank newWiGE(int id) {
        SupportTank wige = new SupportTank();
        wige.setChassis("Test");
        wige.setModel("WiGE");
        wige.setMovementMode(EntityMovementMode.WIGE);
        wige.setWeight(WIGE_TONNAGE);
        wige.setOriginalWalkMP(8);
        wige.autoSetInternal();
        for (int location = 0; location < wige.locations(); location++) {
            wige.initializeArmor(ARMOR_PER_LOCATION, location);
        }
        wige.setCrew(new Crew(CrewType.SINGLE));
        wige.setOwner(getGame().getPlayer(0));
        wige.setId(id);
        return wige;
    }

    /** Plots a path for a new WiGE that starts airborne at elevation 1 in 0101, facing south. */
    private MovePath airbornePath(SupportTank wige, MoveStepType... steps) {
        return getMovePathFor(wige, 1, EntityMovementMode.WIGE, steps);
    }

    private void moveOnServer(SupportTank wige, MovePath path) {
        assertTrue(path.isMoveLegal(), "the test path should be legal to plot");
        new MovePathHandler(gameManager, wige, path, null).processMovement();
    }

    private boolean reported(int messageId) {
        for (Report report : gameManager.getMainPhaseReport()) {
            if (report.messageId == messageId) {
                return true;
            }
        }
        return false;
    }

    @Nested
    class ForcedLanding {

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("A WiGE that moves 2 hexes and ends over rough terrain crashes and is destroyed")
        void forcedLandingInRoughCrashes() {
            setBoard("ROUGH_AHEAD");
            SupportTank wige = newWiGE();
            MovePath path = airbornePath(wige, MoveStepType.FORWARDS, MoveStepType.FORWARDS);

            moveOnServer(wige, path);

            assertTrue(reported(2124), "the landing fails because of the terrain");
            assertTrue(wige.isDoomed(), "a WiGE cannot land in rough, so the crash destroys it (TW p.55, p.68)");
        }

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("A forced landing in sand crashes too: only clear, paved and water hexes are safe")
        void forcedLandingInSandCrashes() {
            setBoard("SAND_AHEAD");
            SupportTank wige = newWiGE();
            MovePath path = airbornePath(wige, MoveStepType.FORWARDS, MoveStepType.FORWARDS);

            moveOnServer(wige, path);

            assertTrue(wige.isDoomed(), "sand is not clear terrain");
        }

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("A forced landing in a clear hex is safe")
        void forcedLandingInClearIsSafe() {
            setBoard("CLEAR_AHEAD");
            SupportTank wige = newWiGE();
            int armorBefore = wige.getTotalArmor();
            MovePath path = airbornePath(wige, MoveStepType.FORWARDS, MoveStepType.FORWARDS);

            moveOnServer(wige, path);

            assertFalse(reported(2124), "no crash in a clear hex");
            assertFalse(wige.isDoomed());
            assertEquals(LANDING_HEX, wige.getPosition());
            assertEquals(0, wige.getElevation(), "the WiGE has landed");
            assertEquals(armorBefore, wige.getTotalArmor(), "landing costs nothing");
        }

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("A forced landing on water is safe: WiGEs treat water as clear terrain")
        void forcedLandingOnWaterIsSafe() {
            setBoard("WATER_AHEAD");
            SupportTank wige = newWiGE();
            MovePath path = airbornePath(wige, MoveStepType.FORWARDS, MoveStepType.FORWARDS);

            moveOnServer(wige, path);

            assertFalse(wige.isDoomed(), "WiGEs float on water (TW p.55, errata v12.0)");
            assertEquals(0, wige.getElevation());
        }

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("A road through rough terrain is paved, so a WiGE may land on it")
        void forcedLandingOnARoadIsSafe() {
            setBoard("ROAD_THROUGH_ROUGH_AHEAD");
            SupportTank wige = newWiGE();
            MovePath path = airbornePath(wige, MoveStepType.FORWARDS, MoveStepType.FORWARDS);

            moveOnServer(wige, path);

            assertFalse(wige.isDoomed(), "a road counts as paved");
            assertEquals(0, wige.getElevation());
        }
    }

    @Nested
    class VoluntaryLanding {

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("A WiGE may plot a landing in rough terrain, but it crashes and is destroyed")
        void choosingToLandInRoughCrashes() {
            setBoard("ROUGH_AHEAD");
            SupportTank wige = newWiGE();
            MovePath path = airbornePath(wige, MoveStepType.FORWARDS, MoveStepType.FORWARDS, MoveStepType.DOWN);

            moveOnServer(wige, path);

            assertTrue(reported(2124), "the landing fails because of the terrain");
            assertTrue(wige.isDoomed(), "TW p.55: a WiGE that attempts to land in rough automatically crashes");
            assertEquals(LANDING_HEX, wige.getPosition(), "it crashes where it tried to land");
        }

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("Choosing to land in a clear hex is safe")
        void choosingToLandInClearIsSafe() {
            setBoard("CLEAR_AHEAD");
            SupportTank wige = newWiGE();
            MovePath path = airbornePath(wige, MoveStepType.FORWARDS, MoveStepType.FORWARDS, MoveStepType.DOWN);

            moveOnServer(wige, path);

            assertFalse(reported(2124));
            assertFalse(wige.isDoomed());
            assertEquals(LANDING_HEX, wige.getPosition());
            assertEquals(0, wige.getElevation(), "the WiGE has landed");
        }
    }

    @Nested
    class LargeUnitInTheLandingHex {

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("A WiGE landing in the hex of a grounded Large Support Vehicle charges it")
        void landingOnALargeSupportVehicleChargesIt() {
            setBoard("CLEAR_AHEAD");
            SupportTank wige = newWiGE();
            getGame().addEntity(wige);
            wige.setPosition(LANDING_HEX);
            wige.setFacing(3);
            wige.setElevation(2);
            wige.delta_distance = 3;
            LargeSupportTank largeVehicle = new LargeSupportTank();
            largeVehicle.setChassis("Test");
            largeVehicle.setModel("Large Support Vehicle");
            largeVehicle.setMovementMode(EntityMovementMode.WHEELED);
            largeVehicle.setWeight(200.0);
            largeVehicle.autoSetInternal();
            for (int location = 0; location < largeVehicle.locations(); location++) {
                largeVehicle.initializeArmor(ARMOR_PER_LOCATION, location);
            }
            largeVehicle.setCrew(new Crew(CrewType.CREW));
            largeVehicle.setOwner(getGame().getPlayer(0));
            largeVehicle.setId(LARGE_UNIT_ID);
            getGame().addEntity(largeVehicle);
            largeVehicle.setPosition(LANDING_HEX);
            largeVehicle.setElevation(0);
            largeVehicle.setDeployed(true);
            largeVehicle.setDone(true);
            int largeVehicleArmorBefore = largeVehicle.getTotalArmor();

            new AirborneVehicleCrashHandler(gameManager).resolveWiGELanding(wige, 2, 3);

            assertTrue(reported(2127), "the WiGE comes down on the Large Support Vehicle and charges it");
            assertTrue(largeVehicle.getTotalArmor() < largeVehicleArmorBefore, "the charge damages it");
        }
    }

    @Nested
    class PlottingAndBots {

        @Test
        @DisplayName("The client can tell that a short move ending over rough terrain will crash")
        void pathKnowsAForcedLandingInRoughCrashes() {
            setBoard("ROUGH_AHEAD");
            MovePath path = airbornePath(newWiGE(), MoveStepType.FORWARDS, MoveStepType.FORWARDS);

            assertTrue(path.landsWiGEVehicleWhereItCrashes());
        }

        @Test
        @DisplayName("The client can tell that a chosen landing in rough terrain will crash")
        void pathKnowsAChosenLandingInRoughCrashes() {
            setBoard("ROUGH_AHEAD");
            MovePath path = airbornePath(newWiGE(), MoveStepType.FORWARDS, MoveStepType.FORWARDS,
                  MoveStepType.DOWN);

            assertTrue(path.landsWiGEVehicleWhereItCrashes());
        }

        @Test
        @DisplayName("Landing in clear or water hexes, or flying 5 hexes, is not a crash")
        void safeLandingsAreNotCrashes() {
            setBoard("CLEAR_AHEAD");
            assertFalse(airbornePath(newWiGE(), MoveStepType.FORWARDS, MoveStepType.FORWARDS)
                  .landsWiGEVehicleWhereItCrashes(), "clear hex");

            setBoard("WATER_AHEAD");
            assertFalse(airbornePath(newWiGE(SECOND_WIGE_ID), MoveStepType.FORWARDS, MoveStepType.FORWARDS)
                  .landsWiGEVehicleWhereItCrashes(), "water hex");
        }

        @Test
        @DisplayName("Princess and CASPAR price a crash landing as losing the unit")
        void botsTreatACrashLandingAsLosingTheUnit() {
            Princess princess = mock(Princess.class);
            when(princess.getBehaviorSettings()).thenReturn(new BehaviorSettings());
            BasicPathRanker ranker = new BasicPathRanker(princess);

            setBoard("ROUGH_AHEAD");
            SupportTank crashingWiGE = newWiGE();
            MovePath crashingPath = airbornePath(crashingWiGE, MoveStepType.FORWARDS, MoveStepType.FORWARDS);
            assertEquals(1000.0, ranker.checkPathForHazards(crashingPath, crashingWiGE, getGame()), 0.001,
                  "a crash landing destroys the WiGE");

            setBoard("CLEAR_AHEAD");
            SupportTank landingWiGE = newWiGE(SECOND_WIGE_ID);
            MovePath landingPath = airbornePath(landingWiGE, MoveStepType.FORWARDS, MoveStepType.FORWARDS);
            assertEquals(0.0, ranker.checkPathForHazards(landingPath, landingWiGE, getGame()), 0.001,
                  "a landing in a clear hex is safe");
        }
    }
}
