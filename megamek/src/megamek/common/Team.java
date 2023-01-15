/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import java.util.Enumeration;
import java.util.Objects;
import java.util.Vector;

/**
 * The Team class holds a list of information about a team. It holds the
 * initiative for the team, and contains a list of players on that team. It also
 * implements functions that gather the number of units each team has.
 */
public final class Team extends TurnOrdered {
    private static final long serialVersionUID = 2270215552964191597L;
    private Vector<Player> players = new Vector<>();
    private int id;
    private Boolean ObserverTeam = null;

    public Team(int newID) {
        id = newID;
    }

    public int getSize() {
        return players.size();
    }

    public int getNonObserverSize() {
        int nonObservers = 0;
        for (int i = 0; i < players.size(); i++) {
            if (!players.get(i).isObserver()) {
                nonObservers++;
            }
        }
        return nonObservers;
    }

    public Enumeration<Player> getPlayers() {
        return players.elements();
    }

    public Enumeration<Player> getNonObserverPlayers() {
        Vector<Player> nonObservers = new Vector<>();
        for (int i = 0; i < players.size(); i++) {
            if (!players.get(i).isObserver()) {
                nonObservers.add(players.get(i));
            }
        }
        return nonObservers.elements();
    }
    
    public Vector<Player> getPlayersVector() {
        return players;
    }

    public void resetTeam() {
        players.removeAllElements();
    }

    public void addPlayer(Player p) {
        players.addElement(p);
    }
    
    public boolean isObserverTeam() {
        if (ObserverTeam == null) {
            cacheObserverStatus();
        }
        return ObserverTeam;
    }
    
    public void cacheObserverStatus() {
        ObserverTeam = Boolean.TRUE;
        for (int i = 0; i < players.size(); i++) {
            if (!players.get(i).isObserver()) {
                ObserverTeam = false;
            }
        }
    }

    //get the next player on this team.
    public Player getNextValidPlayer(Player p, Game game) {
        //start from the next player
        for (int i = players.indexOf(p) + 1; i < players.size(); ++i) {
            if (game.getTurnForPlayer(players.get(i).getId()) != null) {
                return players.get(i);
            }
        }
        //if we haven't found one yet, start again from the beginning
        //worst case we reach exactly our current player again.
        for (int i = 0; i < (players.indexOf(p) + 1); ++i) {
            if (game.getTurnForPlayer(players.get(i).getId()) != null) {
                return players.get(i);
            }
        }
        //this should not happen, but if we don't find anything return ourselves again.
        return p;

    }

    /**
     * Clear the initiative of this object.
     */
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
        //  however the players accurately store their team id
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
    public int getNormalTurns(Game game) {
        int normal = getMultiTurns(game) + getOtherTurns();
        if (0 == normal) {
            normal = getEvenTurns();
        }
        return normal;
    }

    @Override
    public int getEvenTurns() {
        // Sum the even turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop.hasMoreElements(); ) {
            sum += loop.nextElement().getEvenTurns();
        }
        return sum;
    }

    @Override
    public int getOtherTurns() {
        // Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop.hasMoreElements(); ) {
            sum += loop.nextElement().getOtherTurns();
        }
        return sum;
    }

    @Override
    public int getMultiTurns(Game game) {
        // Sum the multi turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop.hasMoreElements(); ) {
            sum += loop.nextElement().getMultiTurns(game);
        }
        return sum;
    }

    @Override
    public int getSpaceStationTurns() {
        // Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop.hasMoreElements(); ) {
            sum += loop.nextElement().getSpaceStationTurns();
        }
        return sum;
    }

    @Override
    public int getJumpshipTurns() {
        // Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop.hasMoreElements(); ) {
            sum += loop.nextElement().getJumpshipTurns();
        }
        return sum;
    }

    @Override
    public int getWarshipTurns() {
        // Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop.hasMoreElements(); ) {
            sum += loop.nextElement().getWarshipTurns();
        }
        return sum;
    }

    @Override
    public int getDropshipTurns() {
        // Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop.hasMoreElements(); ) {
            sum += loop.nextElement().getDropshipTurns();
        }
        return sum;
    }

    @Override
    public int getSmallCraftTurns() {
        // Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop.hasMoreElements(); ) {
            sum += loop.nextElement().getSmallCraftTurns();
        }
        return sum;
    }
    
    @Override
    public int getTeleMissileTurns() {
        // Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop.hasMoreElements(); ) {
            sum += loop.nextElement().getSmallCraftTurns();
        }
        return sum;
    }

    @Override
    public int getAeroTurns() {
        // Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop.hasMoreElements(); ) {
            sum += loop.nextElement().getAeroTurns();
        }
        return sum;
    }

    /**
     * Two teams are equal if their ids and players are equal. <p> Override
     * <code>java.lang.Object#equals(Object)</code>
     */
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
        if (getId() == Player.TEAM_NONE) {
            return "No Team";
        } else {
            return "Team " + getId();
        }
    }

    public boolean hasTAG(Game game) {
        for (Enumeration<Player> e = game.getPlayers(); e.hasMoreElements(); ) {
            Player m = e.nextElement();
            if (getId() == m.getTeam()) {
                if (m.hasTAG()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * cycle through players team and select the best initiative
     */
    public int getTotalInitBonus(boolean bInitiativeCompensationBonus) {
        int dynamicBonus = Integer.MIN_VALUE;
        int constantb = Integer.MIN_VALUE;
        
        for (Player player : getPlayersVector()) {
            dynamicBonus = Math.max(dynamicBonus, player.getTurnInitBonus());
            dynamicBonus = Math.max(dynamicBonus, player.getCommandBonus());
            
            // this is a special case: it's an arbitrary bonus associated with a player
            constantb = Math.max(constantb, player.getConstantInitBonus());
        }
        
        return constantb + dynamicBonus +
                + getInitCompensationBonus(bInitiativeCompensationBonus);
    }
    
    @Override
    public int getInitCompensationBonus() {
        return getInitCompensationBonus(true);
    }

    public int getInitCompensationBonus(boolean bUseInitCompensation) {
        int nInitCompBonus = 0;

        if (bUseInitCompensation) {
            for (Player player : getPlayersVector()) {
                if (player.getInitCompensationBonus() > nInitCompBonus) {
                    nInitCompBonus = player.getInitCompensationBonus();
                }
            }
        }

        return nInitCompBonus;
    }

    @Override
    public void setInitCompensationBonus(int nNewValue) {
        for (Enumeration<Player> p = getPlayers(); p.hasMoreElements(); ) {
            Player player = p.nextElement();
            player.setInitCompensationBonus(nNewValue);
        }
    }
}