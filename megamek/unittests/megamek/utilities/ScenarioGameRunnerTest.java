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
package megamek.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.enums.Gender;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.utilities.ScenarioGameRunner.TeamStanding;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #8700: every bot disconnects itself when the VICTORY phase is announced, and the
 * server then removes its player from the game entirely. That removal races the end-of-game standings
 * collection, so a wiped team's player could already be gone when standings were read - and its CSV row
 * silently vanished from the {@link AIMatchRunner} results. The standings must instead be collected from the
 * game-start player snapshot.
 */
class ScenarioGameRunnerTest {

    private static final int WATCHER_TEAM = 3;
    private static final int WIPED_TEAM = 1;
    private static final int WINNING_TEAM = 2;

    private static final int WIPED_INITIAL_BV = 1000;
    private static final int WINNER_INITIAL_BV = 900;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /** The 2v2 duel from the issue, one round before the end: game state plus the game-start player snapshot. */
    private record Duel(Game game, List<Player> playersAtGameStart, Player wipedPlayer, Player winningPlayer) {}

    /**
     * Builds the wipe from the issue's example: a watcher with no units, a wiped side whose two fighters are
     * both in the graveyard, and a winning side with both fighters still on the board.
     */
    private static Duel buildFinishedDuel() {
        Game game = new Game();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        game.setBoard(smallGroundBoard());

        Player watcher = new Player(0, "Watcher");
        watcher.setTeam(WATCHER_TEAM);
        game.addPlayer(0, watcher);

        Player wipedPlayer = new Player(1, "PrincessSide");
        wipedPlayer.setTeam(WIPED_TEAM);
        wipedPlayer.setInitialEntityCount(2);
        wipedPlayer.setInitialBV(WIPED_INITIAL_BV);
        game.addPlayer(1, wipedPlayer);

        Player winningPlayer = new Player(2, "CasparSide");
        winningPlayer.setTeam(WINNING_TEAM);
        winningPlayer.setInitialEntityCount(2);
        winningPlayer.setInitialBV(WINNER_INITIAL_BV);
        game.addPlayer(2, winningPlayer);

        List<Player> playersAtGameStart = List.copyOf(game.getPlayersList());

        fighter(game, 11, wipedPlayer, new Coords(0, 0));
        fighter(game, 12, wipedPlayer, new Coords(1, 0));
        fighter(game, 21, winningPlayer, new Coords(2, 2));
        fighter(game, 22, winningPlayer, new Coords(3, 2));

        // the wiped side's fighters are shot down, moving them to the graveyard
        game.removeEntity(11, IEntityRemovalConditions.REMOVE_SALVAGEABLE);
        game.removeEntity(12, IEntityRemovalConditions.REMOVE_SALVAGEABLE);

        return new Duel(game, playersAtGameStart, wipedPlayer, winningPlayer);
    }

    private static void fighter(Game game, int id, Player owner, Coords position) {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        WeaponMounted laser = (WeaponMounted) WeaponMounted.createMounted(fighter,
              EquipmentType.get("ISMediumLaser"));
        try {
            fighter.addEquipment(laser, Aero.LOC_NOSE, false);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        fighter.setCrew(new Crew(CrewType.SINGLE, "Test Pilot", 1, 4, 5, Gender.FEMALE, false, null));
        fighter.setId(id);
        fighter.setOwner(owner);
        fighter.setPosition(position);
        fighter.setAltitude(5);
        fighter.setDeployed(true);
        game.addEntity(fighter, false);
    }

    private static Board smallGroundBoard() {
        Hex[] hexes = new Hex[25];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Board board = new Board(5, 5, hexes);
        board.setBoardType(BoardType.GROUND);
        return board;
    }

    private static TeamStanding standingOf(List<TeamStanding> standings, int team) {
        for (TeamStanding standing : standings) {
            if (standing.team() == team) {
                return standing;
            }
        }
        throw new AssertionError("No standing for team " + team + " in " + standings);
    }

    /**
     * The regression itself: the wiped bot's player has been removed from the game (VICTORY-phase disconnect),
     * but its team must still get a standing - with all units destroyed and no Battle Value remaining.
     */
    @Test
    void wipedTeamKeepsItsStandingAfterItsPlayerIsRemoved() {
        Duel duel = buildFinishedDuel();
        int winnerRemainingBV = duel.winningPlayer().getBV();

        duel.game().removePlayer(duel.wipedPlayer().getId());

        List<TeamStanding> standings = ScenarioGameRunner.collectTeamStandings(duel.game(),
              duel.playersAtGameStart(), WATCHER_TEAM);

        assertEquals(2, standings.size(), "both combatant teams must have a standing: " + standings);
        assertEquals(new TeamStanding(WIPED_TEAM, 2, 0, 0, 2, 0, WIPED_INITIAL_BV, 0),
              standingOf(standings, WIPED_TEAM));
        assertEquals(new TeamStanding(WINNING_TEAM, 2, 2, 0, 0, 0, WINNER_INITIAL_BV, winnerRemainingBV),
              standingOf(standings, WINNING_TEAM));
    }

    /**
     * The same race from the winner's side: if the winning bot's disconnect lands first, its surviving
     * entities lose their live owner - the win must still be attributed instead of reporting a draw.
     */
    @Test
    void winnerIsStillDeterminedAfterItsPlayerIsRemoved() {
        Duel duel = buildFinishedDuel();

        duel.game().removePlayer(duel.winningPlayer().getId());

        assertEquals(WINNING_TEAM, ScenarioGameRunner.determineWinningTeam(duel.game(),
              duel.playersAtGameStart(), WATCHER_TEAM));
    }

    /** The unraced case must keep producing exactly what it did before the fix. */
    @Test
    void standingsAndWinnerAreUnchangedWhenNoPlayerWasRemoved() {
        Duel duel = buildFinishedDuel();

        List<TeamStanding> standings = ScenarioGameRunner.collectTeamStandings(duel.game(),
              duel.playersAtGameStart(), WATCHER_TEAM);

        assertEquals(2, standings.size());
        assertEquals(new TeamStanding(WIPED_TEAM, 2, 0, 0, 2, 0, WIPED_INITIAL_BV, 0),
              standingOf(standings, WIPED_TEAM));
        assertEquals(WINNING_TEAM, ScenarioGameRunner.determineWinningTeam(duel.game(),
              duel.playersAtGameStart(), WATCHER_TEAM));
    }
}
