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

import java.util.*;
import java.io.*;


/**
   The Team class holds a list of information about a team.  It holds the
   initative for the team, and contains a list of players on that team.

   It also implements functions that gather the number of units each team has.
*/
public final class Team extends TurnOrdered implements Serializable
{
    private Vector players = new Vector();
    private int id;

    public Team(int newID) {
        id = newID;
    }

    public int getSize()
    {
        return players.size();
    }

    public Enumeration getPlayers()
    {
        return players.elements();
    }

    public void resetTeam()
    {
        players.removeAllElements();
    }

    public void addPlayer(Player p)
    {
        players.addElement(p);
    }

    /**
     * Clear the initiative of this object.
     */
    public void clearInitiative() {
        this.getInitiative().clear();
        TurnOrdered.rollInitiative( players );
    }

    public TurnVectors determineTeamOrder()
    {
        return TurnOrdered.generateTurnOrder( players );
    }

    public int getId() {
        return id;
    }

    public int getLastTurns() {
        // Sum the last turns of all Players in this Team.
        int sum = 0;
        for (Enumeration loop = players.elements(); loop.hasMoreElements(); ) {
            sum += ( (Player) loop.nextElement() ).getLastTurns();
        }
        return sum;
    }

    public int getOtherTurns() {
        // Sum the other turns of all Players in this Team.
        int sum = 0;
        for (Enumeration loop = players.elements(); loop.hasMoreElements(); ) {
            sum += ( (Player) loop.nextElement() ).getOtherTurns();
        }
        return sum;
    }

    public int getMultiTurns() {
        // Sum the multi turns of all Players in this Team.
        int sum = 0;
        for (Enumeration loop = players.elements(); loop.hasMoreElements(); ) {
            sum += ( (Player) loop.nextElement() ).getMultiTurns();
        }
        return sum;
    }

    /**
     * Two teams are equal if their ids and players are equal.
     * <p/>
     * Override <code>java.lang.Object#equals(Object)
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Team other = (Team)object;
        if ( other.getId() != this.getId() ||
             other.getSize() != this.getSize() ) {
            return false;
        }
        Enumeration thisPlayers = this.getPlayers();
        Enumeration otherPlayers = other.getPlayers();
        while ( thisPlayers.hasMoreElements() ) {
            if ( !thisPlayers.nextElement().equals(otherPlayers.nextElement()) ) {
                return false;
            }
        }
        // The teams pass all tests, so they must match.
        return true;
    }

}
