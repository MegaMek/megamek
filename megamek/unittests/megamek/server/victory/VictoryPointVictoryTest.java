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

package megamek.server.victory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.server.scriptedEvents.GameEndTriggeredEvent;
import megamek.server.trigger.SpecificRoundEndTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the victory point resolution of {@link VictoryPointVictory} and its wiring into {@link VictoryHelper}: the
 * side with the highest VP total wins when the game ends (by turn limit or by a game-ending scripted event); tied VP
 * are a draw.
 */
class VictoryPointVictoryTest {

    private static final int GAME_DURATION_ROUNDS = 6;

    /** Victory condition with the False Objective die roll replaced by a settable value. */
    private static class TestableVictoryPointVictory extends VictoryPointVictory {
        private int falseObjectiveRoll = 6;

        @Override
        int rollFalseObjectiveCheck() {
            return falseObjectiveRoll;
        }
    }

    private Game game;
    private TestableVictoryPointVictory victoryPointVictory;

    @BeforeEach
    void setUp() {
        game = new Game();
        game.setVictoryContext(new HashMap<>());
        game.getOptions().getOption(OptionsConstants.VICTORY_USE_GAME_TURN_LIMIT).setValue(true);
        game.getOptions().getOption(OptionsConstants.VICTORY_GAME_TURN_LIMIT).setValue(GAME_DURATION_ROUNDS);
        // Disable the optional victory conditions: in an empty test game, battlefield control would
        // otherwise immediately end the game and mask the victory point behavior under test
        game.getOptions().getOption(OptionsConstants.VICTORY_CHECK_VICTORY).setValue(false);
        victoryPointVictory = new TestableVictoryPointVictory();
    }

    private void enableObjectiveScoring() {
        game.getOptions().getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(true);
    }

    @Test
    void testNoResultBeforeGameDuration() {
        enableObjectiveScoring();
        VictoryPointTracker.getTracker(game).awardToTeam(1, 3, 1, "controls Objective 1");
        game.setCurrentRound(GAME_DURATION_ROUNDS - 1);

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        assertFalse(result.isVictory());
    }

    @Test
    void testHighestTeamVictoryPointsWinAtGameDuration() {
        enableObjectiveScoring();
        VictoryPointTracker tracker = VictoryPointTracker.getTracker(game);
        tracker.awardToTeam(1, 2, 2, "controls Objective 1");
        tracker.awardToTeam(2, 1, 2, "controls Objective 3");
        tracker.awardToTeam(1, 1, 4, "controls Objective 2");
        game.setCurrentRound(GAME_DURATION_ROUNDS);

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        assertTrue(result.isVictory());
        assertFalse(result.isDraw());
        assertEquals(1, result.getWinningTeam());
        assertEquals(3.0, result.getTeamScore(1));
        assertEquals(1.0, result.getTeamScore(2));
    }

    @Test
    void testTiedVictoryPointsAreADraw() {
        enableObjectiveScoring();
        VictoryPointTracker tracker = VictoryPointTracker.getTracker(game);
        tracker.awardToTeam(1, 2, 3, "controls Objective 1");
        tracker.awardToTeam(2, 2, 3, "controls Objective 4");
        game.setCurrentRound(GAME_DURATION_ROUNDS);

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        assertTrue(result.isVictory());
        assertTrue(result.isDraw());
        assertEquals(Player.TEAM_NONE, result.getWinningTeam());
    }

    @Test
    void testScorelessGameIsADrawWhenObjectivesEnabled() {
        enableObjectiveScoring();
        game.setCurrentRound(GAME_DURATION_ROUNDS);

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        assertTrue(result.isVictory());
        assertTrue(result.isDraw());
    }

    @Test
    void testScorelessGameNotApplicableWhenObjectivesDisabled() {
        game.setCurrentRound(GAME_DURATION_ROUNDS);

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        assertFalse(result.isVictory());
    }

    @Test
    void testScoresResolveEvenWhenObjectivesOptionDisabled() {
        // Scenario events may award VP without the lobby option being set; those must still resolve
        VictoryPointTracker.getTracker(game).awardToTeam(2, 1, 5, "scenario event");
        game.setCurrentRound(GAME_DURATION_ROUNDS);

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        assertTrue(result.isVictory());
        assertEquals(2, result.getWinningTeam());
    }

    @Test
    void testPlayerVictoryPointsResolve() {
        enableObjectiveScoring();
        VictoryPointTracker tracker = VictoryPointTracker.getTracker(game);
        tracker.awardToPlayer(0, 2, 2, "controls Objective 1");
        tracker.awardToPlayer(1, 4, 4, "controls Objectives 2 and 3");
        game.setCurrentRound(GAME_DURATION_ROUNDS);

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        assertTrue(result.isVictory());
        assertEquals(1, result.getWinningPlayer());
    }

    // --- Objective Raid end-scoring (Phase 2b-3) ---

    private void enableObjectiveRaid() {
        game.getOptions().getOption(OptionsConstants.VICTORY_OBJECTIVE_RAID).setValue(true);
    }

    private ObjectiveMarker controlledMarker(String name, int controllingTeam, int victoryPointValue) {
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName(name);
        marker.setVictoryPointValue(victoryPointValue);
        marker.setController(controllingTeam, ObjectiveMarker.NO_CONTROLLER);
        return marker;
    }

    @Test
    void testObjectiveRaidEndScoring() {
        enableObjectiveRaid();
        game.placeGroundObject(new Coords(1, 1), controlledMarker("Alpha", 1, 1));
        game.placeGroundObject(new Coords(2, 2), controlledMarker("Bravo", 1, 2));
        game.placeGroundObject(new Coords(3, 3), controlledMarker("Charlie", 2, 1));
        ObjectiveMarker uncontrolledMarker = new ObjectiveMarker();
        uncontrolledMarker.setName("Delta");
        game.placeGroundObject(new Coords(4, 4), uncontrolledMarker);
        game.setCurrentRound(GAME_DURATION_ROUNDS);

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        assertTrue(result.isVictory());
        assertEquals(1, result.getWinningTeam());
        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(3, tracker.getTeamVictoryPoints(1));
        assertEquals(1, tracker.getTeamVictoryPoints(2));

        // the game-end check runs repeatedly (END and END_REPORT) - end-scoring must only award once
        victoryPointVictory.checkVictory(game, game.getVictoryContext());
        assertEquals(3, tracker.getTeamVictoryPoints(1));
        assertEquals(1, tracker.getTeamVictoryPoints(2));
    }

    @Test
    void testObjectiveRaidSuppressesNothingWhenUncontrolled() {
        enableObjectiveRaid();
        ObjectiveMarker uncontrolledMarker = new ObjectiveMarker();
        uncontrolledMarker.setName("Alpha");
        game.placeGroundObject(new Coords(1, 1), uncontrolledMarker);
        game.setCurrentRound(GAME_DURATION_ROUNDS);

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        // no objective is controlled: the mission ends in a draw
        assertTrue(result.isVictory());
        assertTrue(result.isDraw());
    }

    @Test
    void testUnconfirmedCandidateScoresNothingInObjectiveRaid() {
        enableObjectiveRaid();
        ObjectiveMarker candidate = controlledMarker("Maybe", 1, 5);
        candidate.setPotential(true);
        game.placeGroundObject(new Coords(1, 1), candidate);
        game.placeGroundObject(new Coords(2, 2), controlledMarker("Bravo", 2, 1));
        game.setCurrentRound(GAME_DURATION_ROUNDS);

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        assertEquals(2, result.getWinningTeam());
        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(0, tracker.getTeamVictoryPoints(1));
    }

    @Test
    void testFalseObjectiveCountsNothingOnARollOfOne() {
        enableObjectiveRaid();
        ObjectiveMarker falseMarker = controlledMarker("Decoy", 1, 5);
        falseMarker.setFalseObjective(true);
        game.placeGroundObject(new Coords(1, 1), falseMarker);
        game.placeGroundObject(new Coords(2, 2), controlledMarker("Bravo", 2, 1));
        game.placeGroundObject(new Coords(3, 3), controlledMarker("Charlie", 2, 1));
        game.setCurrentRound(GAME_DURATION_ROUNDS);
        victoryPointVictory.falseObjectiveRoll = 1;

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        assertEquals(2, result.getWinningTeam());
        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(0, tracker.getTeamVictoryPoints(1));
        assertEquals(2, tracker.getTeamVictoryPoints(2));
    }

    @Test
    void testFalseObjectiveCountsNormallyOnHigherRolls() {
        enableObjectiveRaid();
        ObjectiveMarker falseMarker = controlledMarker("Decoy", 1, 5);
        falseMarker.setFalseObjective(true);
        game.placeGroundObject(new Coords(1, 1), falseMarker);
        game.placeGroundObject(new Coords(2, 2), controlledMarker("Bravo", 2, 1));
        game.placeGroundObject(new Coords(3, 3), controlledMarker("Charlie", 2, 1));
        game.setCurrentRound(GAME_DURATION_ROUNDS);
        victoryPointVictory.falseObjectiveRoll = 2;

        VictoryResult result = victoryPointVictory.checkVictory(game, game.getVictoryContext());

        assertEquals(1, result.getWinningTeam());
        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(5, tracker.getTeamVictoryPoints(1));
    }

    @Test
    void testFalseRollDoesNotApplyWithTwoOrFewerObjectives() {
        // RAW: the False Objectives variant is not used where there are two or fewer objectives
        enableObjectiveRaid();
        ObjectiveMarker falseMarker = controlledMarker("Decoy", 1, 5);
        falseMarker.setFalseObjective(true);
        game.placeGroundObject(new Coords(1, 1), falseMarker);
        game.placeGroundObject(new Coords(2, 2), controlledMarker("Bravo", 2, 1));
        game.setCurrentRound(GAME_DURATION_ROUNDS);
        victoryPointVictory.falseObjectiveRoll = 1;

        victoryPointVictory.checkVictory(game, game.getVictoryContext());

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(game.getVictoryContext());
        assertEquals(5, tracker.getTeamVictoryPoints(1));
    }

    @Test
    void testVictoryHelperResolvesVictoryPointsAtGameDuration() {
        enableObjectiveScoring();
        VictoryPointTracker tracker = VictoryPointTracker.getTracker(game);
        tracker.awardToTeam(1, 1, 2, "controls Objective 1");
        tracker.awardToTeam(2, 3, 4, "controls Objectives 2, 3 and 4");
        game.setCurrentRound(GAME_DURATION_ROUNDS);
        VictoryHelper victoryHelper = new VictoryHelper(game);

        VictoryResult result = victoryHelper.checkForVictory(game, game.getVictoryContext());

        assertTrue(result.isVictory());
        assertEquals(2, result.getWinningTeam());
    }

    @Test
    void testVictoryHelperReturnsNoResultBeforeGameDuration() {
        enableObjectiveScoring();
        VictoryPointTracker.getTracker(game).awardToTeam(1, 2, 1, "controls Objective 1");
        game.setCurrentRound(GAME_DURATION_ROUNDS - 1);
        VictoryHelper victoryHelper = new VictoryHelper(game);

        VictoryResult result = victoryHelper.checkForVictory(game, game.getVictoryContext());

        assertFalse(result.isVictory());
    }

    @Test
    void testVictoryHelperResolvesVictoryPointsOnScriptedGameEnd() {
        int scriptedEndRound = 3;
        game.addScriptedEvent(new GameEndTriggeredEvent(new SpecificRoundEndTrigger(scriptedEndRound)));
        VictoryPointTracker.getTracker(game).awardToTeam(2, 2, 2, "controls Objective 1");
        game.setCurrentRound(scriptedEndRound);
        VictoryHelper victoryHelper = new VictoryHelper(game);

        VictoryResult result = victoryHelper.checkForVictory(game, game.getVictoryContext());

        assertTrue(result.isVictory());
        assertEquals(2, result.getWinningTeam());
    }

    @Test
    void testVictoryHelperScriptedGameEndWithoutPointsIsADraw() {
        int scriptedEndRound = 3;
        game.addScriptedEvent(new GameEndTriggeredEvent(new SpecificRoundEndTrigger(scriptedEndRound)));
        game.setCurrentRound(scriptedEndRound);
        VictoryHelper victoryHelper = new VictoryHelper(game);

        VictoryResult result = victoryHelper.checkForVictory(game, game.getVictoryContext());

        assertTrue(result.isVictory());
        assertTrue(result.isDraw());
    }
}
