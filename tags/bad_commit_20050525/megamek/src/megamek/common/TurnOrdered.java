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

public abstract class TurnOrdered implements Serializable 
{

    private InitiativeRoll      initiative      = new InitiativeRoll();    

    private transient int       turns_other     = 0;
    private transient int       turns_even      = 0;
    private transient int       turns_multi     = 0;

    /**
     * Return the number of "normal" turns that this item requires.
     * This is normally the sum of multi-unit turns and the other turns.
     * <p/>
     * Subclasses are expected to override this value in order to make
     * the "move even" code work correctly.
     *
     * @return  the <code>int</code> number of "normal" turns this item
     * should take in a phase.
     */
    public int getNormalTurns(IGame game) {
        return this.getMultiTurns(game) + this.getOtherTurns();
    }

    public int getOtherTurns() {  
    return turns_other;     
    }                          

    public int getEvenTurns() {   
    return turns_even;       
    }

    public int getMultiTurns(IGame game) {
        return (int) Math.ceil( ((double)turns_multi) /
                                ((double)game.getOptions().intOption("inf_proto_move_multi")) );
    }

    public void incrementOtherTurns() {
        turns_other++;
    }

    public void incrementEvenTurns() {
        turns_even++;
    }

    public void incrementMultiTurns() {
        turns_multi++;
    }

    public void resetOtherTurns() {
        turns_other = 0;
    }

    public void resetEvenTurns() {
        turns_even = 0;
    }

    public void resetMultiTurns() {
        turns_multi = 0;
    }

    public InitiativeRoll getInitiative() {
        return initiative;
    }

    /**
     * Clear the initiative of this object.
     */
    public void clearInitiative() {
        this.getInitiative().clear();
    }

    public static void rollInitiative(Vector v)
    {
        // Clear all rolls
    for (Enumeration i = v.elements(); i.hasMoreElements();) {
        final TurnOrdered item = (TurnOrdered)i.nextElement();
        item.clearInitiative();
    }

    rollInitAndResolveTies(v, null);

        //This is the *auto-reroll* code for the Tactical Genius (lvl 3)
        // pilot ability.  It is NOT CURRENTLY IMPLEMENTED.  This code may
        // be incomplete/buggy/just plain wrong.
        // TODO : fix me
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



    /**
     * This takes a Vector of TurnOrdered and generates a TurnVector. 
     */
    public static TurnVectors generateTurnOrder( Vector v, IGame game )
    {
    int[] num_even_turns = new int[v.size()];
    int[] num_normal_turns = new int[v.size()];
       
    int total_even_turns = 0;
    int total_normal_turns = 0;
    int index;
        TurnOrdered[] order = new TurnOrdered[v.size()];
        int orderedItems = 0;

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

        // Walk through the ordered items.
        for ( com.sun.java.util.collections.Iterator i = plist.iterator();
              i.hasNext(); orderedItems++ ) {
            final TurnOrdered item = (TurnOrdered)i.next();
            order[orderedItems] = item;
        
            // Track even turns separately from the normal turns.
            num_normal_turns[orderedItems] = item.getNormalTurns(game);
            num_even_turns[orderedItems] = item.getEvenTurns();

            // Keep a running total.
        total_even_turns += num_even_turns[orderedItems];
        total_normal_turns += num_normal_turns[orderedItems];
        }   

    int min;
    int turns_left;

    // We will do the 'normal' turns first, and then the 'even' turns.
    min = Integer.MAX_VALUE;
    for(index = 0; index < orderedItems ; index++) {
        if ( num_normal_turns[index] != 0 && num_normal_turns[index] < min)
        min = num_normal_turns[index];
    }

    TurnVectors turns =
            new TurnVectors(total_normal_turns, total_even_turns, min);

        // Allocate the normal turns.
    turns_left = total_normal_turns;
    while (turns_left > 0) {
        for (index = 0; index < orderedItems; index++) {
        // If you have no turns here, skip
        if (num_normal_turns[index] == 0)
            continue;

        // If you have less than twice the lowest,
                // move 1.  Otherwise, move more.
        int ntm = num_normal_turns[index] / min;
        for (int j = 0; j < ntm; j++) {
            turns.addNormal(order[index]);
            num_normal_turns[index]--;
            turns_left--;
        }

        }
        // Since the smallest unit count had to place 1, reduce min)
        min--;

    } // Handle the next 'normal' turn.

    // Now, we allocate the 'even' turns, if there are any.
    if ( total_even_turns > 0 ) {
        
        min = Integer.MAX_VALUE;
        for (index = 0; index < orderedItems ; index++) {
        if ( num_even_turns[index] != 0 && num_even_turns[index] < min)
            min = num_even_turns[index];
        }
        
        turns_left = total_even_turns;
        while (turns_left > 0) {
        for (index = 0; index < orderedItems; index++) {
            // If you have no turns here, skip
            if (num_even_turns[index] == 0)
            continue;
            
                    // If you have less than twice the lowest,
                    // move 1.  Otherwise, move more.
            int ntm = num_even_turns[index] / min;
                    for (int j = 0; j < ntm; j++) {
                        turns.addEven(order[index]);
                        num_even_turns[index]--;
                        turns_left--;
                    }
            
                }
                // Since the smallest unit count had to place 1, reduce min)
                min--;

            }  // Handle the next 'even' turn

    } // End have-'even'-turns

    return turns;

    }

}


