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

public abstract class TurnOrdered implements Serializable 
{

    protected InitiativeRoll  initiative = new InitiativeRoll();    

    protected int turns_mech   = 0;
    protected int turns_tank   = 0;
    protected int turns_infantry  = 0;

    public int getMechCount() {  
	return turns_mech;     
    }                          

    public int getTankCount() {   
	return turns_tank;       
    }                          

    public int getInfantryCount() {   
	return turns_infantry;      
    }                             

    public InitiativeRoll getInitiative() {
        return initiative;
    }

    public static void rollInitiative(Vector v)
    {
        // Clear all rolls
	for (Enumeration i = v.elements(); i.hasMoreElements();) {
	    final TurnOrdered item = (TurnOrdered)i.nextElement();
	    item.getInitiative().clear();
	}

	rollInitAndResolveTies(v, null);

        //This is the *auto-reroll* code for the Tactical Genius (lvl 3)
        // pilot ability.  It is NOT CURRENTLY IMPLEMENTED.  This code may
        // be incomplete/buggy/just plain wrong.
        /**
        if (v.firstElement() instanceof Team) {
            //find highest init roll
            int highestInit = 2;
            for (Enumeration i = v.elements(); i.hasMoreElements();) {
                final TurnOrdered item = (TurnOrdered)i.nextElement();
                highestInit = Math.max(item.getInitiative().getRoll(item.getInitiative().size() - 1), highestInit);
            }
            System.out.println("\n\n--->HIGH INIT ROLL: " + highestInit);
            //loop through teams
            for (Enumeration i = v.elements(); i.hasMoreElements();) {
                final TurnOrdered item = (TurnOrdered)i.nextElement();
                //loop through players
                for (Enumeration j = ((Team)item).getPlayers(); j.hasMoreElements();) {
                    final Player player = (Player)j.nextElement();
                    if (player.getGame().hasTacticalGenius(player) &&
                        item.getInitiative().getRoll(item.getInitiative().size() - 1) < highestInit && v.size() < 3) {
                        System.out.println("-->AUTO REROLL: " + player.getName());
                        Vector rv = new Vector();
                        rv.addElement(item);
                        rollInitAndResolveTies(v, rv);
                    }
                }
            } 
        }
        */

    }
    
    // This takes a vector of TurnOrdered (Teams or Players), rolls
    //  initiative, and resolves ties.  The second argument is used
    //  when a specific teams initiative should be re-rolled.
    public static void rollInitAndResolveTies(Vector v, Vector rerollRequests) {
        for (Enumeration i = v.elements(); i.hasMoreElements();) {
            final TurnOrdered item = (TurnOrdered)i.nextElement();
            if (rerollRequests == null) { //normal init roll
                item.getInitiative().addRoll(); // add a roll for all teams
            } else {
                //Resolve Tactical Genius (lvl 3) pilot ability
                for (Enumeration j = rerollRequests.elements(); j.hasMoreElements();) {
                    final TurnOrdered rerollItem = (TurnOrdered)j.nextElement();
                    if (item == rerollItem) { // this is the team re-rolling
                        item.getInitiative().replaceRoll();
                        break; // each team only needs one reroll
                    }
                }
            }
        }

        // check for ties
        Vector ties = new Vector();
        for (Enumeration i = v.elements(); i.hasMoreElements();) {
            final TurnOrdered item = (TurnOrdered)i.nextElement();
            ties.removeAllElements();
            ties.addElement(item);
            for (Enumeration j = v.elements(); j.hasMoreElements();) {
                final TurnOrdered other = (TurnOrdered)j.nextElement();
                if (item != other && item.getInitiative().equals(other.getInitiative())) {
                    ties.addElement(other);
                }
            }
            if (ties.size() > 1) {
                rollInitAndResolveTies(ties, null);
            }
        }
    }



    // This takes a vector of TurnOrdered, and generates a new vector. 
    public static TurnVectors generateTurnOrder(Vector v, boolean infLast)
    {
	int[] num_inf_turns = new int[v.size()];
	int[] num_oth_turns = new int[v.size()];
       
	int total_inf_turns = 0;
	int total_oth_turns = 0;
	int idx;
        TurnOrdered[] order = new TurnOrdered[v.size()];
        int oi = 0;

        com.sun.java.util.collections.ArrayList plist = 
	    new com.sun.java.util.collections.ArrayList(v.size());

        for (Enumeration i = v.elements(); i.hasMoreElements();) {
            Object item = i.nextElement();
            plist.add(item);
        }

        com.sun.java.util.collections.Collections.sort(plist, new com.sun.java.util.collections.Comparator() {
            public int compare(Object o1, Object o2) {
                return ((TurnOrdered)o1).getInitiative().compareTo(((TurnOrdered)o2).getInitiative());
            }
        });

        for (com.sun.java.util.collections.Iterator i = plist.iterator(); i.hasNext();) {
            final TurnOrdered item = (TurnOrdered)i.next();
            order[oi] = item;
	    
	    // If infantry are last, separate them.  Otherwise, place all 'turns' in one pile 
	    if (infLast) {
		num_inf_turns[oi] = item.getInfantryCount();
		num_oth_turns[oi] = item.getTankCount() + item.getMechCount();
	    } else {
		num_inf_turns[oi] = 0;
		num_oth_turns[oi] = item.getTankCount() + 
		    item.getMechCount() + item.getInfantryCount();
	    }

	    total_inf_turns += num_inf_turns[oi];
	    total_oth_turns += num_oth_turns[oi];
	    oi++;
        }	

	int min;
	int turns_left;
	TurnVectors turns = new TurnVectors(total_oth_turns, total_inf_turns);
	// We will do the 'other' units first (mechs and vehicles, and if infLast is false, 
	// infantry )

	min = Integer.MAX_VALUE;
	for(idx = 0; idx < oi ; idx++) {
	    if ( num_oth_turns[idx] != 0 && num_oth_turns[idx] < min)
		min = num_oth_turns[idx];
	}

	turns_left = total_oth_turns;

	while(turns_left > 0) {
	    for(idx = 0; idx < oi; idx++) {
		// If you have no turns here, skip
		if (num_oth_turns[idx] == 0)
		    continue;

		/* If you have less than twice the lowest, move 1.  Otherwise, move more. */
		int ntm = (int)Math.floor( ((double)num_oth_turns[idx]) / ((double)min) );
		for (int j = 0; j < ntm; j++) {
		    turns.non_infantry.addElement(order[idx]);
		    num_oth_turns[idx]--;
		    turns_left--;
		}
		    
	    }
	    // Since the smallest unit count had to place 1, reduce min)
	    min--;
	}

	// Now, we do the 'infantry' turns.
	if (infLast) {
	    
	    min = Integer.MAX_VALUE;
	    for(idx = 0; idx < oi ; idx++) {
		if ( num_inf_turns[idx] != 0 && num_inf_turns[idx] < min)
		    min = num_inf_turns[idx];
	    }
	    
	    turns_left = total_inf_turns;
	    
	    while(turns_left > 0) {
		for(idx = 0; idx < oi; idx++) {
		    // If you have no turns here, skip
		    if (num_inf_turns[idx] == 0)
			continue;
		    
		    /* If you have less than twice the lowest, move 1.  Otherwise, move more. */
		    int ntm = (int)Math.floor( ((double)num_inf_turns[idx]) / ((double)min) );
		    for (int j = 0; j < ntm; j++) {
			turns.infantry.addElement(order[idx]);
			num_inf_turns[idx]--;
			turns_left--;
		    }
		    
		}
		// Since the smallest unit count had to place 1, reduce min)
		min--;
	    }
	}

	return turns;

    }
    
    public abstract void updateTurnCount();

}


