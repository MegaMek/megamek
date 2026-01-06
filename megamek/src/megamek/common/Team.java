/*
 * Copyright (c) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import megamek.client.ratgenerator.FactionRecord;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.game.InitiativeBonusBreakdown;
import megamek.common.turns.TurnOrdered;
import megamek.common.turns.TurnVectors;

/**
 * The Team class holds information about a team. It holds the initiative for the team, and contains a list of players
 * on that team.
 * <p>
 * Note that Team should be usable for any type of game (TW, AS, BF, SBF) and therefore should not make any direct use
 * of Game, Entity, AlphaStrikeElement etc., instead using IGame and InGameObject if necessary.
 */
public final class Team extends TurnOrdered {

    private final List<Player> players = new ArrayList<>();
    private final int id;
    private String faction = FactionRecord.IS_GENERAL_KEY;

    public Team(int newID) {
        id = newID;
    }

    /** @return The number of players on this team (including observers). */
    public int size() {
        return players.size();
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public boolean hasPlayer(Player player) {
        return players.contains(player);
    }

    public List<Player> players() {
        return new ArrayList<>(players);
    }

    public List<Player> nonObserverPlayers() {
        return players.stream().filter(p -> !p.isObserver()).collect(toList());
    }

    /** @return The number of players on this team that are not observers. */
    public int getNonObserverSize() {
        return nonObserverPlayers().size();
    }

    /** Adds the given player to this team. Null players will not be added. */
    public void addPlayer(@Nullable Player player) {
        if (player != null) {
            players.removeIf(p -> p.equals(player));
            players.add(player);
        }
    }

    /** @return True when this team only consists of observer players. */
    public boolean isObserverTeam() {
        return getNonObserverSize() == 0;
    }

    /** @return The next player on this team, starting from Player p. */
    public Player getNextValidPlayer(Player p, Game game) {
        // start from the next player
        for (int i = players.indexOf(p) + 1; i < players.size(); ++i) {
            if (game.getTurnForPlayer(players.get(i).getId()) != null) {
                return players.get(i);
            }
        }
        // if we haven't found one yet, start again from the beginning
        // worst case we reach exactly our current player again.
        for (int i = 0; i < (players.indexOf(p) + 1); ++i) {
            if (game.getTurnForPlayer(players.get(i).getId()) != null) {
                return players.get(i);
            }
        }
        // this should not happen, but if we don't find anything return ourselves again.
        return p;

    }

    @Override
    public void clearInitiative(boolean bUseInitComp, Map<Team, Integer> initiativeAptitude) {
        getInitiative().clear();
        TurnOrdered.rollInitiative(players, bUseInitComp, initiativeAptitude);
    }

    public TurnVectors determineTeamOrder(Game game) {
        return TurnOrdered.generateTurnOrder(players, game);
    }

    public int getId() {
        // If Team Initiative is not turned on, id will be 0 for all teams,
        // however the players accurately store their team id
        if (!players.isEmpty()) {
            return players.get(0).getTeam();
        } else {
            return id;
        }
    }

    /**
     * Return the number of "normal" turns that this item requires. This is normally the sum of multi-unit turns and the
     * other turns. A team without any "normal" turns must return its number of even turns to produce a fair
     * distribution of moves.
     *
     * @return the <code>int</code> number of "normal" turns this item should take in a phase.
     */
    @Override
    public int getNormalTurns(IGame game) {
        int normalTurns = getMultiTurns(game) + getOtherTurns();
        return (normalTurns == 0) ? getEvenTurns() : normalTurns;
    }

    @Override
    public int getEvenTurns() {
        return players.stream().mapToInt(Player::getEvenTurns).sum();
    }

    @Override
    public int getOtherTurns() {
        return players.stream().mapToInt(Player::getOtherTurns).sum();
    }

    @Override
    public int getMultiTurns(IGame game) {
        return players.stream().mapToInt(p -> p.getMultiTurns(game)).sum();
    }

    @Override
    public int getSpaceStationTurns() {
        return players.stream().mapToInt(Player::getSpaceStationTurns).sum();
    }

    @Override
    public int getJumpshipTurns() {
        return players.stream().mapToInt(Player::getJumpshipTurns).sum();
    }

    @Override
    public int getWarshipTurns() {
        return players.stream().mapToInt(Player::getWarshipTurns).sum();
    }

    @Override
    public int getDropshipTurns() {
        return players.stream().mapToInt(Player::getDropshipTurns).sum();
    }

    @Override
    public int getSmallCraftTurns() {
        return players.stream().mapToInt(Player::getSmallCraftTurns).sum();
    }

    @Override
    public int getTeleMissileTurns() {
        return players.stream().mapToInt(Player::getTeleMissileTurns).sum();
    }

    @Override
    public int getAeroTurns() {
        return players.stream().mapToInt(Player::getAeroTurns).sum();
    }

    /** Two teams are equal if their ids and players are equal. */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if ((null == object) || (getClass() != object.getClass())) {
            return false;
        }
        final Team other = (Team) object;
        return (id == other.id) && Objects.equals(players, other.players);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, players);
    }

    @Override
    public String toString() {
        return (getId() == Player.TEAM_NONE) ? "No Team" : "Team " + getId();
    }

    /** @return The best initiative among the team's players. */
    public int getTotalInitBonus(boolean bInitiativeCompensationBonus) {
        return getInitBonusBreakdown(bInitiativeCompensationBonus).total();
    }

    /**
     * Returns a breakdown of all initiative bonus components for this team. This allows the initiative report to show
     * what contributes to the total bonus.
     *
     * @param bInitiativeCompensationBonus Whether to include initiative compensation bonus
     *
     * @return The breakdown of all bonus components
     */
    public InitiativeBonusBreakdown getInitBonusBreakdown(boolean bInitiativeCompensationBonus) {
        int hqBonus = 0;
        int quirkBonus = 0;
        String quirkName = null;
        int consoleBonus = 0;
        int crewCommandBonus = 0;
        int tcpBonus = 0;

        for (Player player : players) {
            // Track each source separately - these bonuses are always >= 0
            hqBonus = Math.max(hqBonus, player.getHQInitBonus());

            // For quirks, track both the bonus and the name of the quirk
            int playerQuirkBonus = player.getQuirkInitBonus();
            if (playerQuirkBonus > quirkBonus) {
                quirkBonus = playerQuirkBonus;
                quirkName = player.getQuirkInitBonusName();
            }

            consoleBonus = Math.max(consoleBonus, player.getCommandConsoleBonus());
            crewCommandBonus = Math.max(crewCommandBonus, player.getCrewCommandBonus());
            tcpBonus = Math.max(tcpBonus, player.getTCPInitBonus());
        }

        // Constant bonus can be negative, so we need to take max across players properly
        int constantBonus = players.stream().mapToInt(Player::getConstantInitBonus).max().orElse(0);

        int compensationBonus = getInitCompensationBonus(bInitiativeCompensationBonus);

        return new InitiativeBonusBreakdown(
              hqBonus,
              quirkBonus,
              quirkName,
              consoleBonus,
              crewCommandBonus,
              tcpBonus,
              constantBonus,
              compensationBonus,
              0  // crew bonus is for individual initiative mode only
        );
    }

    @Override
    public int getInitCompensationBonus() {
        return getInitCompensationBonus(true);
    }

    public int getInitCompensationBonus(boolean bUseInitCompensation) {
        int nInitCompBonus = 0;
        if (bUseInitCompensation) {
            nInitCompBonus = players.stream().mapToInt(Player::getInitCompensationBonus).max().orElse(0);
        }
        return Math.max(0, nInitCompBonus);
    }

    @Override
    public void setInitCompensationBonus(int initCompBonus) {
        players.forEach(p -> p.setInitCompensationBonus(initCompBonus));
    }

    public void setFaction(String fac) {
        faction = fac;
    }

    public String getFaction() {
        return faction;
    }

    /**
     * Determine if another team is an enemy of this team
     *
     *
     */
    public boolean isEnemyOf(Team t) {
        boolean enemy = true;
        if (t.equals(this)) {
            enemy = false;
        } else if (t.isObserverTeam()) {
            enemy = false;
        } else if (players.isEmpty()) {
            enemy = false;
        } else if (t.players().stream().noneMatch(p -> p.isEnemyOf(players.get(0)))) {
            enemy = false;
        }
        return enemy;
    }
}
