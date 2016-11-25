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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import megamek.common.options.OptionsConstants;

public abstract class TurnOrdered implements ITurnOrdered {

    /**
     *
     */
    private static final long serialVersionUID = 4131468442031773195L;

    private InitiativeRoll initiative = new InitiativeRoll();
    private static ITurnOrdered lastRoundInitWinner = null;

    private transient int turns_other = 0;
    private transient int turns_even = 0;
    private transient HashMap<Integer, Integer> turns_multi = new HashMap<Integer, Integer>();

    // these are special turns for all of the aero units (only used in the
    // movement phase)
    private transient int turns_aero = 0;
    private transient int turns_ss = 0;
    private transient int turns_js = 0;
    private transient int turns_ws = 0;
    private transient int turns_ds = 0;
    private transient int turns_sc = 0;

    /**
     * Return the number of "normal" turns that this item requires. This is
     * normally the sum of multi-unit turns and the other turns.
     * <p/>
     * Subclasses are expected to override this value in order to make the "move
     * even" code work correctly.
     *
     * @return the <code>int</code> number of "normal" turns this item should
     *         take in a phase.
     */
    @Override
    public int getNormalTurns(IGame game) {
        return getMultiTurns(game) + getOtherTurns();
    }

    @Override
    public int getOtherTurns() {
        return turns_other;
    }

    @Override
    public int getEvenTurns() {
        return turns_even;
    }

    @Override
    public int getMultiTurns(IGame game) {
        int turns = 0;
        // turns_multi is transient, so it could be null
        if (turns_multi == null) {
            turns_multi = new HashMap<Integer, Integer>();
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT)) {
            double lanceSize = game.getOptions().intOption(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT_NUMBER);
            Integer numMekMultis = turns_multi.get(GameTurn.CLASS_MECH);
            if (numMekMultis != null) {
                turns += (int) Math.ceil(numMekMultis / lanceSize);
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT)) {
            double lanceSize = game.getOptions().intOption(OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT_NUMBER);
            Integer numTankMultis = turns_multi.get(GameTurn.CLASS_TANK);
            if (numTankMultis != null) {
                turns += (int) Math.ceil(numTankMultis / lanceSize);
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.INIT_PROTOS_MOVE_MULTI)) {
            double lanceSize = game.getOptions().intOption(OptionsConstants.INIT_INF_PROTO_MOVE_MULTI);
            Integer numProtoMultis = turns_multi.get(GameTurn.CLASS_PROTOMECH);
            if (numProtoMultis != null) {
                turns += (int) Math.ceil(numProtoMultis / lanceSize);
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.INIT_INF_MOVE_MULTI)) {
            double lanceSize = game.getOptions().intOption(OptionsConstants.INIT_INF_PROTO_MOVE_MULTI);
            Integer numInfMultis = turns_multi.get(GameTurn.CLASS_INFANTRY);
            if (numInfMultis != null) {
                turns += (int) Math.ceil(numInfMultis / lanceSize);
            }
        }
        return turns;
    }

    @Override
    public int getSpaceStationTurns() {
        return turns_ss;
    }

    @Override
    public int getJumpshipTurns() {
        return turns_js;
    }

    @Override
    public int getWarshipTurns() {
        return turns_ws;
    }

    @Override
    public int getDropshipTurns() {
        return turns_ds;
    }

    @Override
    public int getSmallCraftTurns() {
        return turns_sc;
    }

    @Override
    public int getAeroTurns() {
        return turns_aero;
    }

    @Override
    public void incrementOtherTurns() {
        turns_other++;
    }

    @Override
    public void incrementEvenTurns() {
        turns_even++;
    }

    @Override
    public void incrementMultiTurns(int entityClass) {
        // turns_multi is transient, so it could be null
        if (turns_multi == null) {
            turns_multi = new HashMap<Integer, Integer>();
        }
        Integer classCount = turns_multi.get(entityClass);
        if (classCount == null) {
            turns_multi.put(entityClass, 1);
        } else {
            turns_multi.put(entityClass, classCount + 1);
        }
    }

    @Override
    public void incrementSpaceStationTurns() {
        turns_ss++;
    }

    @Override
    public void incrementJumpshipTurns() {
        turns_js++;
    }

    @Override
    public void incrementWarshipTurns() {
        turns_ws++;
    }

    @Override
    public void incrementDropshipTurns() {
        turns_ds++;
    }

    @Override
    public void incrementSmallCraftTurns() {
        turns_sc++;
    }

    @Override
    public void incrementAeroTurns() {
        turns_aero++;
    }

    @Override
    public void resetOtherTurns() {
        turns_other = 0;
    }

    @Override
    public void resetEvenTurns() {
        turns_even = 0;
    }

    @Override
    public void resetMultiTurns() {
        // turns_multi is transient, so it could be null
        if (turns_multi == null) {
            turns_multi = new HashMap<Integer, Integer>();
        } else {
            turns_multi.clear();
        }
    }

    @Override
    public void resetSpaceStationTurns() {
        turns_ss = 0;
    }

    @Override
    public void resetJumpshipTurns() {
        turns_js = 0;
    }

    @Override
    public void resetWarshipTurns() {
        turns_ws = 0;
    }

    @Override
    public void resetDropshipTurns() {
        turns_ds = 0;
    }

    @Override
    public void resetSmallCraftTurns() {
        turns_sc = 0;
    }

    @Override
    public void resetAeroTurns() {
        turns_aero = 0;
    }

    @Override
    public InitiativeRoll getInitiative() {
        return initiative;
    }

    @Override
    public void setInitiative(InitiativeRoll newRoll) {
        initiative = newRoll;
    }

    /**
     * Clear the initiative of this object.
     */
    @Override
    public void clearInitiative(boolean bUseInitComp) {
        getInitiative().clear();
    }

    public static void rollInitiative(List<? extends ITurnOrdered> v, boolean bUseInitiativeCompensation) {
        // Clear all rolls
        for (ITurnOrdered item : v) {
            item.clearInitiative(bUseInitiativeCompensation);
        }

        rollInitAndResolveTies(v, null, bUseInitiativeCompensation);

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
         * highestInit && v.size() < 3) { System.out.println("-->AUTO REROLL: "
         * + player.getName()); Vector rv = new Vector(); rv.addElement(item);
         * rollInitAndResolveTies(v, rv); } } } }
         */

    }

    /**
     * This takes a vector of TurnOrdered (Teams or Players), rolls initiative,
     * and resolves ties. The second argument is used when a specific teams
     * initiative should be re-rolled.
     * 
     * @param v
     *            A vector of items that need to have turns.
     * @param rerollRequests
     * @param bInitCompBonus
     *            A flag that determines whether initiative compensation bonus
     *            should be used: used to prevent one side getting long init win
     *            streaks
     */
    public static void rollInitAndResolveTies(List<? extends ITurnOrdered> v,
            List<? extends ITurnOrdered> rerollRequests, boolean bInitCompBonus) {
        for (ITurnOrdered item : v) {
            int bonus = 0;
            if (item instanceof Team) {
                bonus = ((Team) item).getTotalInitBonus(bInitCompBonus);
            }
            if (item instanceof Entity) {
                Entity e = (Entity) item;
                bonus = e.game.getTeamForPlayer(e.owner).getTotalInitBonus(false) + e.getCrew().getInitBonus();
            }
            if (rerollRequests == null) { // normal init roll
                item.getInitiative().addRoll(bonus); // add a roll for all
                // teams
            } else {
                // Resolve Tactical Genius (lvl 3) pilot ability
                for (ITurnOrdered rerollItem : rerollRequests) {
                    if (item == rerollItem) { // this is the team re-rolling
                        item.getInitiative().replaceRoll(bonus);
                        break; // each team only needs one reroll
                    }
                }
            }
        }

        // check for ties
        Vector<ITurnOrdered> ties = new Vector<ITurnOrdered>();
        for (ITurnOrdered item : v) {
            ties.removeAllElements();
            ties.addElement(item);
            for (ITurnOrdered other : v) {
                if ((item != other) && item.getInitiative().equals(other.getInitiative())) {
                    ties.addElement(other);
                }
            }
            if (ties.size() > 1) {
                // We want to ignore init compensation here, because it will
                // get dealt with once we're done resolving ties
                rollInitAndResolveTies(ties, null, false);
            }
        }

        // initiative compensation
        if (bInitCompBonus && (v.size() > 0) && (v.get(0) instanceof Team)) {
            final ITurnOrdered comparisonElement = v.get(0);
            int difference = 0;
            ITurnOrdered winningElement = comparisonElement;

            // figure out who won init this round
            for (ITurnOrdered currentElement : v) {
                if (currentElement.getInitiative().compareTo(comparisonElement.getInitiative()) > difference) {
                    difference = currentElement.getInitiative().compareTo(comparisonElement.getInitiative());
                    winningElement = currentElement;
                }
            }

            // set/reset the init comp counters
            ((Team) winningElement).setInitCompensationBonus(0);
            if (lastRoundInitWinner != null) {
                for (ITurnOrdered item : v) {
                    if (!(item.equals(winningElement) || item.equals(lastRoundInitWinner))) {
                        Team team = (Team) item;
                        int newBonus = team.getInitCompensationBonus(bInitCompBonus) + 1;
                        team.setInitCompensationBonus(newBonus);
                    }
                }
            }
            lastRoundInitWinner = winningElement;
        }
    }

    /**
     * This takes a Vector of TurnOrdered and generates a TurnVector.
     */
    public static TurnVectors generateTurnOrder(List<? extends ITurnOrdered> v, IGame game) {
        int[] num_even_turns = new int[v.size()];
        int[] num_normal_turns = new int[v.size()];
        int[] num_space_station_turns = new int[v.size()];
        int[] num_jumpship_turns = new int[v.size()];
        int[] num_warship_turns = new int[v.size()];
        int[] num_dropship_turns = new int[v.size()];
        int[] num_small_craft_turns = new int[v.size()];
        int[] num_aero_turns = new int[v.size()];

        int total_even_turns = 0;
        int total_normal_turns = 0;
        int total_space_station_turns = 0;
        int total_jumpship_turns = 0;
        int total_warship_turns = 0;
        int total_dropship_turns = 0;
        int total_small_craft_turns = 0;
        int total_aero_turns = 0;
        int index;
        ITurnOrdered[] order = new ITurnOrdered[v.size()];
        int orderedItems = 0;

        ArrayList<ITurnOrdered> plist = new ArrayList<ITurnOrdered>(v.size());
        plist.addAll(v);

        Collections.sort(plist, new Comparator<ITurnOrdered>() {
            public int compare(ITurnOrdered o1, ITurnOrdered o2) {
                return o1.getInitiative().compareTo(o2.getInitiative());
            }
        });

        // Walk through the ordered items.
        for (Iterator<ITurnOrdered> i = plist.iterator(); i.hasNext(); orderedItems++) {
            final ITurnOrdered item = i.next();
            order[orderedItems] = item;

            // Track even turns separately from the normal turns.
            num_normal_turns[orderedItems] = item.getNormalTurns(game);
            num_even_turns[orderedItems] = item.getEvenTurns();
            num_space_station_turns[orderedItems] = item.getSpaceStationTurns();
            num_jumpship_turns[orderedItems] = item.getJumpshipTurns();
            num_warship_turns[orderedItems] = item.getWarshipTurns();
            num_dropship_turns[orderedItems] = item.getDropshipTurns();
            num_small_craft_turns[orderedItems] = item.getSmallCraftTurns();
            num_aero_turns[orderedItems] = item.getAeroTurns();

            // Keep a running total.
            total_even_turns += num_even_turns[orderedItems];
            total_normal_turns += num_normal_turns[orderedItems];
            total_space_station_turns += num_space_station_turns[orderedItems];
            total_jumpship_turns += num_jumpship_turns[orderedItems];
            total_warship_turns += num_warship_turns[orderedItems];
            total_dropship_turns += num_dropship_turns[orderedItems];
            total_small_craft_turns += num_small_craft_turns[orderedItems];
            total_aero_turns += num_aero_turns[orderedItems];
        }

        int min;
        int turns_left;
        int ntm;
        int minSS;
        int minJS;
        int minWS;
        int minDS;
        int minSC;
        int minAero;

        // ok first we have to add in the special Aero turns and then go to
        // 'normal' turns which are really just ground turns

        // We will do the 'normal' turns first, and then the 'even' turns.
        min = Integer.MAX_VALUE;
        minSS = Integer.MAX_VALUE;
        minJS = Integer.MAX_VALUE;
        minWS = Integer.MAX_VALUE;
        minDS = Integer.MAX_VALUE;
        minSC = Integer.MAX_VALUE;
        minAero = Integer.MAX_VALUE;
        for (index = 0; index < orderedItems; index++) {
            if ((num_normal_turns[index] != 0) && (num_normal_turns[index] < min)) {
                min = num_normal_turns[index];
            }
            if ((num_space_station_turns[index] != 0) && (num_space_station_turns[index] < minSS)) {
                minSS = num_space_station_turns[index];
            }
            if ((num_jumpship_turns[index] != 0) && (num_jumpship_turns[index] < minJS)) {
                minJS = num_jumpship_turns[index];
            }
            if ((num_warship_turns[index] != 0) && (num_warship_turns[index] < minWS)) {
                minWS = num_warship_turns[index];
            }
            if ((num_dropship_turns[index] != 0) && (num_dropship_turns[index] < minDS)) {
                minDS = num_dropship_turns[index];
            }
            if ((num_small_craft_turns[index] != 0) && (num_small_craft_turns[index] < minSC)) {
                minSC = num_small_craft_turns[index];
            }
            if ((num_aero_turns[index] != 0) && (num_aero_turns[index] < minAero)) {
                minAero = num_aero_turns[index];
            }
        }

        int total_turns = total_normal_turns + total_space_station_turns + total_jumpship_turns + total_warship_turns
                + total_dropship_turns + total_small_craft_turns + total_aero_turns;

        TurnVectors turns = new TurnVectors(total_normal_turns, total_turns, total_space_station_turns,
                total_jumpship_turns, total_warship_turns, total_dropship_turns, total_small_craft_turns,
                total_aero_turns, total_even_turns, min);

        // Allocate the normal turns.
        turns_left = total_normal_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_normal_turns[index] == 0) {
                    continue;
                }

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                if (game.getOptions().booleanOption(OptionsConstants.INIT_FRONT_LOAD_INITIATIVE)) {
                    ntm = (int) Math.ceil(((double) num_normal_turns[index]) / (double) min);
                } else {
                    ntm = num_normal_turns[index] / min;
                }
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
                if ((num_even_turns[index] != 0) && (num_even_turns[index] < min)) {
                    min = num_even_turns[index];
                }
            }

            turns_left = total_even_turns;
            while (turns_left > 0) {
                for (index = 0; index < orderedItems; index++) {
                    // If you have no turns here, skip
                    if (num_even_turns[index] == 0) {
                        continue;
                    }

                    // If you have less than twice the lowest,
                    // move 1. Otherwise, move more.
                    if (game.getOptions().booleanOption(OptionsConstants.INIT_FRONT_LOAD_INITIATIVE)) {
                        ntm = (int) Math.ceil(((double) num_even_turns[index]) / (double) min);
                    } else {
                        ntm = num_even_turns[index] / min;
                    }
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

        // Allocate the space station turns.
        turns_left = total_space_station_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_space_station_turns[index] == 0) {
                    continue;
                }

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                if (game.getOptions().booleanOption(OptionsConstants.INIT_FRONT_LOAD_INITIATIVE)) {
                    ntm = (int) Math.ceil(((double) num_space_station_turns[index]) / (double) minSS);
                } else {
                    ntm = num_space_station_turns[index] / minSS;
                }
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
                if (num_jumpship_turns[index] == 0) {
                    continue;
                }

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                if (game.getOptions().booleanOption(OptionsConstants.INIT_FRONT_LOAD_INITIATIVE)) {
                    ntm = (int) Math.ceil(((double) num_jumpship_turns[index]) / (double) minJS);
                } else {
                    ntm = num_jumpship_turns[index] / minJS;
                }
                for (int j = 0; j < ntm; j++) {
                    turns.addJumpship(order[index]);
                    num_jumpship_turns[index]--;
                    turns_left--;
                }

            }
            // Since the smallest unit count had to place 1, reduce min)
            minJS--;

        } // Handle the next 'jumpship' turn.

        // Allocate the warship turns.
        turns_left = total_warship_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_warship_turns[index] == 0) {
                    continue;
                }

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                if (game.getOptions().booleanOption(OptionsConstants.INIT_FRONT_LOAD_INITIATIVE)) {
                    ntm = (int) Math.ceil(((double) num_warship_turns[index]) / (double) minWS);
                } else {
                    ntm = num_warship_turns[index] / minWS;
                }
                for (int j = 0; j < ntm; j++) {
                    turns.addWarship(order[index]);
                    num_warship_turns[index]--;
                    turns_left--;
                }

            }
            // Since the smallest unit count had to place 1, reduce min)
            minWS--;

        } // Handle the next 'warship' turn.

        // Allocate the dropship turns.
        turns_left = total_dropship_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_dropship_turns[index] == 0) {
                    continue;
                }

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                if (game.getOptions().booleanOption(OptionsConstants.INIT_FRONT_LOAD_INITIATIVE)) {
                    ntm = (int) Math.ceil(((double) num_dropship_turns[index]) / (double) minDS);
                } else {
                    ntm = num_dropship_turns[index] / minDS;
                }
                for (int j = 0; j < ntm; j++) {
                    turns.addDropship(order[index]);
                    num_dropship_turns[index]--;
                    turns_left--;
                }

            }
            // Since the smallest unit count had to place 1, reduce min)
            minDS--;

        } // Handle the next 'dropship' turn.

        // Allocate the small craft turns.
        turns_left = total_small_craft_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_small_craft_turns[index] == 0) {
                    continue;
                }

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                if (game.getOptions().booleanOption(OptionsConstants.INIT_FRONT_LOAD_INITIATIVE)) {
                    ntm = (int) Math.ceil(((double) num_small_craft_turns[index]) / (double) minSC);
                } else {
                    ntm = num_small_craft_turns[index] / minSC;
                }
                for (int j = 0; j < ntm; j++) {
                    turns.addSmallCraft(order[index]);
                    num_small_craft_turns[index]--;
                    turns_left--;
                }

            }
            // Since the smallest unit count had to place 1, reduce min)
            minSC--;

        } // Handle the next 'smal craft' turn.

        // Allocate the aero turns.
        turns_left = total_aero_turns;
        while (turns_left > 0) {
            for (index = 0; index < orderedItems; index++) {
                // If you have no turns here, skip
                if (num_aero_turns[index] == 0) {
                    continue;
                }

                // If you have less than twice the lowest,
                // move 1. Otherwise, move more.
                ntm = num_aero_turns[index] / minAero;
                for (int j = 0; j < ntm; j++) {
                    turns.addAero(order[index]);
                    num_aero_turns[index]--;
                    turns_left--;
                }

            }
            // Since the smallest unit count had to place 1, reduce min)
            minAero--;

        } // Handle the next 'aero' turn.
        return turns;
    }
}
