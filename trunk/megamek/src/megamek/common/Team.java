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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * The Team class holds a list of information about a team. It holds the
 * initative for the team, and contains a list of players on that team. It also
 * implements functions that gather the number of units each team has.
 */
public final class Team extends TurnOrdered implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 2270215552964191597L;
    private Vector<Player> players = new Vector<Player>();
    private int id;

    public Team(int newID) {
        id = newID;
    }

    public int getSize() {
        return players.size();
    }

    public Enumeration<Player> getPlayers() {
        return players.elements();
    }

    public void resetTeam() {
        players.removeAllElements();
    }

    public void addPlayer(Player p) {
        players.addElement(p);
    }

    /**
     * Clear the initiative of this object.
     */
    public void clearInitiative() {
        this.getInitiative().clear();
        TurnOrdered.rollInitiative(players);
    }

    public TurnVectors determineTeamOrder(IGame game) {
        return TurnOrdered.generateTurnOrder(players, game);
    }

    public int getId() {
        return id;
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
    public int getNormalTurns(IGame game) {
        int normal = this.getMultiTurns(game) + this.getOtherTurns();
        if (0 == normal)
            normal = this.getEvenTurns();
        return normal;
    }

    public int getEvenTurns() {
        // Sum the even turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop
                .hasMoreElements();) {
            sum += loop.nextElement().getEvenTurns();
        }
        return sum;
    }

    public int getOtherTurns() {
        // Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop
                .hasMoreElements();) {
            sum += loop.nextElement().getOtherTurns();
        }
        return sum;
    }

    public int getMultiTurns(IGame game) {
        // Sum the multi turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop
                .hasMoreElements();) {
            sum += loop.nextElement().getMultiTurns(game);
        }
        return sum;
    }
    
    public int getSpaceStationTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop
                .hasMoreElements();) {
            sum += loop.nextElement().getSpaceStationTurns();
        }
        return sum;
    }
    
    public int getJumpshipTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop
                .hasMoreElements();) {
            sum += loop.nextElement().getJumpshipTurns();
        }
        return sum;
    }
    
    public int getWarshipTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop
                .hasMoreElements();) {
            sum += loop.nextElement().getWarshipTurns();
        }
        return sum;
    }
    
    public int getDropshipTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop
                .hasMoreElements();) {
            sum += loop.nextElement().getDropshipTurns();
        }
        return sum;
    }
    
    public int getSmallCraftTurns() {
//      Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration<Player> loop = players.elements(); loop
                .hasMoreElements();) {
            sum += loop.nextElement().getSmallCraftTurns();
        }
        return sum;
    }

    /**
     * Two teams are equal if their ids and players are equal. <p/> Override
     * <code>java.lang.Object#equals(Object)
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Team other = (Team) object;
        if (other.getId() != this.getId() || other.getSize() != this.getSize()) {
            return false;
        }
        Enumeration<Player> thisPlayers = this.getPlayers();
        Enumeration<Player> otherPlayers = other.getPlayers();
        while (thisPlayers.hasMoreElements()) {
            if (!thisPlayers.nextElement().equals(otherPlayers.nextElement())) {
                return false;
            }
        }
        // The teams pass all tests, so they must match.
        return true;
    }

    public boolean hasTAG(IGame game) {
        for (Enumeration<Player> e = game.getPlayers(); e.hasMoreElements();) {
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
     *  cycle through players team and select the best initiative
     *  take negatives only if the current bonus is zero
     */
    public int getTotalInitBonus() {

        int constantb = 0;
        int turnb = 0;
        int commandb = 0;

        for (Enumeration<Player> p = getPlayers(); p.hasMoreElements();) {
            Player player = p.nextElement();
            if (player.getConstantInitBonus() > constantb
                    && player.getConstantInitBonus() != 0) {
                constantb = player.getConstantInitBonus();
            }
            // also accept it if it is negative and current bonus is zero
            if (player.getConstantInitBonus() < 0 && constantb == 0) {
                constantb = player.getConstantInitBonus();
            }
        }
        
        for (Enumeration<Player> p = getPlayers(); p.hasMoreElements();) {
            Player player = p.nextElement();
            turnb += player.getTurnInitBonus();
            if(player.getCommandBonus() > commandb)
                commandb = player.getCommandBonus();
        }
        
        return constantb + turnb + commandb;
    }
    
    /**
     * cycle through entities on team and collect all the airborne VTOL/WIGE
     * @return a vector of relevant entity ids
     */
    public Vector<Integer> getAirborneVTOL() {
    
        //a vector of unit ids
        Vector<Integer> units = new Vector<Integer>();       
        for (Enumeration<Player> loop = players.elements(); loop.hasMoreElements();) {
            Player player = loop.nextElement();
            units.addAll(player.getAirborneVTOL());
        }
        return units;
    }
}