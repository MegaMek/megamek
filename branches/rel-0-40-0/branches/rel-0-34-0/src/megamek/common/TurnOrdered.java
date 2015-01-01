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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

public abstract class TurnOrdered implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4131468442031773195L;

    private InitiativeRoll initiative = new InitiativeRoll();

    private transient int turns_other = 0;
    private transient int turns_even = 0;
    private transient int turns_multi = 0;
    
    //Try adding in special kinds of turns for various Aero types
    private transient int turns_ss = 0;
    private transient int turns_js = 0;
    private transient int turns_ws = 0;
    private transient int turns_ds = 0;
    private transient int turns_sc = 0;

    /**
     * Return the number of "normal" turns that this item requires. This is
     * normally the sum of multi-unit turns and the other turns. <p/> Subclasses
     * are expected to override this value in order to make the "move even" code
     * work correctly.
     * 
     * @return the <code>int</code> number of "normal" turns this item should
     *         take in a phase.
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
        
        int turns = 0;
        
        if ( game.getOptions().booleanOption("vehicle_lance_movement") ) {
            turns += game.getOptions().intOption("vehicle_lance_movement_number");
        }
        
        if ( game.getOptions().booleanOption("protos_move_multi") || game.getOptions().booleanOption("inf_move_multi") ) {
            turns += game.getOptions().intOption("inf_proto_move_multi");
        }
        return (int) Math.ceil(((double) turns_multi)/ (double)turns); 
    }
    
    public int getSpaceStationTurns() {
        return turns_ss;
    }
    
    public int getJumpshipTurns() {
        return turns_js;
    }
    
    public int getWarshipTurns() {
        return turns_ws;
    }
    
    public int getDropshipTurns() {
        return turns_ds;
    }
    
    public int getSmallCraftTurns() {
        return turns_sc;
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
    
    public void incrementSpaceStationTurns() {
        turns_ss++;
    }
    
    public void incrementJumpshipTurns() {
        turns_js++;
    }
    
    public void incrementWarshipTurns() {
        turns_ws++;
    }
    
    public void incrementDropshipTurns() {
        turns_ds++;
    }
    
    public void incrementSmallCraftTurns() {
        turns_sc++;
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
    
    public void resetSpaceStationTurns() {
        turns_ss = 0;
    }
    
    public void resetJumpshipTurns() {
        turns_js = 0;
    }
    
    public void resetWarshipTurns() {
        turns_ws = 0;
    }
    
    public void resetDropshipTurns() {
        turns_ds = 0;
    }
    
    public void resetSmallCraftTurns() {
        turns_sc = 0;
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

    public static void rollInitiative(Vector<? extends TurnOrdered> v) {
        // Clear all rolls
        for (Enumeration<? extends TurnOrdered> i = v.elements(); i
                .hasMoreElements();) {
            final TurnOrdered item = i.nextElement();
            item.clearInitiative();
        }

        rollInitAndResolveTies(v, null);

        // This is the *auto-reroll* code for the Tactical Genius (lvl 3)
        // pilot ability. It is NOT CURRENTLY IMPLEMENTED. This code may
        // be incomplete/buggy/just plain wrong.
        // TODO : fix me
        /**
         * if (v.firstElement() instanceof Team) { //find highest init roll int
         * highestInit = 2; for (Enumeration i = v.elements();
         * i.hasMoreElements();) { final TurnOrdered item =
         * (TurnOrdered)i.nextElement(); highestInit =
         * Math.max(item.getInitiative().getRoll(item.getInitiative().size() -
         * 1), highestInit); } System.out.println("\n\n--->HIGH INIT ROLL: " +
         * highestInit); //loop through teams for (Enumeration i = v.elements();
         * i.hasMoreElements();) { final TurnOrdered item =
         * (TurnOrdered)i.nextElement(); //loop through players for (Enumeration
         * j = ((Team)item).getPlayers(); j.hasMoreElements();) { final Player
         * player = (Player)j.nextElement(); if
         * (player.getGame().hasTacticalGenius(player) &&
         * item.getInitiative().getRoll(item.getInitiative().size() - 1) <
         * highestInit && v.size() < 3) { System.out.println("-->AUTO REROLL: " +
         * player.getName()); Vector rv = new Vector(); rv.addElement(item);
         * rollInitAndResolveTies(v, rv); } } } }
         */

    }

    /**
     * This takes a vector of TurnOrdered (Teams or Players), rolls initiative,
     * and resolves ties. The second argument is used when a specific teams
     * initiative should be re-rolled.
     */
    public static void rollInitAndResolveTies(Vector<? extends TurnOrdered> v,
            Vector<? extends TurnOrdered> rerollRequests) {
        for (Enumeration<? extends TurnOrdered> i = v.elements(); i
                .hasMoreElements();) {
            final TurnOrdered item = i.nextElement();
            int bonus = 0;
            if (item instanceof Team) {
                bonus = ((Team) item).getTotalInitBonus();
            }
            if (item instanceof Entity) {
                Entity e = (Entity) item;
                bonus = e.game.getTeamForPlayer(e.owner).getTotalInitBonus() + e.getCrew().getInitBonus();
            }
            if (rerollRequests == null) { // normal init roll
                item.getInitiative().addRoll(bonus); // add a roll for all
                                                        // teams
            } else {
                // Resolve Tactical Genius (lvl 3) pilot ability
                for (Enumeration<? extends TurnOrdered> j = rerollRequests
                        .elements(); j.hasMoreElements();) {
                    final TurnOrdered rerollItem = j.nextElement();
                    if (item == rerollItem) { // this is the team re-rolling
                        item.getInitiative().replaceRoll(bonus);
                        break; // each team only needs one reroll
                    }
                }
            }
        }

        // check for ties
        Vector<TurnOrdered> ties = new Vector<TurnOrdered>();
        for (Enumeration<? extends TurnOrdered> i = v.elements(); i
                .hasMoreElements();) {
            final TurnOrdered item = i.nextElement();
            ties.removeAllElements();
            ties.addElement(item);
            for (Enumeration<? extends TurnOrdered> j = v.elements(); j
                    .hasMoreElements();) {
                final TurnOrdered other = j.nextElement();
                if (item != other
                        && item.getInitiative().equals(other.getInitiative())) {
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
    public static TurnVectors generateTurnOrder(
            Vector<? extends TurnOrdered> v, IGame game) {
        int[] num_even_turns = new int[v.size()];
        int[] num_normal_turns = new int[v.size()];
        int[] num_space_station_turns = new int[v.size()];
        int[] num_jumpship_turns = new int[v.size()];
        int[] num_warship_turns = new int[v.size()];
        int[] num_dropship_turns = new int[v.size()];
        int[] num_small_craft_turns = new int[v.size()];

        int total_even_turns = 0;
        int total_normal_turns = 0;
        int total_space_station_turns = 0;
        int total_jumpship_turns = 0;
        int total_warship_turns = 0;
        int total_dropship_turns = 0;
        int total_small_craft_turns = 0;
        int index;
        TurnOrdered[] order = new TurnOrdered[v.size()];
        int orderedItems = 0;

        ArrayList<TurnOrdered> plist = new ArrayList<TurnOrdered>(v.size());
        plist.addAll(v);

        Collections.sort(plist, new Comparator<TurnOrdered>() {
            public int compare(TurnOrdered o1, TurnOrdered o2) {
                return o1.getInitiative().compareTo(o2.getInitiative());
            }
        });

        // Walk through the ordered items.
        for (Iterator<TurnOrdered> i = plist.iterator(); i.hasNext(); orderedItems++) {
            final TurnOrdered item = i.next();
            order[orderedItems] = item;

            // Track even turns separately from the normal turns.
            num_normal_turns[orderedItems] = item.getNormalTurns(game);
            num_even_turns[orderedItems] = item.getEvenTurns();
            num_space_station_turns[orderedItems] = item.getSpaceStationTurns();
            num_jumpship_turns[orderedItems] = item.getJumpshipTurns();
            num_warship_turns[orderedItems] = item.getWarshipTurns();
            num_dropship_turns[orderedItems] = item.getDropshipTurns();
            num_small_craft_turns[orderedItems] = item.getSmallCraftTurns();
      
            // Keep a running total.
            total_even_turns += num_even_turns[orderedItems];
            total_normal_turns += num_normal_turns[orderedItems];
            total_space_station_turns += num_space_station_turns[orderedItems];
            total_jumpship_turns += num_jumpship_turns[orderedItems];
            total_warship_turns += num_warship_turns[orderedItems];
            total_dropship_turns += num_dropship_turns[orderedItems];
            total_small_craft_turns += num_small_craft_turns[orderedItems];
        }

        int min;
        int turns_left;
        int minSS;
        int minJS;
        int minWS;
        int minDS;
        int minSC;
        
        //ok first we have to add in the special Aero turns and then go to 
        //'normal' turns (which include fighters)  
        
        // We will do the 'normal' turns first, and then the 'even' turns.
        min = Integer.MAX_VALUE;
        minSS = Integer.MAX_VALUE;
        minJS = Integer.MAX_VALUE;
        minWS = Integer.MAX_VALUE;
        minDS = Integer.MAX_VALUE;
        minSC = Integer.MAX_VALUE;
        for (index = 0; index < orderedItems; index++) {
            if (num_normal_turns[index] != 0 && num_normal_turns[index] < min)
                min = num_normal_turns[index];
            if (num_space_station_turns[index] != 0 && num_space_station_turns[index] < minSS)
                minSS = num_space_station_turns[index];
            if (num_jumpship_turns[index] != 0 && num_jumpship_turns[index] < minJS)
                minJS = num_jumpship_turns[index];
            if (num_warship_turns[index] != 0 && num_warship_turns[index] < minWS)
                minWS = num_warship_turns[index];
            if (num_dropship_turns[index] != 0 && num_dropship_turns[index] < minDS)
                minDS = num_dropship_turns[index];
            if (num_small_craft_turns[index] != 0 && num_small_craft_turns[index] < minSC)
                minSC = num_small_craft_turns[index];
        }

        int total_turns = total_normal_turns + total_space_station_turns + total_jumpship_turns 
                          + total_warship_turns + total_dropship_turns + total_small_craft_turns;
        
        TurnVectors turns = new TurnVectors(total_normal_turns, total_turns,
                total_space_station_turns,
                total_jumpship_turns, total_warship_turns, total_dropship_turns, 
                total_small_craft_turns,
                total_even_turns, min);

        // Allocate the space station turns.
        turns_left = total_space_station_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_space_station_turns[index] == 0)
                    continue;

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                int ntm = num_space_station_turns[index] / minSS;
                for (int j = 0; j < ntm; j++) {
                    turns.addSpaceStation(order[index]);
                    num_space_station_turns[index]--;
                    turns_left--;
                }

            }
            // Since the smallest unit count had to place 1, reduce min)
            minSS--;

        } // Handle the next 'space station' turn.
        
        // Allocate the jumpship turns.
        turns_left = total_jumpship_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_jumpship_turns[index] == 0)
                    continue;

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                int ntm = num_jumpship_turns[index] / minJS;
                for (int j = 0; j < ntm; j++) {
                    turns.addJumpship(order[index]);
                    num_jumpship_turns[index]--;
                    turns_left--;
                }

            }
            // Since the smallest unit count had to place 1, reduce min)
            minJS--;

        } // Handle the next 'jumpship' turn.
        
        //Allocate the warship turns.
        turns_left = total_warship_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_warship_turns[index] == 0)
                    continue;

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                int ntm = num_warship_turns[index] / minWS;
                for (int j = 0; j < ntm; j++) {
                    turns.addWarship(order[index]);
                    num_warship_turns[index]--;
                    turns_left--;
                }

            }
            // Since the smallest unit count had to place 1, reduce min)
            minWS--;

        } // Handle the next 'warship' turn.
        
        //Allocate the dropship turns.
        turns_left = total_dropship_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_dropship_turns[index] == 0)
                    continue;

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                int ntm = num_dropship_turns[index] / minDS;
                for (int j = 0; j < ntm; j++) {
                    turns.addDropship(order[index]);
                    num_dropship_turns[index]--;
                    turns_left--;
                }

            }
            // Since the smallest unit count had to place 1, reduce min)
            minDS--;

        } // Handle the next 'dropship' turn.
        
        //Allocate the small craft turns.
        turns_left = total_small_craft_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_small_craft_turns[index] == 0)
                    continue;

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                int ntm = num_small_craft_turns[index] / minSC;
                for (int j = 0; j < ntm; j++) {
                    turns.addSmallCraft(order[index]);
                    num_small_craft_turns[index]--;
                    turns_left--;
                }

            }
            // Since the smallest unit count had to place 1, reduce min)
            minSC--;

        } // Handle the next 'smal craft' turn.
        
        
        // Allocate the normal turns.
        turns_left = total_normal_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_normal_turns[index] == 0)
                    continue;

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                int ntm = num_normal_turns[index] / min;
                for (int j = 0; j < ntm; j++) {
                    turns.addNormal(order[index]);
                    num_normal_turns[index]--;
                    turns_left--;
                }

            }
            // Since the smallest unit count had to place 1, reduce min)
            min--;

        } // Handle the next 'regular' turn.

        // Now, we allocate the 'even' turns, if there are any.
        if (total_even_turns > 0) {

            min = Integer.MAX_VALUE;
            for (index = 0; index < orderedItems; index++) {
                if (num_even_turns[index] != 0 && num_even_turns[index] < min)
                    min = num_even_turns[index];
            }

            turns_left = total_even_turns;
            while (turns_left > 0) {
                for (index = 0; index < orderedItems; index++) {
                    // If you have no turns here, skip
                    if (num_even_turns[index] == 0)
                        continue;

                    // If you have less than twice the lowest,
                    // move 1. Otherwise, move more.
                    int ntm = num_even_turns[index] / min;
                    for (int j = 0; j < ntm; j++) {
                        turns.addEven(order[index]);
                        num_even_turns[index]--;
                        turns_left--;
                    }
                }
                // Since the smallest unit count had to place 1, reduce min)
                min--;
            } // Handle the next 'even' turn
        } // End have-'even'-turns
        return turns;
    }
}
