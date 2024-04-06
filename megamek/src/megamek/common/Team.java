/*
 * Copyright (c) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.common.annotations.Nullable;

/**
 * The Team class holds information about a team. It holds the initiative for the team, and contains a
 * list of players on that team.
 *
 * Note that Team should be usable for any type of game (TW, AS, BF, SBF) and therefore should not
 * make any direct use of Game, Entity, AlphaStrikeElement etc., instead using IGame and InGameObject if necessary.
 */
public final class Team extends TurnOrdered {

    private final List<Player> players = new ArrayList<>();
    private final int id;

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

/** 
     * @return The next player on this team, starting from Player p.
     */
    public Player getNextValidPlayer(Player p, Game game) {
        // Find the next valid player starting from the given player 'p'
        Player nextPlayer = findNextValidPlayerFrom(p, game);
        // If a valid player is found, return it. Otherwise, return the current player 'p'.
        if (nextPlayer != null) {
            return nextPlayer;
        } else {
            // This should not happen, but if we don't find any valid player, return ourselves again.
            return p;
        }
    }

    /**
     * Find the next valid player starting from the given player 'p'.
     * @param p The starting player.
     * @param game The current game.
     * @return The next valid player or null if none is found.
     */
    private Player findNextValidPlayerFrom(Player p, Game game) {
        // Get the index of the starting player 'p' in the players list.
        int startingIndex = players.indexOf(p);
        // Search for the next valid player starting from the current player's index.
        Player nextPlayer = searchNextValidPlayer(startingIndex, game);
        // If no valid player is found, search from the beginning of the players list.
        if (nextPlayer == null) {
            nextPlayer = searchNextValidPlayerFromBeginning(p, game);
        }
        // Return the next valid player or null if none is found.
        return nextPlayer;
    }

    /**
     * Search for the next valid player starting from the specified index.
     * @param startingIndex The index to start the search from.
     * @param game The current game.
     * @return The next valid player or null if none is found.
     */
    private Player searchNextValidPlayer(int startingIndex, Game game) {
        // Iterate over the players list starting from the specified index.
        for (int i = startingIndex + 1; i < players.size(); ++i) {
            // Check if the player at the current index has a turn in the game.
            if (game.getTurnForPlayer(players.get(i).getId()) != null) {
                // If a turn is found, return the player.
                return players.get(i);
            }
        }
        // Return null if no valid player is found.
        return null;
    }

    /**
     * Search for the next valid player starting from the beginning of the players list.
     * @param p The current player.
     * @param game The current game.
     * @return The next valid player or null if none is found.
     */
    private Player searchNextValidPlayerFromBeginning(Player p, Game game) {
        // Iterate over the players list starting from the beginning.
        for (int i = 0; i < (players.indexOf(p) + 1); ++i) {
            // Check if the player at the current index has a turn in the game.
            if (game.getTurnForPlayer(players.get(i).getId()) != null) {
                // If a turn is found, return the player.
                return players.get(i);
            }
        }
        // Return null if no valid player is found.
        return null;
    }

    @Override
    public void clearInitiative(boolean bUseInitComp) {
        getInitiative().clear();
        TurnOrdered.rollInitiative(players, bUseInitComp);
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
     * Return the number of "normal" turns that this item requires. This is
     * normally the sum of multi-unit turns and the other turns. A team without
     * any "normal" turns must return it's number of even turns to produce a
     * fair distribution of moves.
     *
     * @return the <code>int</code> number of "normal" turns this item should
     *         take in a phase.
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
        int dynamicBonus = Integer.MIN_VALUE;
        int constantBonus = Integer.MIN_VALUE;
        
        for (Player player : players) {
            dynamicBonus = Math.max(dynamicBonus, player.getTurnInitBonus());
            dynamicBonus = Math.max(dynamicBonus, player.getCommandBonus());
            
            // this is a special case: it's an arbitrary bonus associated with a player
            constantBonus = Math.max(constantBonus, player.getConstantInitBonus());
        }
        
        return constantBonus + dynamicBonus + getInitCompensationBonus(bInitiativeCompensationBonus);
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
}