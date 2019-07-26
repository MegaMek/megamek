/**
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

/**
 * The Team class holds a list of information about a team. It holds the
 * initative for the team, and contains a list of players on that team. It also
 * implements functions that gather the number of units each team has.
 */
public final class Team extends TurnOrdered {
    /**
     *
     */
    private static final long serialVersionUID = 2270215552964191597L;
    private List<IPlayer> players = new ArrayList<>();
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
        for (IPlayer player : players) {
            if (!player.isObserver()) {
                nonObservers++;
            }
        }
        return nonObservers;
    }

    public List<IPlayer> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public List<IPlayer> getNonObserverPlayers() {
        List<IPlayer> nonObservers = new ArrayList<>();
        for (IPlayer player : players) {
            if (!player.isObserver()) {
                nonObservers.add(player);
            }
        }
        return Collections.unmodifiableList(nonObservers);
    }
    

    public void resetTeam() {
        players.clear();
    }

    public void addPlayer(IPlayer p) {
        players.add(p);
    }
    
    public boolean isObserverTeam() {
        if (ObserverTeam == null) {
            cacheObversverStatus();
        }
        return ObserverTeam.booleanValue();
    }
    
    public void cacheObversverStatus() {
        ObserverTeam = Boolean.valueOf(true);
        for (IPlayer player : players) {
            if (!player.isObserver()) {
                ObserverTeam = false;
            }
        }
    }

    //get the next player on this team.
    public IPlayer getNextValidPlayer(IPlayer p, IGame game) {
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

    public TurnVectors determineTeamOrder(IGame game) {
        return TurnOrdered.generateTurnOrder(players, game);
    }

    public int getId() {
        // If Team Initiative is not turned on, id will be 0 for all teams,
        //  however the players accurately store their team id
        if (players.size() > 0) {
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
        for (IPlayer player : players) {
            sum += player.getEvenTurns();
        }
        return sum;
    }

    @Override
    public int getOtherTurns() {
        // Sum the other turns of all Players in this Team.
        int sum = 0;
        for (IPlayer player : players) {
            sum += player.getOtherTurns();
        }
        return sum;
    }

    @Override
    public int getMultiTurns(IGame game) {
        // Sum the multi turns of all Players in this Team.
        int sum = 0;
        for (IPlayer player : players) {
            sum += player.getMultiTurns(game);
        }
        return sum;
    }

    @Override
    public int getSpaceStationTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (IPlayer player : players) {
            sum += player.getSpaceStationTurns();
        }
        return sum;
    }

    @Override
    public int getJumpshipTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (IPlayer player : players) {
            sum += player.getJumpshipTurns();
        }
        return sum;
    }

    @Override
    public int getWarshipTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (IPlayer player : players) {
            sum += player.getWarshipTurns();
        }
        return sum;
    }

    @Override
    public int getDropshipTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (IPlayer player : players) {
            sum += player.getDropshipTurns();
        }
        return sum;
    }

    @Override
    public int getSmallCraftTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (IPlayer player : players) {
            sum += player.getSmallCraftTurns();
        }
        return sum;
    }
    
    @Override
    public int getTeleMissileTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (IPlayer player : players) {
            sum += player.getSmallCraftTurns();
        }
        return sum;
    }

    @Override
    public int getAeroTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (IPlayer player : players) {
            sum += player.getAeroTurns();
        }
        return sum;
    }

    /**
     * Two teams are equal if their ids and players are equal. <p/> Override
     * <code>java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals(Object object) {
        if(this == object) {
            return true;
        }
        if((null == object) || (getClass() != object.getClass())) {
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
    public String toString(){
        if (getId() == IPlayer.TEAM_NONE){
            return "No Team";
        } else {
            return "Team " + getId();
        }
    }

    public boolean hasTAG(IGame game) {
        for (IPlayer m : game.getPlayers()) {
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
     * take negatives only if the current bonus is zero
     */
    public int getTotalInitBonus(boolean bInitiativeCompensationBonus) {
        int turnb = 0;
        int constantb = Integer.MIN_VALUE;
        int commandb = Integer.MIN_VALUE;
        constantb = Integer.MIN_VALUE;
        for (IPlayer player : players) {
            turnb += player.getTurnInitBonus();
            if (player.getCommandBonus() > commandb) {
                commandb = player.getCommandBonus();
            }
            if (player.getConstantInitBonus() > constantb) {
                constantb = player.getConstantInitBonus();
            }
        }
        return constantb + turnb + commandb
                + getInitCompensationBonus(bInitiativeCompensationBonus);
    }
    
    @Override
    public int getInitCompensationBonus() {
        return getInitCompensationBonus(true);
    }

    public int getInitCompensationBonus(boolean bUseInitCompensation) {
        int nInitCompBonus = 0;

        if (bUseInitCompensation) {
            for (IPlayer player : players) {
                if (player.getInitCompensationBonus() > nInitCompBonus) {
                    nInitCompBonus = player.getInitCompensationBonus();
                }
            }
        }

        return nInitCompBonus;
    }

    @Override
    public void setInitCompensationBonus(int nNewValue) {
        for (IPlayer player : players) {
            player.setInitCompensationBonus(nNewValue);
        }
    }

    /**
     * cycle through entities on team and collect all the airborne VTOL/WIGE
     *
     * @return a vector of relevant entity ids
     */
    public Vector<Integer> getAirborneVTOL() {

        //a vector of unit ids
        Vector<Integer> units = new Vector<Integer>();
        for (IPlayer player : players) {
            units.addAll(player.getAirborneVTOL());
        }
        return units;
    }
}
