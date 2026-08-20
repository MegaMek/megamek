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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.equipment.ObjectiveScoringScheme;
import megamek.common.equipment.ObjectiveScoringScheme.HoldCounting;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.server.totalWarfare.ObjectiveResolutionHandler.PlacedObjective;
import megamek.server.totalWarfare.ObjectiveResolutionHandler.ResolvedObjective;
import megamek.server.totalWarfare.ObjectiveResolutionHandler.Side;
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

    private Game game;
    private GameOptions gameOptions;
    private ObjectiveResolutionHandler handler;
    private Player teamOnePlayer;
    private Player teamTwoPlayer;
    private int nextEntityId = 1;

    @BeforeEach
    void setUp() {
        game = mock(Game.class);
        TWGameManager gameManager = mock(TWGameManager.class);
        when(gameManager.getGame()).thenReturn(game);
        handler = new ObjectiveResolutionHandler(gameManager);

        teamOnePlayer = new Player(0, "Alice");
        teamOnePlayer.setTeam(1);
        teamTwoPlayer = new Player(1, "Bob");
        teamTwoPlayer.setTeam(2);
        gameOptions = new GameOptions();
        // objective scoring is opt-in; the End Phase resolution tests exercise it enabled
        gameOptions.getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(true);
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
    void testNoResolutionWhenObjectiveScoringIsOff() {
        // markers can be placed without opting into objective scoring: with the option off, no control is
        // resolved and no victory points are awarded, so they cannot decide the winner of the game
        gameOptions.getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(false);
        Coords position = new Coords(2, 2);
        PlacedObjective objective = objectiveAt(position, 1, teamOnePlayer);
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(position, new ArrayList<>(List.of(objective.marker())));
        List<Entity> entities = List.of(groundUnit(teamOnePlayer, position));
        when(game.getGroundObjects()).thenReturn(groundObjects);
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        assertNull(VictoryPointTracker.findTracker(game.getVictoryContext()));
        assertEquals(ObjectiveMarker.NO_CONTROLLER, objective.marker().getControllingTeam());
    }

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
        // the resolved controller is stored on the markers for the objective victory triggers
        assertEquals(1, leftObjective.marker().getControllingTeam());
        assertEquals(1, rightObjective.marker().getControllingTeam());
        assertEquals(ObjectiveMarker.NO_CONTROLLER, leftObjective.marker().getControllingPlayerId());
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

    @Test
    void testLandedVTOLCannotControl() {
        // RAW (Control Radius - Assets): VTOL vehicle Assets can never control objectives, even landed
        PlacedObjective objective = objectiveAt(new Coords(5, 5), 1, teamOnePlayer);
        Entity landedVTOL = groundUnit(teamOnePlayer, new Coords(5, 5));
        when(landedVTOL.getMovementMode()).thenReturn(EntityMovementMode.VTOL);

        assertNull(handler.determineControllingSide(objective, List.of(landedVTOL)));
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

    // --- Scoring scheme counters (Control Points) ---

    /** Installs one marker with the given scheme as the game's only ground object. */
    private PlacedObjective schemePoint(ObjectiveScoringScheme scheme, Player owner, Coords position) {
        PlacedObjective objective = objectiveAt(position, 1, owner);
        objective.marker().setScoringScheme(scheme);
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(position, new ArrayList<>(List.of(objective.marker())));
        when(game.getGroundObjects()).thenReturn(groundObjects);
        return objective;
    }

    private void resolveWith(Entity... presentUnits) {
        List<Entity> entities = List.of(presentUnits);
        when(game.getEntitiesVector()).thenReturn(entities);
        handler.resolveObjectives();
    }

    @Test
    void testHoldSecuresAfterThresholdTurns() {
        Coords position = new Coords(5, 5);
        PlacedObjective objective = schemePoint(ObjectiveScoringScheme.hold(2, HoldCounting.CONSECUTIVE),
              teamTwoPlayer, position);
        Entity holdingUnit = groundUnit(teamOnePlayer, position);

        resolveWith(holdingUnit);
        assertFalse(objective.marker().getScoringScheme().isDecided());
        resolveWith(holdingUnit);

        assertTrue(objective.marker().getScoringScheme().isDecided());
        assertEquals(1, objective.marker().getScoringScheme().getSecuredTeam());
        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(1, tracker.getTeamVictoryPoints(1));
    }

    @Test
    void testConsecutiveHoldResetsOnLoss() {
        Coords position = new Coords(5, 5);
        PlacedObjective objective = schemePoint(ObjectiveScoringScheme.hold(2, HoldCounting.CONSECUTIVE),
              teamTwoPlayer, position);
        Entity holdingUnit = groundUnit(teamOnePlayer, position);

        resolveWith(holdingUnit);
        resolveWith();                 // an uncontrolled turn breaks the run
        resolveWith(holdingUnit);
        assertFalse(objective.marker().getScoringScheme().isDecided());
        resolveWith(holdingUnit);

        assertTrue(objective.marker().getScoringScheme().isDecided());
    }

    @Test
    void testCumulativeHoldPausesOnLoss() {
        Coords position = new Coords(5, 5);
        PlacedObjective objective = schemePoint(ObjectiveScoringScheme.hold(2, HoldCounting.CUMULATIVE),
              teamTwoPlayer, position);
        Entity holdingUnit = groundUnit(teamOnePlayer, position);

        resolveWith(holdingUnit);
        resolveWith();                 // the interruption only pauses the count
        resolveWith(holdingUnit);

        assertTrue(objective.marker().getScoringScheme().isDecided());
    }

    @Test
    void testDefendGripDrainsAndFalls() {
        Coords position = new Coords(5, 5);
        PlacedObjective objective = schemePoint(ObjectiveScoringScheme.defend(2, 1), teamOnePlayer, position);
        Entity raider = groundUnit(teamTwoPlayer, position);

        resolveWith(raider);
        assertFalse(objective.marker().getScoringScheme().isDecided());
        resolveWith(raider);

        assertTrue(objective.marker().getScoringScheme().isDecided());
        assertEquals(2, objective.marker().getScoringScheme().getSecuredTeam());
        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(1, tracker.getTeamVictoryPoints(2));
    }

    @Test
    void testDefendTiedPresenceDefersFall() {
        Coords position = new Coords(5, 5);
        PlacedObjective objective = schemePoint(ObjectiveScoringScheme.defend(1, 1), teamOnePlayer, position);
        Player teamThreePlayer = new Player(2, "Craig");
        teamThreePlayer.setTeam(3);
        Entity teamTwoRaider = groundUnit(teamTwoPlayer, position);
        Entity teamThreeRaider = groundUnit(teamThreePlayer, position);

        resolveWith(teamTwoRaider, teamThreeRaider);
        // the grip is gone, but two enemy sides are tied - the fall waits for a unique leader
        assertFalse(objective.marker().getScoringScheme().isDecided());

        resolveWith(teamTwoRaider);
        assertTrue(objective.marker().getScoringScheme().isDecided());
        assertEquals(2, objective.marker().getScoringScheme().getSecuredTeam());
    }

    @Test
    void testCaptureProgressCapturesThePoint() {
        Coords position = new Coords(5, 5);
        PlacedObjective objective = schemePoint(ObjectiveScoringScheme.capture(2, 1), teamOnePlayer, position);
        Entity attacker = groundUnit(teamTwoPlayer, position);

        resolveWith(attacker);
        assertFalse(objective.marker().getScoringScheme().isDecided());
        resolveWith(attacker);

        assertTrue(objective.marker().getScoringScheme().isDecided());
        assertEquals(2, objective.marker().getScoringScheme().getSecuredTeam());
    }

    @Test
    void testOwnerPushesCaptureProgressBack() {
        Coords position = new Coords(5, 5);
        PlacedObjective objective = schemePoint(ObjectiveScoringScheme.capture(2, 1), teamOnePlayer, position);
        Entity attacker = groundUnit(teamTwoPlayer, position);
        Entity defender = groundUnit(teamOnePlayer, position);

        resolveWith(attacker);         // progress 1 of 2
        resolveWith(defender);         // the owner pushes it back to 0
        resolveWith(attacker);         // progress 1 again
        assertFalse(objective.marker().getScoringScheme().isDecided());
        resolveWith(attacker);         // progress 2 - captured

        assertTrue(objective.marker().getScoringScheme().isDecided());
    }

    @Test
    void testDecidedPointAwardsOnlyOnce() {
        Coords position = new Coords(5, 5);
        PlacedObjective objective = schemePoint(ObjectiveScoringScheme.hold(1, HoldCounting.CONSECUTIVE),
              teamTwoPlayer, position);
        Entity holdingUnit = groundUnit(teamOnePlayer, position);

        resolveWith(holdingUnit);
        resolveWith(holdingUnit);
        resolveWith(holdingUnit);

        assertTrue(objective.marker().getScoringScheme().isDecided());
        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(1, tracker.getTeamVictoryPoints(1));
    }

    @Test
    void testStandardPairingIgnoresCounterPoints() {
        // one STANDARD point and one HOLD point, both controlled by team 1: the pairing rule sees only the
        // STANDARD point (a friendly one, so no pairing VP), while the HOLD counter advances separately
        Coords standardPosition = new Coords(2, 2);
        Coords holdPosition = new Coords(6, 6);
        PlacedObjective standardObjective = objectiveAt(standardPosition, 1, teamOnePlayer);
        PlacedObjective holdObjective = objectiveAt(holdPosition, 1, teamTwoPlayer);
        holdObjective.marker().setScoringScheme(ObjectiveScoringScheme.hold(5, HoldCounting.CONSECUTIVE));
        Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();
        groundObjects.put(standardPosition, new ArrayList<>(List.of(standardObjective.marker())));
        groundObjects.put(holdPosition, new ArrayList<>(List.of(holdObjective.marker())));
        when(game.getGroundObjects()).thenReturn(groundObjects);
        List<Entity> entities = List.of(groundUnit(teamOnePlayer, standardPosition),
              groundUnit(teamOnePlayer, holdPosition));
        when(game.getEntitiesVector()).thenReturn(entities);

        handler.resolveObjectives();

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertTrue((tracker == null) || (tracker.getTeamVictoryPoints(1) == 0));
        assertEquals(1, holdObjective.marker().getScoringScheme().getHeldTurns(1, -1));
    }
}
