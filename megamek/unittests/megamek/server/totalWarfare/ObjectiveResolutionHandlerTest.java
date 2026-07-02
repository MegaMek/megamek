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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Collections;

import megamek.common.CriticalSlot;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.equipment.Sensor;
import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.Hex;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.units.Terrains;
import megamek.server.totalWarfare.ObjectiveResolutionHandler.PlacedObjective;
import megamek.server.totalWarfare.ObjectiveResolutionHandler.ResolvedObjective;
import megamek.server.totalWarfare.ObjectiveResolutionHandler.Side;
import megamek.server.victory.ScanTally;
import megamek.server.victory.VictoryPointTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the End-Phase objective control algorithm (eligibility exclusions, control radius, strict majority, ties) and
 * the standard control mission victory point scoring of {@link ObjectiveResolutionHandler}.
 */
class ObjectiveResolutionHandlerTest {

    private static final Side TEAM_1 = new Side(true, 1);
    private static final Side TEAM_2 = new Side(true, 2);

    /**
     * Handler with the non-deterministic seams (line of sight, dice rolls) replaced by settable values.
     */
    private static class TestableObjectiveResolutionHandler extends ObjectiveResolutionHandler {
        private boolean lineOfSight = true;
        private int scanRoll = 12;
        private int confirmationRoll = 6;
        private int fragileRoll = 6;
        private int forcedDropRoll = 12;

        TestableObjectiveResolutionHandler(TWGameManager gameManager) {
            super(gameManager);
        }

        @Override
        boolean hasLineOfSight(Entity scanner, Entity target) {
            return lineOfSight;
        }

        @Override
        boolean hasLineOfSightToHex(Entity scanner, Coords position) {
            return lineOfSight;
        }

        @Override
        int rollScanCheck() {
            return scanRoll;
        }

        @Override
        int rollObjectiveConfirmation() {
            return confirmationRoll;
        }

        @Override
        int rollFragileCheck() {
            return fragileRoll;
        }

        @Override
        int rollForcedDropCheck() {
            return forcedDropRoll;
        }
    }

    private Game game;
    private GameOptions gameOptions;
    private TestableObjectiveResolutionHandler handler;
    private Player teamOnePlayer;
    private Player teamTwoPlayer;
    private int nextEntityId = 1;

    @BeforeEach
    void setUp() {
        game = mock(Game.class);
        TWGameManager gameManager = mock(TWGameManager.class);
        when(gameManager.getGame()).thenReturn(game);
        handler = new TestableObjectiveResolutionHandler(gameManager);

        teamOnePlayer = new Player(0, "Alice");
        teamOnePlayer.setTeam(1);
        teamTwoPlayer = new Player(1, "Bob");
        teamTwoPlayer.setTeam(2);
        gameOptions = new GameOptions();
        when(game.getPlayer(0)).thenReturn(teamOnePlayer);
        when(game.getPlayer(1)).thenReturn(teamTwoPlayer);
        when(game.getOptions()).thenReturn(gameOptions);
        when(game.getVictoryContext()).thenReturn(new HashMap<>());
        when(game.getCurrentRound()).thenReturn(3);
    }

    private PlacedObjective objectiveAt(Coords position, int controlRadius, Player owner) {
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("Objective at " + position);
        marker.setControlRadius(controlRadius);
        marker.setOwnerId(owner.getId());
        return new PlacedObjective(position, marker);
    }

    private Entity groundUnit(Player owner, Coords position) {
        Entity entity = mock(Entity.class);
        when(entity.getId()).thenReturn(nextEntityId++);
        when(entity.getOwner()).thenReturn(owner);
        when(entity.getPosition()).thenReturn(position);
        when(entity.isDeployed()).thenReturn(true);
        when(entity.getTransportId()).thenReturn(Entity.NONE);
        when(entity.getMovementMode()).thenReturn(EntityMovementMode.BIPED);
        // isOffBoard, isDestroyed, isCrippled, isProne, isImmobile, isAirborne and isAirborneVTOLorWIGE
        // default to false on the mock, matching an ordinary active ground unit
        return entity;
    }

    // --- Control algorithm ---

    @Test
    void testSingleUnitControls() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        List<Entity> entities = List.of(groundUnit(teamOnePlayer, new Coords(5, 5)));

        assertEquals(TEAM_1, handler.determineControllingSide(objective, entities));
    }

    @Test
    void testStrictMajorityControls() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 2, teamOnePlayer);
        List<Entity> entities = List.of(
              groundUnit(teamOnePlayer, new Coords(5, 5)),
              groundUnit(teamTwoPlayer, new Coords(5, 6)),
              groundUnit(teamTwoPlayer, new Coords(6, 5)));

        assertEquals(TEAM_2, handler.determineControllingSide(objective, entities));
    }

    @Test
    void testTieIsUncontrolled() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        List<Entity> entities = List.of(
              groundUnit(teamOnePlayer, new Coords(5, 5)),
              groundUnit(teamTwoPlayer, new Coords(5, 6)));

        assertNull(handler.determineControllingSide(objective, entities));
    }

    @Test
    void testNoUnitsInRangeIsUncontrolled() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        List<Entity> entities = List.of(groundUnit(teamOnePlayer, new Coords(10, 10)));

        assertNull(handler.determineControllingSide(objective, entities));
    }

    @Test
    void testControlRadiusZeroOnlyCountsOwnHex() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 0, teamOnePlayer);
        List<Entity> entities = List.of(
              groundUnit(teamOnePlayer, new Coords(5, 6)),
              groundUnit(teamTwoPlayer, new Coords(5, 5)));

        assertEquals(TEAM_2, handler.determineControllingSide(objective, entities));
    }

    @Test
    void testControlRadiusTwoCountsDistantUnit() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 2, teamOnePlayer);
        Coords twoHexesAway = new Coords(5, 7);
        assertEquals(2, twoHexesAway.distance(new Coords(5, 5)));
        List<Entity> entities = List.of(groundUnit(teamOnePlayer, twoHexesAway));

        assertEquals(TEAM_1, handler.determineControllingSide(objective, entities));
    }

    @Test
    void testCrippledUnitDoesNotCount() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        Entity crippledUnit = groundUnit(teamOnePlayer, new Coords(5, 5));
        when(crippledUnit.isCrippled()).thenReturn(true);
        List<Entity> entities = List.of(crippledUnit, groundUnit(teamTwoPlayer, new Coords(5, 6)));

        assertEquals(TEAM_2, handler.determineControllingSide(objective, entities));
    }

    @Test
    void testProneUnitDoesNotCount() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        Entity proneUnit = groundUnit(teamOnePlayer, new Coords(5, 5));
        when(proneUnit.isProne()).thenReturn(true);

        assertNull(handler.determineControllingSide(objective, List.of(proneUnit)));
    }

    @Test
    void testImmobileUnitDoesNotCount() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        Entity immobileUnit = groundUnit(teamOnePlayer, new Coords(5, 5));
        when(immobileUnit.isImmobile()).thenReturn(true);

        assertNull(handler.determineControllingSide(objective, List.of(immobileUnit)));
    }

    @Test
    void testTransportedUnitDoesNotCount() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        Entity transportedUnit = groundUnit(teamOnePlayer, new Coords(5, 5));
        when(transportedUnit.getTransportId()).thenReturn(7);

        assertNull(handler.determineControllingSide(objective, List.of(transportedUnit)));
    }

    @Test
    void testAirborneUnitCannotControl() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        Entity airborneUnit = groundUnit(teamOnePlayer, new Coords(5, 5));
        when(airborneUnit.isAirborne()).thenReturn(true);

        assertNull(handler.determineControllingSide(objective, List.of(airborneUnit)));
    }

    @Test
    void testAirborneVTOLCannotControl() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        Entity airborneVTOL = groundUnit(teamOnePlayer, new Coords(5, 5));
        when(airborneVTOL.isAirborneVTOLorWIGE()).thenReturn(true);

        assertNull(handler.determineControllingSide(objective, List.of(airborneVTOL)));
    }

    @Test
    void testUndeployedUnitDoesNotCount() {
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        Entity undeployedUnit = groundUnit(teamOnePlayer, new Coords(5, 5));
        when(undeployedUnit.isDeployed()).thenReturn(false);

        assertNull(handler.determineControllingSide(objective, List.of(undeployedUnit)));
    }

    // --- Standard control scoring ---

    private ResolvedObjective resolved(PlacedObjective placed, Side owner, Side controller) {
        return new ResolvedObjective(placed, owner, controller);
    }

    private List<ResolvedObjective> standardFourObjectives(Side controllerOfTeamOneCounters,
          Side controllerOfTeamTwoCounters) {
        List<ResolvedObjective> resolvedObjectives = new ArrayList<>();
        resolvedObjectives.add(resolved(objectiveAt(new Coords(0, 0), 1, teamOnePlayer), TEAM_1,
              controllerOfTeamOneCounters));
        resolvedObjectives.add(resolved(objectiveAt(new Coords(0, 9), 1, teamOnePlayer), TEAM_1,
              controllerOfTeamOneCounters));
        resolvedObjectives.add(resolved(objectiveAt(new Coords(9, 0), 1, teamTwoPlayer), TEAM_2,
              controllerOfTeamTwoCounters));
        resolvedObjectives.add(resolved(objectiveAt(new Coords(9, 9), 1, teamTwoPlayer), TEAM_2,
              controllerOfTeamTwoCounters));
        return resolvedObjectives;
    }

    @Test
    void testOneFriendlyAndOneEnemyScoresOnePoint() {
        List<ResolvedObjective> resolvedObjectives = new ArrayList<>();
        resolvedObjectives.add(resolved(objectiveAt(new Coords(0, 0), 1, teamOnePlayer), TEAM_1, TEAM_1));
        resolvedObjectives.add(resolved(objectiveAt(new Coords(9, 0), 1, teamTwoPlayer), TEAM_2, TEAM_1));
        resolvedObjectives.add(resolved(objectiveAt(new Coords(0, 9), 1, teamOnePlayer), TEAM_1, null));
        resolvedObjectives.add(resolved(objectiveAt(new Coords(9, 9), 1, teamTwoPlayer), TEAM_2, null));
        VictoryPointTracker tracker = new VictoryPointTracker();

        handler.awardStandardControlVictoryPoints(resolvedObjectives, tracker);

        assertEquals(1, tracker.getTeamVictoryPoints(1));
        assertEquals(0, tracker.getTeamVictoryPoints(2));
    }

    @Test
    void testControllingAllObjectivesScoresTwoPoints() {
        List<ResolvedObjective> resolvedObjectives = standardFourObjectives(TEAM_1, TEAM_1);
        VictoryPointTracker tracker = new VictoryPointTracker();

        handler.awardStandardControlVictoryPoints(resolvedObjectives, tracker);

        assertEquals(2, tracker.getTeamVictoryPoints(1));
        assertEquals(0, tracker.getTeamVictoryPoints(2));
    }

    @Test
    void testOnlyFriendlyObjectivesScoreNothing() {
        // Each side holds only its own counters - standard control requires one friendly AND one enemy
        List<ResolvedObjective> resolvedObjectives = standardFourObjectives(TEAM_1, TEAM_2);
        VictoryPointTracker tracker = new VictoryPointTracker();

        handler.awardStandardControlVictoryPoints(resolvedObjectives, tracker);

        assertEquals(0, tracker.getTeamVictoryPoints(1));
        assertEquals(0, tracker.getTeamVictoryPoints(2));
    }

    @Test
    void testOnlyEnemyObjectivesScoreNothing() {
        // The sides swapped counters - each holds only enemy ones
        List<ResolvedObjective> resolvedObjectives = standardFourObjectives(TEAM_2, TEAM_1);
        VictoryPointTracker tracker = new VictoryPointTracker();

        handler.awardStandardControlVictoryPoints(resolvedObjectives, tracker);

        assertEquals(0, tracker.getTeamVictoryPoints(1));
        assertEquals(0, tracker.getTeamVictoryPoints(2));
    }

    @Test
    void testBothSidesCanScoreInTheSameRound() {
        List<ResolvedObjective> resolvedObjectives = new ArrayList<>();
        resolvedObjectives.add(resolved(objectiveAt(new Coords(0, 0), 1, teamOnePlayer), TEAM_1, TEAM_1));
        resolvedObjectives.add(resolved(objectiveAt(new Coords(9, 0), 1, teamTwoPlayer), TEAM_2, TEAM_1));
        resolvedObjectives.add(resolved(objectiveAt(new Coords(0, 9), 1, teamOnePlayer), TEAM_1, TEAM_2));
        resolvedObjectives.add(resolved(objectiveAt(new Coords(9, 9), 1, teamTwoPlayer), TEAM_2, TEAM_2));
        VictoryPointTracker tracker = new VictoryPointTracker();

        handler.awardStandardControlVictoryPoints(resolvedObjectives, tracker);

        assertEquals(1, tracker.getTeamVictoryPoints(1));
        assertEquals(1, tracker.getTeamVictoryPoints(2));
    }

    @Test
    void testUnknownOwnerCountsAsEnemy() {
        List<ResolvedObjective> resolvedObjectives = new ArrayList<>();
        resolvedObjectives.add(resolved(objectiveAt(new Coords(0, 0), 1, teamOnePlayer), TEAM_1, TEAM_1));
        resolvedObjectives.add(resolved(objectiveAt(new Coords(9, 9), 1, teamOnePlayer), null, TEAM_1));
        resolvedObjectives.add(resolved(objectiveAt(new Coords(5, 5), 1, teamTwoPlayer), TEAM_2, null));
        VictoryPointTracker tracker = new VictoryPointTracker();

        handler.awardStandardControlVictoryPoints(resolvedObjectives, tracker);

        assertEquals(1, tracker.getTeamVictoryPoints(1));
    }

    // --- End-to-end End Phase resolution ---

    @Test
    void testResolveObjectivesAwardsIntoGameTracker() {
        Coords leftPosition = new Coords(2, 2);
        Coords rightPosition = new Coords(12, 2);
        PlacedObjective leftObjective = objectiveAt(leftPosition, 1, teamOnePlayer);
        PlacedObjective rightObjective = objectiveAt(rightPosition, 1, teamTwoPlayer);
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(leftPosition, new ArrayList<>(List.of(leftObjective.marker())));
        groundObjects.put(rightPosition, new ArrayList<>(List.of(rightObjective.marker())));
        // build the mocked entities before stubbing game methods - stubbing one mock while another
        // stubbing is in progress trips Mockito's UnfinishedStubbingException
        List<Entity> entities = List.of(
              groundUnit(teamOnePlayer, leftPosition),
              groundUnit(teamOnePlayer, rightPosition));
        when(game.getGroundObjects()).thenReturn(groundObjects);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        // Team 1 controls both objectives (all of them) and receives 2 VP
        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(2, tracker.getTeamVictoryPoints(1));
        assertEquals(0, tracker.getTeamVictoryPoints(2));
    }

    // --- Objective destruction via buildings ---

    /**
     * Stubs the game with one objective marker placed at the given position inside a mocked building whose presence
     * is taken from {@code buildingHolder[0]} on every check, so tests can "destroy" the building between End Phases
     * by clearing the holder.
     */
    private PlacedObjective objectiveInBuilding(Coords position, IBuilding[] buildingHolder) {
        PlacedObjective objective = objectiveAt(position, 1, teamOnePlayer);
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(position, new ArrayList<>(List.of(objective.marker())));
        Board board = mock(Board.class);
        when(board.getBuildingAt(position)).thenAnswer(invocation -> buildingHolder[0]);
        when(game.getBoard()).thenReturn(board);
        when(game.getGroundObjects()).thenReturn(groundObjects);
        when(game.getEntitiesVector()).thenReturn(List.of());
        return objective;
    }

    private IBuilding buildingWithCF(Coords position, int constructionFactor) {
        IBuilding building = mock(IBuilding.class);
        when(building.getCurrentCF(position)).thenReturn(constructionFactor);
        return building;
    }

    @Test
    void testDestructibleObjectiveDestroyedWithBuilding() {
        Coords position = new Coords(4, 4);
        IBuilding[] buildingHolder = { buildingWithCF(position, 40) };
        PlacedObjective objective = objectiveInBuilding(position, buildingHolder);

        handler.resolveObjectives();
        assertTrue(objective.marker().isBuildingLinkInitialized());
        assertTrue(objective.marker().isInsideBuilding());
        assertFalse(objective.marker().isDestroyed());

        buildingHolder[0] = null;
        handler.resolveObjectives();

        assertTrue(objective.marker().isDestroyed());
        assertTrue(objective.marker().isDestructionProcessed());
    }

    @Test
    void testDestructibleObjectiveDestroyedWhenBuildingCFReachesZero() {
        Coords position = new Coords(4, 4);
        IBuilding building = mock(IBuilding.class);
        when(building.getCurrentCF(position)).thenReturn(40, 0);
        IBuilding[] buildingHolder = { building };
        PlacedObjective objective = objectiveInBuilding(position, buildingHolder);

        handler.resolveObjectives();
        assertFalse(objective.marker().isDestroyed());

        handler.resolveObjectives();

        assertTrue(objective.marker().isDestroyed());
    }

    @Test
    void testIndestructibleObjectiveSurvivesBuildingDestruction() {
        // RAW: destroyed with the building "unless the mission states that objectives cannot be destroyed"
        Coords position = new Coords(4, 4);
        IBuilding[] buildingHolder = { buildingWithCF(position, 40) };
        PlacedObjective objective = objectiveInBuilding(position, buildingHolder);
        objective.marker().setInvulnerable(true);

        handler.resolveObjectives();
        buildingHolder[0] = null;
        handler.resolveObjectives();

        assertFalse(objective.marker().isDestroyed());
    }

    @Test
    void testObjectiveOutsideBuildingsIsNeverBuildingDestroyed() {
        Coords position = new Coords(4, 4);
        IBuilding[] buildingHolder = { null };
        PlacedObjective objective = objectiveInBuilding(position, buildingHolder);

        handler.resolveObjectives();

        assertTrue(objective.marker().isBuildingLinkInitialized());
        assertFalse(objective.marker().isInsideBuilding());
        assertFalse(objective.marker().isDestroyed());
    }

    @Test
    void testDestroyedObjectiveExcludedFromScoring() {
        // Two objectives; the destructible one dies with its building, leaving only the enemy-owned one under
        // control - holding only enemy objectives scores nothing, and "controls all" ignores destroyed counters
        Coords friendlyPosition = new Coords(2, 2);
        Coords enemyPosition = new Coords(12, 2);
        PlacedObjective friendlyObjective = objectiveAt(friendlyPosition, 1, teamOnePlayer);
        friendlyObjective.marker().setDestroyed(true);
        PlacedObjective enemyObjective = objectiveAt(enemyPosition, 1, teamTwoPlayer);
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(friendlyPosition, new ArrayList<>(List.of(friendlyObjective.marker())));
        groundObjects.put(enemyPosition, new ArrayList<>(List.of(enemyObjective.marker())));
        List<Entity> entities = List.of(groundUnit(teamOnePlayer, enemyPosition));
        when(game.getGroundObjects()).thenReturn(groundObjects);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(0, tracker.getTeamVictoryPoints(1));
        assertTrue(friendlyObjective.marker().isDestructionProcessed());
    }

    @Test
    void testDestroyedObjectiveDoesNotScore() {
        Coords position = new Coords(2, 2);
        PlacedObjective objective = objectiveAt(position, 1, teamOnePlayer);
        objective.marker().setDestroyed(true);
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(position, new ArrayList<>(List.of(objective.marker())));
        List<Entity> entities = List.of(groundUnit(teamOnePlayer, position));
        when(game.getGroundObjects()).thenReturn(groundObjects);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertNull(tracker);
    }

    // --- Sensor Check mission (scanning) ---

    private void enableSensorCheckMission() {
        // deliberately without any TacOps sensor option: a scan is a Piloting-based sensor check (RAW)
        gameOptions.getOption(OptionsConstants.VICTORY_USE_SENSOR_CHECK).setValue(true);
        when(game.getGroundObjects()).thenReturn(new HashMap<>());
        when(game.getRetreatedEntities()).thenAnswer(invocation -> Collections.enumeration(List.<Entity>of()));
    }

    private static final int SCANNER_PILOTING_SKILL = 5;

    private void stubScannerBasics(Entity scanner) {
        when(scanner.getActiveSensor()).thenReturn(new Sensor(Sensor.TYPE_MEK_RADAR));
        Crew crew = mock(Crew.class);
        when(crew.getPiloting()).thenReturn(SCANNER_PILOTING_SKILL);
        when(scanner.getCrew()).thenReturn(crew);
        when(scanner.getMisc()).thenReturn(List.of());
    }

    private Entity scannerUnit(Player owner, Coords position) {
        Entity scanner = groundUnit(owner, position);
        stubScannerBasics(scanner);
        return scanner;
    }

    private MiscMounted probeEquipment(String internalName) {
        MiscType probeType = mock(MiscType.class);
        when(probeType.hasFlag(MiscType.F_BAP)).thenReturn(true);
        when(probeType.getInternalName()).thenReturn(internalName);
        MiscMounted probeMounted = mock(MiscMounted.class);
        when(probeMounted.getType()).thenReturn(probeType);
        return probeMounted;
    }

    @Test
    void testScanTargetNumberIsPilotingPlusScanModifier() {
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(0, 0));
        Entity target = groundUnit(teamTwoPlayer, new Coords(0, 1));

        assertEquals(SCANNER_PILOTING_SKILL + ObjectiveResolutionHandler.SCAN_MODIFIER,
              handler.computeScanTargetNumber(scanner, target));
    }

    @Test
    void testScanTargetNumberAgainstStealth() {
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(0, 0));
        Entity target = groundUnit(teamTwoPlayer, new Coords(0, 1));
        when(target.isStealthActive()).thenReturn(true);

        assertEquals(SCANNER_PILOTING_SKILL + ObjectiveResolutionHandler.SCAN_MODIFIER
                    + ObjectiveResolutionHandler.SCAN_STEALTH_MODIFIER,
              handler.computeScanTargetNumber(scanner, target));
    }

    @Test
    void testRawExamplePilotingFiveWithBloodhoundNeedsFivePlus() {
        // RAW example: a unit with Piloting Skill 5 and a Bloodhound (level 3) needs a 5+ to scan
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(0, 0));
        when(scanner.hasBAP(true)).thenReturn(true);
        List<MiscMounted> equipment = List.of(probeEquipment("ISBloodhoundActiveProbe"));
        when(scanner.getMisc()).thenReturn(equipment);
        Entity target = groundUnit(teamTwoPlayer, new Coords(0, 1));

        assertEquals(5, handler.computeScanTargetNumber(scanner, target));
    }

    @Test
    void testActiveProbeLevels() {
        Entity noProbeScanner = scannerUnit(teamOnePlayer, new Coords(0, 0));
        assertEquals(0, handler.activeProbeLevel(noProbeScanner));

        Entity lightProbeScanner = scannerUnit(teamOnePlayer, new Coords(0, 0));
        when(lightProbeScanner.hasBAP(true)).thenReturn(true);
        List<MiscMounted> lightProbe = List.of(probeEquipment("CLLightActiveProbe"));
        when(lightProbeScanner.getMisc()).thenReturn(lightProbe);
        assertEquals(ObjectiveResolutionHandler.PROBE_LEVEL_LIGHT, handler.activeProbeLevel(lightProbeScanner));

        Entity beagleScanner = scannerUnit(teamOnePlayer, new Coords(0, 0));
        when(beagleScanner.hasBAP(true)).thenReturn(true);
        List<MiscMounted> beagleProbe = List.of(probeEquipment("ISBeagleActiveProbe"));
        when(beagleScanner.getMisc()).thenReturn(beagleProbe);
        assertEquals(ObjectiveResolutionHandler.PROBE_LEVEL_STANDARD, handler.activeProbeLevel(beagleScanner));

        Entity bloodhoundScanner = scannerUnit(teamOnePlayer, new Coords(0, 0));
        when(bloodhoundScanner.hasBAP(true)).thenReturn(true);
        List<MiscMounted> bloodhoundProbe = List.of(probeEquipment("ISBloodhoundActiveProbe"));
        when(bloodhoundScanner.getMisc()).thenReturn(bloodhoundProbe);
        assertEquals(ObjectiveResolutionHandler.PROBE_LEVEL_BLOODHOUND, handler.activeProbeLevel(bloodhoundScanner));

        // probe capability without probe equipment (implants, quirks) counts as a light probe
        Entity implantScanner = scannerUnit(teamOnePlayer, new Coords(0, 0));
        when(implantScanner.hasBAP(true)).thenReturn(true);
        assertEquals(ObjectiveResolutionHandler.PROBE_LEVEL_LIGHT, handler.activeProbeLevel(implantScanner));
    }

    @Test
    void testOneSensorCriticalHitAddsToScanTargetNumber() {
        Mek scanner = mock(Mek.class);
        stubScannerBasics(scanner);
        when(scanner.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD)).thenReturn(1);
        Entity target = groundUnit(teamTwoPlayer, new Coords(0, 1));

        assertEquals(SCANNER_PILOTING_SKILL + ObjectiveResolutionHandler.SCAN_MODIFIER
                    + ObjectiveResolutionHandler.SCAN_SENSOR_CRITICAL_MODIFIER,
              handler.computeScanTargetNumber(scanner, target));
    }

    @Test
    void testTwoSensorCriticalHitsPreventScanning() {
        enableSensorCheckMission();
        Mek scanner = mock(Mek.class);
        when(scanner.getId()).thenReturn(nextEntityId++);
        when(scanner.getOwner()).thenReturn(teamOnePlayer);
        when(scanner.getPosition()).thenReturn(new Coords(5, 5));
        when(scanner.isDeployed()).thenReturn(true);
        when(scanner.getTransportId()).thenReturn(Entity.NONE);
        stubScannerBasics(scanner);
        when(scanner.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD)).thenReturn(2);
        Entity target = groundUnit(teamTwoPlayer, new Coords(5, 6));
        List<Entity> entities = List.of(scanner, target);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        ScanTally tally = ScanTally.findTally(game.getVictoryContext());
        assertEquals(0, tally.getScanCount(scanner.getId()));
    }

    @Test
    void testOnlyDesignatedTargetsCanBeScanned() {
        enableSensorCheckMission();
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(5, 5));
        Entity closeUndesignatedTarget = groundUnit(teamTwoPlayer, new Coords(5, 6));
        Entity designatedTarget = groundUnit(teamTwoPlayer, new Coords(5, 7));
        when(designatedTarget.isDesignatedScanTarget()).thenReturn(true);
        List<Entity> entities = List.of(scanner, closeUndesignatedTarget, designatedTarget);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        ScanTally tally = ScanTally.findTally(game.getVictoryContext());
        assertEquals(1, tally.getScanCount(scanner.getId()));
        assertTrue(tally.hasScanned(scanner.getId(), designatedTarget.getId()));
        assertFalse(tally.hasScanned(scanner.getId(), closeUndesignatedTarget.getId()));
    }

    @Test
    void testScanningRangeDefaultAndProbe() {
        Entity plainScanner = scannerUnit(teamOnePlayer, new Coords(0, 0));
        assertEquals(ObjectiveResolutionHandler.DEFAULT_SCANNING_RANGE, handler.scanningRange(plainScanner));

        Entity probeScanner = scannerUnit(teamOnePlayer, new Coords(0, 0));
        when(probeScanner.hasBAP(true)).thenReturn(true);
        when(probeScanner.getBAPRange()).thenReturn(4);
        assertEquals(4, handler.scanningRange(probeScanner));

        Entity shortProbeScanner = scannerUnit(teamOnePlayer, new Coords(0, 0));
        when(shortProbeScanner.hasBAP(true)).thenReturn(true);
        when(shortProbeScanner.getBAPRange()).thenReturn(1);
        assertEquals(ObjectiveResolutionHandler.DEFAULT_SCANNING_RANGE, handler.scanningRange(shortProbeScanner));
    }

    @Test
    void testSuccessfulScanIsBanked() {
        enableSensorCheckMission();
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(5, 5));
        Entity target = groundUnit(teamTwoPlayer, new Coords(5, 6));
        List<Entity> entities = List.of(scanner, target);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.scanRoll = 12;

        handler.resolveObjectives();

        ScanTally tally = ScanTally.findTally(game.getVictoryContext());
        assertEquals(1, tally.getScanCount(scanner.getId()));
        assertTrue(tally.hasScanned(scanner.getId(), target.getId()));
    }

    @Test
    void testFailedScanIsNotBanked() {
        enableSensorCheckMission();
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(5, 5));
        Entity target = groundUnit(teamTwoPlayer, new Coords(5, 6));
        List<Entity> entities = List.of(scanner, target);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.scanRoll = 2;

        handler.resolveObjectives();

        ScanTally tally = ScanTally.findTally(game.getVictoryContext());
        assertEquals(0, tally.getScanCount(scanner.getId()));
    }

    @Test
    void testScanBlockedByLineOfSight() {
        enableSensorCheckMission();
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(5, 5));
        Entity target = groundUnit(teamTwoPlayer, new Coords(5, 6));
        List<Entity> entities = List.of(scanner, target);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.lineOfSight = false;

        handler.resolveObjectives();

        ScanTally tally = ScanTally.findTally(game.getVictoryContext());
        assertEquals(0, tally.getScanCount(scanner.getId()));
    }

    @Test
    void testScanTargetBeyondRangeIsNotScanned() {
        enableSensorCheckMission();
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(5, 5));
        Entity target = groundUnit(teamTwoPlayer, new Coords(5, 9));
        List<Entity> entities = List.of(scanner, target);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        ScanTally tally = ScanTally.findTally(game.getVictoryContext());
        assertEquals(0, tally.getScanCount(scanner.getId()));
    }

    @Test
    void testUnitWithoutSensorCannotScan() {
        enableSensorCheckMission();
        Entity scanner = groundUnit(teamOnePlayer, new Coords(5, 5));
        Entity target = groundUnit(teamTwoPlayer, new Coords(5, 6));
        List<Entity> entities = List.of(scanner, target);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        ScanTally tally = ScanTally.findTally(game.getVictoryContext());
        assertEquals(0, tally.getScanCount(scanner.getId()));
    }

    @Test
    void testOneScanPerUnitPerTurnAndNoRescan() {
        enableSensorCheckMission();
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(5, 5));
        Entity firstTarget = groundUnit(teamTwoPlayer, new Coords(5, 6));
        Entity secondTarget = groundUnit(teamTwoPlayer, new Coords(5, 7));
        List<Entity> entities = List.of(scanner, firstTarget, secondTarget);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();
        ScanTally tally = ScanTally.findTally(game.getVictoryContext());
        assertEquals(1, tally.getScanCount(scanner.getId()));
        assertTrue(tally.hasScanned(scanner.getId(), firstTarget.getId()));

        // the closest target is banked, so the next End Phase scans the remaining one
        handler.resolveObjectives();
        assertEquals(2, tally.getScanCount(scanner.getId()));

        // all targets banked - further End Phases bank nothing new
        handler.resolveObjectives();
        assertEquals(2, tally.getScanCount(scanner.getId()));
    }

    @Test
    void testScanSkippedWhenMissionOff() {
        when(game.getGroundObjects()).thenReturn(new HashMap<>());

        handler.resolveObjectives();

        assertNull(ScanTally.findTally(game.getVictoryContext()));
    }

    private Entity fledScanner(Player owner, int scannerId) {
        Entity fledEntity = mock(Entity.class);
        when(fledEntity.getId()).thenReturn(scannerId);
        when(fledEntity.getOwner()).thenReturn(owner);
        when(fledEntity.getRemovalCondition()).thenReturn(IEntityRemovalConditions.REMOVE_IN_RETREAT);
        return fledEntity;
    }

    @Test
    void testExfiltrationConvertsScansToVictoryPoints() {
        enableSensorCheckMission();
        ScanTally tally = ScanTally.getTally(game);
        tally.recordScan(42, 10);
        tally.recordScan(42, 11);
        Entity exfiltratedScanner = fledScanner(teamOnePlayer, 42);
        when(game.getEntitiesVector()).thenReturn(List.of());
        when(game.getRetreatedEntities())
              .thenAnswer(invocation -> Collections.enumeration(List.of(exfiltratedScanner)));
        when(game.getCurrentRound()).thenReturn(ObjectiveResolutionHandler.EXFILTRATION_EARLIEST_ROUND);

        handler.resolveObjectives();

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(2, tracker.getTeamVictoryPoints(1));
        assertTrue(tally.isExfiltrationProcessed(42));
    }

    @Test
    void testExfiltrationAwardedOnlyOnce() {
        enableSensorCheckMission();
        ScanTally tally = ScanTally.getTally(game);
        tally.recordScan(42, 10);
        Entity exfiltratedScanner = fledScanner(teamOnePlayer, 42);
        when(game.getEntitiesVector()).thenReturn(List.of());
        when(game.getRetreatedEntities())
              .thenAnswer(invocation -> Collections.enumeration(List.of(exfiltratedScanner)));
        when(game.getCurrentRound()).thenReturn(ObjectiveResolutionHandler.EXFILTRATION_EARLIEST_ROUND);

        handler.resolveObjectives();
        handler.resolveObjectives();

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(1, tracker.getTeamVictoryPoints(1));
    }

    @Test
    void testFleeingBeforeExfiltrationRoundForfeitsScans() {
        enableSensorCheckMission();
        ScanTally tally = ScanTally.getTally(game);
        tally.recordScan(42, 10);
        Entity earlyFledScanner = fledScanner(teamOnePlayer, 42);
        when(game.getEntitiesVector()).thenReturn(List.of());
        when(game.getRetreatedEntities())
              .thenAnswer(invocation -> Collections.enumeration(List.of(earlyFledScanner)));
        when(game.getCurrentRound()).thenReturn(ObjectiveResolutionHandler.EXFILTRATION_EARLIEST_ROUND - 1);

        handler.resolveObjectives();

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertTrue((tracker == null) || (tracker.getTeamVictoryPoints(1) == 0));
        assertTrue(tally.isExfiltrationProcessed(42));

        // arriving at the exfiltration round later does not resurrect the forfeited scans
        when(game.getCurrentRound()).thenReturn(ObjectiveResolutionHandler.EXFILTRATION_EARLIEST_ROUND);
        handler.resolveObjectives();
        VictoryPointTracker trackerAfter = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertTrue((trackerAfter == null) || (trackerAfter.getTeamVictoryPoints(1) == 0));
    }

    // --- Objective variants (Potential, False, Fragile) ---

    @Test
    void testLandedVTOLCannotControl() {
        // RAW (Control Radius - Assets): VTOL vehicle Assets can never control objectives, even landed
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        Entity landedVTOL = groundUnit(teamOnePlayer, new Coords(5, 5));
        when(landedVTOL.getMovementMode()).thenReturn(EntityMovementMode.VTOL);

        assertNull(handler.determineControllingSide(objective, List.of(landedVTOL)));
    }

    /** Places one objective marker as the game's only ground object and stubs an empty entity list. */
    private PlacedObjective placeSingleObjective(PlacedObjective objective) {
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(objective.position(), new ArrayList<>(List.of(objective.marker())));
        when(game.getGroundObjects()).thenReturn(groundObjects);
        when(game.getEntitiesVector()).thenReturn(List.of());
        return objective;
    }

    @Test
    void testCandidateConfirmedByScan() {
        PlacedObjective candidate = objectiveAt(new Coords(5, 5), 1, teamTwoPlayer);
        candidate.marker().setPotential(true);
        placeSingleObjective(candidate);
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(5, 6));
        List<Entity> entities = List.of(scanner);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.scanRoll = 12;
        handler.confirmationRoll = ObjectiveResolutionHandler.CONFIRMATION_MINIMUM_ROLL;

        handler.resolveObjectives();

        assertTrue(candidate.marker().isConfirmed());
        verify(game, never()).removeGroundObject(candidate.position(), candidate.marker());
    }

    @Test
    void testUselessCandidateRemovedFromBattlefield() {
        PlacedObjective candidate = objectiveAt(new Coords(5, 5), 1, teamTwoPlayer);
        candidate.marker().setPotential(true);
        placeSingleObjective(candidate);
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(5, 6));
        List<Entity> entities = List.of(scanner);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.scanRoll = 12;
        handler.confirmationRoll = ObjectiveResolutionHandler.CONFIRMATION_MINIMUM_ROLL - 1;

        handler.resolveObjectives();

        assertFalse(candidate.marker().isConfirmed());
        verify(game).removeGroundObject(candidate.position(), candidate.marker());
    }

    @Test
    void testFailedConfirmationScanKeepsCandidate() {
        PlacedObjective candidate = objectiveAt(new Coords(5, 5), 1, teamTwoPlayer);
        candidate.marker().setPotential(true);
        placeSingleObjective(candidate);
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(5, 6));
        List<Entity> entities = List.of(scanner);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.scanRoll = 2;

        handler.resolveObjectives();

        assertFalse(candidate.marker().isConfirmed());
        verify(game, never()).removeGroundObject(candidate.position(), candidate.marker());
    }

    @Test
    void testConfirmationScanTakesPriorityOverUnitScan() {
        enableSensorCheckMission();
        PlacedObjective candidate = objectiveAt(new Coords(5, 5), 1, teamTwoPlayer);
        candidate.marker().setPotential(true);
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(candidate.position(), new ArrayList<>(List.of(candidate.marker())));
        when(game.getGroundObjects()).thenReturn(groundObjects);
        Entity scanner = scannerUnit(teamOnePlayer, new Coords(5, 6));
        Entity enemyUnit = groundUnit(teamTwoPlayer, new Coords(5, 7));
        List<Entity> entities = List.of(scanner, enemyUnit);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        assertTrue(candidate.marker().isConfirmed());
        ScanTally tally = ScanTally.findTally(game.getVictoryContext());
        assertEquals(0, tally.getScanCount(scanner.getId()));
    }

    @Test
    void testUnconfirmedCandidateDoesNotScore() {
        // Team 1 controls its own objective and an unconfirmed enemy candidate: without the candidate
        // counting as an enemy objective, standard control scoring awards nothing
        Coords friendlyPosition = new Coords(2, 2);
        Coords candidatePosition = new Coords(4, 2);
        PlacedObjective friendlyObjective = objectiveAt(friendlyPosition, 1, teamOnePlayer);
        PlacedObjective enemyCandidate = objectiveAt(candidatePosition, 1, teamTwoPlayer);
        enemyCandidate.marker().setPotential(true);
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(friendlyPosition, new ArrayList<>(List.of(friendlyObjective.marker())));
        groundObjects.put(candidatePosition, new ArrayList<>(List.of(enemyCandidate.marker())));
        List<Entity> entities = List.of(
              groundUnit(teamOnePlayer, friendlyPosition),
              groundUnit(teamOnePlayer, candidatePosition));
        when(game.getGroundObjects()).thenReturn(groundObjects);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(0, tracker.getTeamVictoryPoints(1));
    }

    @Test
    void testConfirmedCandidateScoresNormally() {
        // Same situation, but the candidate is confirmed - it counts as an enemy objective, so holding
        // 1 friendly + 1 enemy of the 3 scorable objectives awards 1 VP
        Coords friendlyPosition = new Coords(2, 2);
        Coords candidatePosition = new Coords(4, 2);
        Coords uncontrolledPosition = new Coords(12, 12);
        PlacedObjective friendlyObjective = objectiveAt(friendlyPosition, 1, teamOnePlayer);
        PlacedObjective enemyCandidate = objectiveAt(candidatePosition, 1, teamTwoPlayer);
        enemyCandidate.marker().setPotential(true);
        enemyCandidate.marker().setConfirmed(true);
        PlacedObjective uncontrolledEnemyObjective = objectiveAt(uncontrolledPosition, 1, teamTwoPlayer);
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(friendlyPosition, new ArrayList<>(List.of(friendlyObjective.marker())));
        groundObjects.put(candidatePosition, new ArrayList<>(List.of(enemyCandidate.marker())));
        groundObjects.put(uncontrolledPosition, new ArrayList<>(List.of(uncontrolledEnemyObjective.marker())));
        List<Entity> entities = List.of(
              groundUnit(teamOnePlayer, friendlyPosition),
              groundUnit(teamOnePlayer, candidatePosition));
        when(game.getGroundObjects()).thenReturn(groundObjects);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(1, tracker.getTeamVictoryPoints(1));
    }

    @Test
    void testFragileObjectiveDestroyedByFire() {
        Coords position = new Coords(4, 4);
        PlacedObjective fragileObjective = objectiveAt(position, 1, teamOnePlayer);
        fragileObjective.marker().setFragile(true);
        placeSingleObjective(fragileObjective);
        Board board = mock(Board.class);
        Hex burningHex = mock(Hex.class);
        when(burningHex.containsTerrain(Terrains.FIRE)).thenReturn(true);
        when(board.getHex(position)).thenReturn(burningHex);
        when(game.getBoard()).thenReturn(board);
        handler.fragileRoll = ObjectiveResolutionHandler.FRAGILE_DESTRUCTION_MAXIMUM_ROLL;

        handler.resolveObjectives();

        assertTrue(fragileObjective.marker().isDestroyed());
        assertTrue(fragileObjective.marker().isDestructionProcessed());
    }

    @Test
    void testFragileObjectiveSurvivesFireRoll() {
        Coords position = new Coords(4, 4);
        PlacedObjective fragileObjective = objectiveAt(position, 1, teamOnePlayer);
        fragileObjective.marker().setFragile(true);
        placeSingleObjective(fragileObjective);
        Board board = mock(Board.class);
        Hex burningHex = mock(Hex.class);
        when(burningHex.containsTerrain(Terrains.FIRE)).thenReturn(true);
        when(board.getHex(position)).thenReturn(burningHex);
        when(game.getBoard()).thenReturn(board);
        handler.fragileRoll = ObjectiveResolutionHandler.FRAGILE_DESTRUCTION_MAXIMUM_ROLL + 1;

        handler.resolveObjectives();

        assertFalse(fragileObjective.marker().isDestroyed());
    }

    @Test
    void testNonFragileObjectiveIgnoresFire() {
        Coords position = new Coords(4, 4);
        PlacedObjective objective = objectiveAt(position, 1, teamOnePlayer);
        placeSingleObjective(objective);
        Board board = mock(Board.class);
        Hex burningHex = mock(Hex.class);
        when(burningHex.containsTerrain(Terrains.FIRE)).thenReturn(true);
        when(board.getHex(position)).thenReturn(burningHex);
        when(game.getBoard()).thenReturn(board);
        handler.fragileRoll = ObjectiveResolutionHandler.FRAGILE_DESTRUCTION_MAXIMUM_ROLL;

        handler.resolveObjectives();

        assertFalse(objective.marker().isDestroyed());
    }

    // --- Mobile Objective carrying (Phase 5b) ---

    private ObjectiveMarker mobileMarker(Player owner) {
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("MacGuffin");
        marker.setMobile(true);
        marker.setOwnerId(owner.getId());
        return marker;
    }

    private Entity carrierOf(Player owner, Coords position, ObjectiveMarker marker) {
        Entity carrier = groundUnit(owner, position);
        when(carrier.getDistinctCarriedObjects()).thenReturn(List.of(marker));
        return carrier;
    }

    @Test
    void testCarriedObjectiveAutoControlledByCarrier() {
        ObjectiveMarker marker = mobileMarker(teamTwoPlayer);
        Coords position = new Coords(5, 5);
        Entity carrier = groundUnit(teamOnePlayer, position);
        PlacedObjective carriedObjective = new PlacedObjective(position, marker, carrier);
        // a swarm of enemy units in the radius cannot contest a carried objective
        List<Entity> entities = List.of(
              carrier,
              groundUnit(teamTwoPlayer, position),
              groundUnit(teamTwoPlayer, new Coords(5, 6)),
              groundUnit(teamTwoPlayer, new Coords(6, 5)));

        assertEquals(TEAM_1, handler.determineControllingSide(carriedObjective, entities));
    }

    @Test
    void testImmobileCarrierDropsObjectiveWithFragileRoll() {
        ObjectiveMarker marker = mobileMarker(teamOnePlayer);
        marker.setFragile(true);
        Coords position = new Coords(5, 5);
        Entity carrier = carrierOf(teamOnePlayer, position, marker);
        when(carrier.isImmobile()).thenReturn(true);
        when(game.getGroundObjects()).thenReturn(new HashMap<>());
        List<Entity> entities = List.of(carrier);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.fragileRoll = ObjectiveResolutionHandler.FRAGILE_DESTRUCTION_MAXIMUM_ROLL;

        handler.resolveObjectives();

        verify(carrier).dropCarriedObject(marker, false);
        verify(game).placeGroundObject(position, marker);
        assertTrue(marker.isDestroyed());
    }

    @Test
    void testProneCarrierDropsObjectiveWithoutFragileRoll() {
        ObjectiveMarker marker = mobileMarker(teamOnePlayer);
        marker.setFragile(true);
        Coords position = new Coords(5, 5);
        Entity carrier = carrierOf(teamOnePlayer, position, marker);
        when(carrier.isProne()).thenReturn(true);
        when(game.getGroundObjects()).thenReturn(new HashMap<>());
        List<Entity> entities = List.of(carrier);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.fragileRoll = 1;

        handler.resolveObjectives();

        verify(carrier).dropCarriedObject(marker, false);
        verify(game).placeGroundObject(position, marker);
        assertFalse(marker.isDestroyed());
    }

    @Test
    void testMobileCarrierKeepsObjective() {
        ObjectiveMarker marker = mobileMarker(teamOnePlayer);
        Coords position = new Coords(5, 5);
        Entity carrier = carrierOf(teamOnePlayer, position, marker);
        when(game.getGroundObjects()).thenReturn(new HashMap<>());
        List<Entity> entities = List.of(carrier);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        verify(carrier, never()).dropCarriedObject(marker, false);
    }

    @Test
    void testFailedForcedDropCheckDropsObjective() {
        ObjectiveMarker marker = mobileMarker(teamOnePlayer);
        Coords position = new Coords(5, 5);
        Entity carrier = carrierOf(teamOnePlayer, position, marker);
        carrier.damageThisPhase = 5;
        int carrierId = carrier.getId();
        when(carrier.getBasePilotingRoll()).thenReturn(new PilotingRollData(carrierId, 5, "test piloting"));
        List<Entity> entities = List.of(carrier);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.forcedDropRoll = 4;

        handler.resolveForcedObjectiveDrops();

        verify(carrier).dropCarriedObject(marker, false);
        verify(game).placeGroundObject(position, marker);
    }

    @Test
    void testPassedForcedDropCheckKeepsObjective() {
        ObjectiveMarker marker = mobileMarker(teamOnePlayer);
        Entity carrier = carrierOf(teamOnePlayer, new Coords(5, 5), marker);
        carrier.damageThisPhase = 5;
        int carrierId = carrier.getId();
        when(carrier.getBasePilotingRoll()).thenReturn(new PilotingRollData(carrierId, 5, "test piloting"));
        List<Entity> entities = List.of(carrier);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.forcedDropRoll = 5;

        handler.resolveForcedObjectiveDrops();

        verify(carrier, never()).dropCarriedObject(marker, false);
    }

    @Test
    void testTwoHandActuatorsEaseForcedDropCheck() {
        // Entity needs 5+, rolls 4 and drops; a Mek with two intact hand actuators needs 3+ and keeps hold
        ObjectiveMarker marker = mobileMarker(teamOnePlayer);
        Mek mekCarrier = mock(Mek.class);
        when(mekCarrier.getId()).thenReturn(nextEntityId++);
        when(mekCarrier.getOwner()).thenReturn(teamOnePlayer);
        when(mekCarrier.getPosition()).thenReturn(new Coords(5, 5));
        when(mekCarrier.getDistinctCarriedObjects()).thenReturn(List.of(marker));
        when(mekCarrier.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM)).thenReturn(true);
        when(mekCarrier.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM)).thenReturn(true);
        mekCarrier.damageThisPhase = 5;
        int mekCarrierId = mekCarrier.getId();
        when(mekCarrier.getBasePilotingRoll()).thenReturn(new PilotingRollData(mekCarrierId, 5, "test"));
        List<Entity> entities = List.of(mekCarrier);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.forcedDropRoll = 4;

        handler.resolveForcedObjectiveDrops();

        verify(mekCarrier, never()).dropCarriedObject(marker, false);
    }

    @Test
    void testUndamagedCarrierMakesNoForcedDropCheck() {
        ObjectiveMarker marker = mobileMarker(teamOnePlayer);
        Entity carrier = carrierOf(teamOnePlayer, new Coords(5, 5), marker);
        List<Entity> entities = List.of(carrier);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.forcedDropRoll = 2;

        handler.resolveForcedObjectiveDrops();

        verify(carrier, never()).dropCarriedObject(marker, false);
    }

    @Test
    void testDroppedObjectiveNeverDestroyedUnlessFragile() {
        ObjectiveMarker plainMarker = mobileMarker(teamOnePlayer);
        Entity carrier = carrierOf(teamOnePlayer, new Coords(5, 5), plainMarker);
        handler.fragileRoll = 1;

        assertFalse(handler.resolveObjectiveDropDamage(carrier, plainMarker));
        assertFalse(plainMarker.isDestroyed());

        ObjectiveMarker fragileMarker = mobileMarker(teamOnePlayer);
        fragileMarker.setFragile(true);
        assertTrue(handler.resolveObjectiveDropDamage(carrier, fragileMarker));
        assertTrue(fragileMarker.isDestroyed());

        ObjectiveMarker luckyFragileMarker = mobileMarker(teamOnePlayer);
        luckyFragileMarker.setFragile(true);
        handler.fragileRoll = ObjectiveResolutionHandler.FRAGILE_DESTRUCTION_MAXIMUM_ROLL + 1;
        assertFalse(handler.resolveObjectiveDropDamage(carrier, luckyFragileMarker));
        assertFalse(luckyFragileMarker.isDestroyed());
    }

    @Test
    void testCapturedUnitDoesNotExfiltrate() {
        enableSensorCheckMission();
        ScanTally tally = ScanTally.getTally(game);
        tally.recordScan(42, 10);
        Entity capturedScanner = fledScanner(teamOnePlayer, 42);
        when(capturedScanner.getRemovalCondition()).thenReturn(IEntityRemovalConditions.REMOVE_CAPTURED);
        when(game.getEntitiesVector()).thenReturn(List.of());
        when(game.getRetreatedEntities())
              .thenAnswer(invocation -> Collections.enumeration(List.of(capturedScanner)));
        when(game.getCurrentRound()).thenReturn(ObjectiveResolutionHandler.EXFILTRATION_EARLIEST_ROUND);

        handler.resolveObjectives();

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertTrue((tracker == null) || (tracker.getTeamVictoryPoints(1) == 0));
    }
}
