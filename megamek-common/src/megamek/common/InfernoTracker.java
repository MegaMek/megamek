/**
 * MegaMek - Copyright (C) 2002-2003 Ben Mazur (bmazur@sev.org)
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

/**
 * This class records and defines the effects of hits by Inferno rounds on units
 * and hexes. It does not *apply* the effect, it just defines it. <p/> It makes
 * use of an inner class to define an Inferno round. This inner class should not
 * be directly accessed, but instead refered to by the constants:
 * <code>STANDARD_ROUND</code> and <code>INFERNO_IV_ROUND</code>.
 */
public class InfernoTracker implements Serializable, RoundUpdated {
    // Private helper classes, methods, and attributes.

    /**
     * 
     */
    private static final long serialVersionUID = -5256053831078922473L;

    /**
     * This class defines the effects of a single hit by an Inferno round.
     */
    /* package */static class Inferno implements Serializable {
        /**
         * 
         */
        private static final long serialVersionUID = 1799687411697517801L;
        private int heatPerRound;
        private int burnRoundsPerHit;

        public Inferno() {
            heatPerRound = 6;
            burnRoundsPerHit = 3;
        }

        public Inferno(int heat, int rounds) {
            this.heatPerRound = heat;
            this.burnRoundsPerHit = rounds;
        }

        public int getHeatPerRound() {
            return heatPerRound;
        }

        public int getBurnRoundsPerHit() {
            return burnRoundsPerHit;
        }

    } // End /* package */ class Inferno implements Serializable

    /**
     * The number of turns of standard Inferno burn remaining.
     */
    private int turnsLeftToBurn = 0;

    /**
     * The number of turns of Inferno IV burn remaining.
     */
    private int turnsIVLeftToBurn = 0;

    // Public constants, constructors, and methods.

    /**
     * The hit from a standard Inferno round.
     */
    public static final Inferno STANDARD_ROUND = new Inferno(6, 3);

    /**
     * The hit from a Inferno IV round.
     */
    public static final Inferno INFERNO_IV_ROUND = new Inferno(10, 3);

    /**
     * A single burn turn from a standard Inferno round.
     */
    public static final Inferno STANDARD_TURN = new Inferno(6, 1);

    /**
     * A single burn turn from an Inferno IV round.
     */
    public static final Inferno INFERNO_IV_TURN = new Inferno(10, 1);

    /**
     * Create an empty tracker.
     */
    public InfernoTracker() {
    }

    /**
     * Add the number of hits with the given inferno round to the tracker.
     * 
     * @param round - the <code>Inferno</code> round that hits. If this value
     *            is not <code>STANDARD_ROUND</code> or
     *            <code>INFERNO_IV_ROUND</code>, then an
     *            <code>IllegalArgumentException</code> will be thrown.
     * @param hits - the <code>int</code> number of rounds that hit. If a
     *            negative number is passed, then an
     *            <code>IllegalArgumentException</code> will be thrown.
     */
    public void add(Inferno round, int hits) {
        // Make sure the # of hits is valid.
        if (hits < 0) {
            throw new IllegalArgumentException(
                    "InfernoTracker can't track negative hits. ");
        }

        // Add a number of turns to the appropriate
        // track, based on the round that hit.
        switch (round.getHeatPerRound()) {
            case 6:
                this.turnsLeftToBurn += round.getBurnRoundsPerHit() * hits;
                break;
            case 10:
                this.turnsIVLeftToBurn += round.getBurnRoundsPerHit() * hits;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown Inferno round added to the InfernoTracker.");
        }

    }

    /**
     * Clear the effects of the inferno rounds (the unit got a bath).
     */
    public void clear() {
        this.turnsLeftToBurn = 0;
        this.turnsIVLeftToBurn = 0;
    }

    /**
     * Determine if the unit or hex still has inferno rounds that are burning.
     * 
     * @return <code>true</code> if there are inferno rounds that are still
     *         still burning. <code>false</code> if no inferno rounds have hit
     *         yet, or if they have burned out.
     */
    public boolean isStillBurning() {
        if (turnsLeftToBurn > 0 || turnsIVLeftToBurn > 0) {
            return true;
        }
        return false;
    }

    /**
     * The infernos have burned for a round.
     */
    public void newRound(int roundNumber) {

        // BMRr, pg. 77 makes me think that Inferno IVs
        // burn in parallel with standard rounds.
        if (turnsIVLeftToBurn > 0) {
            turnsIVLeftToBurn--;
        }
        // Decrement the standard rounds' track.
        else if (turnsLeftToBurn > 0) {
            turnsLeftToBurn--;
        }
    }

    /**
     * Determine the total number of turns left for the Infernos to burn.
     * 
     * @return The <code>int</code> number of turns that this unit or hex will
     *         suffer the effects of an Inferno round (either standard <b>or</b>
     *         Inferno IV). This number will be positive when the
     *         <code>isStillBurning()</code> method returns <code>true</code>.
     *         It will not be negative.
     */
    public int getTurnsLeftToBurn() {
        int result = 0;

        // Add the number of standard burn turns to Inferno IV turns.
        result = turnsLeftToBurn + turnsIVLeftToBurn;

        return result;
    }

    /**
     * Determine just the number of turns left for Arrow IV Infernos to burn.
     * 
     * @return The <code>int</code> number of turns that this unit or hex will
     *         suffer the effects of an Inferno IV round (ignore any standard
     *         inferno rounds). This number will be positive when the
     *         <code>isStillBurning()</code> method returns <code>true</code>.
     *         It will not be negative.
     */
    public int getArrowIVTurnsLeftToBurn() {
        int result = 0;

        // Add the number of standard burn turns to Inferno IV turns.
        result = turnsIVLeftToBurn;

        return result;
    }

    /**
     * Determine the number of heat points generated in the current turn.
     * 
     * @return the <code>int</code> number of heat points added this turn.
     *         This value will be positive when <code>isStillBurning()</code>
     *         returns <code>true</code>. It will not be negative.
     */
    public int getHeat() {
        int result = 0;

        // Use Inferno IV heat, if any is burning.
        if (turnsIVLeftToBurn > 0) {
            result = 10;
        }

        // Decrement the standard rounds' track.
        else if (turnsLeftToBurn > 0) {
            result = 6;
        }

        return result;
    }

    public void setTurnsLeftToBurn(int turnsLeftToBurn) {
        this.turnsLeftToBurn = turnsLeftToBurn;
    }

}
