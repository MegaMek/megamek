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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.actions.ScanAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.BankedScan;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.equipment.ObjectiveScoringScheme;
import megamek.common.equipment.ObjectiveScoringScheme.ScanPayout;
import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.server.victory.VictoryPointTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * The End Phase scan pass: a declared scan is checked, what it found is banked on the unit, and the readings are
 * paid or lost when the unit leaves. Rolls and line of sight are answered by the test; the game's default ruleset,
 * Core Rules, supplies the check: Piloting 5 + 3 = 8 for the crews below.
 */
class ObjectiveScanHandlerTest {

    private static final Coords SCANNER_HEX = new Coords(3, 3);
    private static final Coords POINT_HEX = new Coords(5, 3);
    private static final Coords EMPTY_HEX = new Coords(4, 4);
    private static final int POINT_VALUE = 2;

    /** The handler with the dice and the line of sight decided by the test. */
    private static final class HandlerUnderTest extends ObjectiveScanHandler {
        private int nextRoll = 8;
        private boolean lineOfSight = true;

        HandlerUnderTest(TWGameManager gameManager) {
            super(gameManager);
        }

        @Override
        Roll rollScanCheck() {
            return new FixedRoll(nextRoll);
        }

        @Override
        boolean hasLineOfSight(Entity scanner, Targetable target) {
            return lineOfSight;
        }
    }

    /** Two dice that always show the same total. */
    private static final class FixedRoll extends Roll {
        private final int value;

        FixedRoll(int value) {
            super(2, 1);
            this.value = value;
        }

        @Override
        public int getIntValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public String getReport() {
            return String.valueOf(value);
        }

        @Override
        public int[] getIntValues() {
            return new int[] { value };
        }
    }

    private Game game;
    private TWGameManager gameManager;
    private HandlerUnderTest handler;
    private GameOptions gameOptions;
    private Player alice;
    private Player bob;
    private int nextEntityId = 1;

    @BeforeEach
    void setUp() {
        game = mock(Game.class);
        gameManager = mock(TWGameManager.class);
        when(gameManager.getGame()).thenReturn(game);
        handler = new HandlerUnderTest(gameManager);

        alice = new Player(0, "Alice");
        alice.setTeam(1);
        bob = new Player(1, "Bob");
        bob.setTeam(2);
        gameOptions = new GameOptions();
        gameOptions.getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(true);
        when(game.getOptions()).thenReturn(gameOptions);
        when(game.getPlayer(0)).thenReturn(alice);
        when(game.getPlayer(1)).thenReturn(bob);
        when(game.getVictoryContext()).thenReturn(new HashMap<>());
        when(game.getCurrentRound()).thenReturn(3);
        when(game.getPlanetaryConditions()).thenReturn(new PlanetaryConditions());
        when(game.getRetreatedEntities()).thenReturn(Collections.emptyEnumeration());
        when(game.getGraveyardEntities()).thenReturn(Collections.emptyEnumeration());
        when(game.getEntitiesVector()).thenReturn(List.of());
    }

    // --- the scan itself ---

    @Test
    void testAScanOfTheSidesOwnScanPointBanksAReadingOnTheUnit() {
        ObjectiveMarker scanPoint = scanPointOf(alice, ScanPayout.ON_EXIT);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        orderScan(scout, POINT_HEX);

        handler.resolveScans();

        assertEquals(1, scout.getBankedScans().size(), "one reading, of the point");
        assertTrue(scout.getBankedScans().getFirst().isObjectiveReading());
        assertNull(scout.getPendingScan(), "the order is spent");
        assertFalse(scanPoint.getScoringScheme().isDecided(), "the point pays only once the reading is home");
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_SCAN_SUCCESS));
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_READING_BANKED), "nothing to reveal: the plain line");
        verify(gameManager).entityUpdate(scout.getId());
        assertNull(VictoryPointTracker.findTracker(game.getVictoryContext()), "nothing scored yet");
    }

    @Test
    void testASuccessfulScanTellsTheScanningPlayerWhatItReveals() {
        ObjectiveMarker scanPoint = scanPointOf(alice, ScanPayout.ON_EXIT);
        scanPoint.setScanRevealsNote("Fresh tracks lead north.");
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        orderScan(scout, POINT_HEX);

        handler.resolveScans();

        ArgumentCaptor<Report> reports = ArgumentCaptor.forClass(Report.class);
        verify(gameManager, atLeastOnce()).addReport(reports.capture());
        Report reveal = reports.getAllValues().stream()
              .filter(report -> report.messageId == ObjectiveScanHandler.REPORT_SCAN_REVEALS)
              .findFirst()
              .orElseThrow();
        assertEquals(Report.PLAYER, reveal.type, "the note is the scanning player's alone");
        assertEquals(alice.getId(), reveal.player);
        assertFalse(reportIds().contains(ObjectiveScanHandler.REPORT_READING_BANKED),
              "the note replaces the plain banked line");
    }

    @Test
    void testAScanOfAnEmptyHexFindsNothingOfInterest() {
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        orderScan(scout, EMPTY_HEX);

        handler.resolveScans();

        assertTrue(scout.getBankedScans().isEmpty());
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_NOTHING_OF_INTEREST));
    }

    @Test
    void testAnotherSidesScanPointLooksLikeNothingOfInterest() {
        scanPointOf(bob, ScanPayout.ON_EXIT);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        orderScan(scout, POINT_HEX);

        handler.resolveScans();

        assertTrue(scout.getBankedScans().isEmpty(), "not this side's point to read");
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_NOTHING_OF_INTEREST));
    }

    @Test
    void testAFailedCheckBanksNothing() {
        scanPointOf(alice, ScanPayout.ON_EXIT);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        orderScan(scout, POINT_HEX);
        handler.nextRoll = 7;

        handler.resolveScans();

        assertTrue(scout.getBankedScans().isEmpty(), "7 against a target number of 8 fails");
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_SCAN_FAILED));
        verify(gameManager, never()).entityUpdate(anyInt());
    }

    @Test
    void testATargetOutOfRangeOrOutOfSightIsRefusedWithoutARoll() {
        scanPointOf(alice, ScanPayout.ON_EXIT);
        BipedMek scout = mekOf(alice, new Coords(0, 3));
        orderScan(scout, POINT_HEX);

        handler.resolveScans();
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_CANNOT_SCAN), "5 hexes with a range of 2");

        BipedMek nearScout = mekOf(alice, SCANNER_HEX);
        orderScan(nearScout, POINT_HEX);
        handler.lineOfSight = false;
        handler.resolveScans();
        assertTrue(nearScout.getBankedScans().isEmpty(), "in range but no line of sight");
    }

    @Test
    void testAPointThatNeedNotBeCarriedHomePaysOnTheScan() {
        ObjectiveMarker scanPoint = scanPointOf(alice, ScanPayout.ON_SCAN);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        orderScan(scout, POINT_HEX);

        handler.resolveScans();

        assertTrue(scanPoint.getScoringScheme().isDecided());
        assertEquals(POINT_VALUE, VictoryPointTracker.findTracker(game.getVictoryContext()).getTeamVictoryPoints(1));
        assertTrue(scout.getBankedScans().isEmpty(), "nothing to carry: it is paid already");
    }

    @Test
    void testPointsPaidOnTheScanAreTakenBackWhenTheScoutIsLostBeforeItLeaves() {
        ObjectiveMarker scanPoint = scanPointOf(alice, ScanPayout.ON_SCAN_UNTIL_LOST);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        orderScan(scout, POINT_HEX);

        handler.resolveScans();
        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(POINT_VALUE, tracker.getTeamVictoryPoints(1), "paid the moment the scan succeeds");
        assertTrue(scanPoint.getScoringScheme().isDecided());
        assertEquals(1, scout.getBankedScans().size(), "the reading stays on the scout as the stake");

        // the defender's answer: the scout dies before it gets off the board
        scout.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
        when(game.getGraveyardEntities()).thenReturn(Collections.enumeration(List.of(scout)));
        when(game.getEntitiesVector()).thenReturn(List.of());
        handler.resolveScans();

        assertEquals(0, tracker.getTeamVictoryPoints(1), "the points are taken back");
        assertFalse(scanPoint.getScoringScheme().isDecided(), "and the point is open to be scanned again");
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_SCAN_POINTS_TAKEN_BACK));
    }

    @Test
    void testPointsPaidOnTheScanStayWhenTheScoutLeavesByAnyExit() {
        ObjectiveMarker scanPoint = scanPointOf(alice, ScanPayout.ON_SCAN_UNTIL_LOST);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        orderScan(scout, POINT_HEX);
        handler.resolveScans();

        // round 2, over the wrong edge: neither the exit turn nor the home edge applies to points already paid
        alice.setStartingPos(Board.START_N);
        scout.setRetreatedDirection(OffBoardDirection.SOUTH);
        leaveOverTheHomeEdge(scout, 2);
        handler.resolveScans();

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(POINT_VALUE, tracker.getTeamVictoryPoints(1), "still paid, and paid once");
        assertTrue(scanPoint.getScoringScheme().isDecided());
    }

    @Test
    void testOrdersAreDroppedWhenObjectivesAreOff() {
        gameOptions.getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(false);
        scanPointOf(alice, ScanPayout.ON_EXIT);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        orderScan(scout, POINT_HEX);

        handler.resolveScans();

        assertNull(scout.getPendingScan());
        assertTrue(scout.getBankedScans().isEmpty());
        verify(gameManager, never()).addReport(org.mockito.ArgumentMatchers.any(Report.class));
    }

    @Test
    void testTheCheckBreakdownReadsBaseFirstThenSignedModifiers() {
        TargetRoll roll = new TargetRoll(5, "Piloting skill");
        roll.addModifier(3, "scanning");
        roll.addModifier(-2, "active probe level 2");

        assertEquals("Piloting skill 5, +3 scanning, -2 active probe level 2", ObjectiveScanHandler.breakdownOf(roll));
    }

    // --- the Sensor Check mission: enemy units as targets ---

    @Test
    void testInASensorCheckMissionAnEnemyUnitIsAReadingWorthOnePointWhenCarriedHome() {
        gameOptions.getOption(OptionsConstants.VICTORY_USE_SENSOR_CHECK).setValue(true);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        BipedMek enemy = mekOf(bob, new Coords(4, 3));
        when(game.getEntity(enemy.getId())).thenReturn(enemy);
        scout.setPendingScan(new ScanAction(scout.getId(), enemy.getId()));
        when(game.getEntitiesVector()).thenReturn(List.of(scout, enemy));

        handler.resolveScans();
        assertEquals(1, scout.getBankedScans().size());
        assertFalse(scout.getBankedScans().getFirst().isObjectiveReading());

        leaveOverTheHomeEdge(scout, 5);
        handler.resolveScans();
        assertEquals(1, VictoryPointTracker.findTracker(game.getVictoryContext()).getTeamVictoryPoints(1));
    }

    @Test
    void testWithoutTheSensorCheckMissionAnEnemyUnitIsNothingOfInterest() {
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        BipedMek enemy = mekOf(bob, new Coords(4, 3));
        when(game.getEntity(enemy.getId())).thenReturn(enemy);
        scout.setPendingScan(new ScanAction(scout.getId(), enemy.getId()));
        when(game.getEntitiesVector()).thenReturn(List.of(scout, enemy));

        handler.resolveScans();

        assertTrue(scout.getBankedScans().isEmpty());
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_NOTHING_OF_INTEREST));
    }

    // --- carrying the readings home ---

    @Test
    void testCarryingAReadingHomeOnTheExitTurnPaysThePointOnce() {
        ObjectiveMarker scanPoint = scanPointOf(alice, ScanPayout.ON_EXIT);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        scout.bankScan(BankedScan.ofObjective(3, POINT_HEX, scanPoint.generalName()));
        leaveOverTheHomeEdge(scout, 5);

        handler.resolveScans();

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(POINT_VALUE, tracker.getTeamVictoryPoints(1));
        assertTrue(scanPoint.getScoringScheme().isDecided());
        assertTrue(scout.isBankedScansRedeemed());
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_SCAN_POINTS_AWARDED));

        // a second scout with the same reading is insurance, not a second payment
        BipedMek secondScout = mekOf(alice, SCANNER_HEX);
        secondScout.bankScan(BankedScan.ofObjective(4, POINT_HEX, scanPoint.generalName()));
        leaveOverTheHomeEdge(secondScout, 6);
        handler.resolveScans();
        assertEquals(POINT_VALUE, tracker.getTeamVictoryPoints(1), "still paid once");
    }

    @Test
    void testLeavingBeforeTheExitTurnLosesTheReadings() {
        ObjectiveMarker scanPoint = scanPointOf(alice, ScanPayout.ON_EXIT);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        scout.bankScan(BankedScan.ofObjective(3, POINT_HEX, scanPoint.generalName()));
        leaveOverTheHomeEdge(scout, 4);

        handler.resolveScans();

        assertNull(VictoryPointTracker.findTracker(game.getVictoryContext()));
        assertFalse(scanPoint.getScoringScheme().isDecided());
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_READINGS_LOST_EARLY_EXIT));
        assertTrue(scout.isBankedScansRedeemed(), "settled once, never again");
    }

    @Test
    void testLeavingOverTheWrongEdgeLosesTheReadingsWhenTheSideHasAHomeEdge() {
        ObjectiveMarker scanPoint = scanPointOf(alice, ScanPayout.ON_EXIT);
        alice.setStartingPos(Board.START_N);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        scout.bankScan(BankedScan.ofObjective(3, POINT_HEX, scanPoint.generalName()));
        scout.setRetreatedDirection(OffBoardDirection.EAST);
        leaveOverTheHomeEdge(scout, 5);

        handler.resolveScans();

        assertNull(VictoryPointTracker.findTracker(game.getVictoryContext()), "east is not the north side's home");
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_READINGS_LOST_WRONG_EDGE));

        // a side deployed anywhere has no single home edge, so any edge counts
        alice.setStartingPos(Board.START_ANY);
        BipedMek secondScout = mekOf(alice, SCANNER_HEX);
        secondScout.bankScan(BankedScan.ofObjective(4, POINT_HEX, scanPoint.generalName()));
        secondScout.setRetreatedDirection(OffBoardDirection.EAST);
        leaveOverTheHomeEdge(secondScout, 6);
        handler.resolveScans();
        assertEquals(POINT_VALUE, VictoryPointTracker.findTracker(game.getVictoryContext()).getTeamVictoryPoints(1));
    }

    @Test
    void testCornerDeploymentsHaveTwoHomeEdges() {
        assertEquals(java.util.Set.of(OffBoardDirection.NORTH, OffBoardDirection.EAST),
              ObjectiveScanHandler.homeEdgesOf(Board.START_NE));
        assertEquals(java.util.Set.of(OffBoardDirection.SOUTH), ObjectiveScanHandler.homeEdgesOf(Board.START_S));
        assertTrue(ObjectiveScanHandler.homeEdgesOf(Board.START_CENTER).isEmpty());
    }

    @Test
    void testADestroyedScoutTakesItsReadingsWithIt() {
        ObjectiveMarker scanPoint = scanPointOf(alice, ScanPayout.ON_EXIT);
        BipedMek scout = mekOf(alice, SCANNER_HEX);
        scout.bankScan(BankedScan.ofObjective(3, POINT_HEX, scanPoint.generalName()));
        scout.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
        when(game.getGraveyardEntities()).thenReturn(Collections.enumeration(List.of(scout)));
        when(game.getCurrentRound()).thenReturn(6);

        handler.resolveScans();

        assertNull(VictoryPointTracker.findTracker(game.getVictoryContext()), "the defender's counterplay");
        assertFalse(scanPoint.getScoringScheme().isDecided());
        assertTrue(reportIds().contains(ObjectiveScanHandler.REPORT_READINGS_LOST_WITH_UNIT));
    }

    // --- fixture ---

    private ObjectiveMarker scanPointOf(Player owner, ScanPayout payout) {
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("Depot");
        marker.setOwnerId(owner.getId());
        marker.setVictoryPointValue(POINT_VALUE);
        marker.setScoringScheme(ObjectiveScoringScheme.scan(payout));
        when(game.getGroundObjects(POINT_HEX)).thenReturn(List.of(marker));
        return marker;
    }

    private BipedMek mekOf(Player owner, Coords position) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(nextEntityId++);
        mek.setChassis("Scout");
        mek.setModel("S-" + mek.getId());
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(owner);
        mek.setPosition(position);
        mek.setDeployed(true);
        return mek;
    }

    private void orderScan(BipedMek scout, Coords hex) {
        scout.setPendingScan(new ScanAction(scout.getId(), hex, 0));
        when(game.getEntitiesVector()).thenReturn(List.of(scout));
    }

    private void leaveOverTheHomeEdge(BipedMek scout, int round) {
        scout.setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);
        when(game.getRetreatedEntities()).thenReturn(Collections.enumeration(List.of(scout)));
        when(game.getEntitiesVector()).thenReturn(List.of());
        when(game.getCurrentRound()).thenReturn(round);
    }

    private List<Integer> reportIds() {
        ArgumentCaptor<Report> reports = ArgumentCaptor.forClass(Report.class);
        verify(gameManager, atLeastOnce()).addReport(reports.capture());
        return reports.getAllValues().stream().map(report -> report.messageId).toList();
    }
}
