/**
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

    private TurnVectors team_order;

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

    public void updateTurnCount()
    {
        turns_mech = 0;
        turns_tank = 0;
        turns_infantry = 0;

        for (Enumeration i = players.elements(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();

            player.updateTurnCount();

            turns_mech += player.getMechCount();
            turns_tank += player.getTankCount();
            turns_infantry += player.getInfantryCount();
        }
    }

    public void determineTeamOrder(boolean infLast)
    {
        TurnOrdered.rollInitiative(players);
        team_order = TurnOrdered.generateTurnOrder(players, infLast);
    }

    public void resetTurnOrder()
    {
        team_order = null;
    }

    public TurnVectors getTurnOrder()
    {
        return team_order;
    }

    public int getId() {
        return id;
    }
}
