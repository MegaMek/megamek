/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import java.io.Serializable;

/**
 * Encapsulate all information known about a requested roll. This information
 * can be logged for full statistical analysis and auditing, so hopefully people
 * will <b>finally</b> stop questioning whether the RNG is any good.
 *
 * @author Suvarov454
 * @since July 20, 2004, 4:21 PM
 */
public abstract class Roll implements Serializable {

    /**
     * Make sure that all rolls are uniquely identified.
     */
    private static long nextId = 1;

    /**
     * Get the next unique identifier.
     *
     * @return the next unique <code>long</code> identifier.
     */
    private static long getNextId() {
        return nextId++;
    }

    /**
     * The unique identifier for this roll.
     */
    protected final long id;

    /**
     * The number of faces on each virtual die.
     */
    protected final int faces;

    /**
     * The lowest value on each virtual die.
     */
    protected final int min;

    /**
     * Store the configuration information for this roll.
     *
     * @param count - the <code>int</code> number of results possible on each
     *            virtual die.
     * @param start - the <code>int</code> value that is the start of the
     *            value set of each virtual die.
     */
    protected Roll(int count, int start) {
        this.id = Roll.getNextId();
        this.faces = count;
        this.min = start;
    }

    /**
     * Get the value of the roll. This is the total of each of the rolls of each
     * virtual die.
     *
     * @return the <code>int</code> value of the roll.
     */
    public abstract int getIntValue();

    /**
     * Get a <code>String</code> containing the roll for each of the virtual
     * dice.
     *
     * @return the <code>String</code> value of the roll.
     */
    @Override
    public abstract String toString();

    /**
     * Get a <code>String</code> report that can be parsed to analyse the
     * roll.
     *
     * @return the <code>String</code> details of the roll.
     */
    public abstract String getReport();

    /**
     * Formats <code>Roll</code> output for test harnesses.
     *
     * @param roll - the <code>Roll</code> to be reported.
     */
    protected static void output(Roll roll) {
        System.out.print("The integer total is ");
        System.out.print(roll.getIntValue());
        System.out.println(".");
        System.out.print("The string total is ");
        System.out.print(roll);
        System.out.println(".");
        System.out.print("The string report is ");
        System.out.print(roll.getReport());
        System.out.println(".");
    }

    /**
     * Simple check if the total roll of the virtual die is a success.
     *
     * @return the <code>bool</code> value of the roll, true if success.
     */
    public boolean isTargetRollSuccess(int target) {
        return getIntValue() >= target;
    }

    /**
     * Simple check if the total roll of the virtual die is a success.
     *
     * @return the <code>bool</code> value of the roll, true if success.
     */
    public boolean isTargetRollSuccess(TargetRoll target) {
        return getIntValue() >= target.getValue();
    }

    /**
     * Simple check if the total roll of the virtual die is a success.
     * Automatic failure on a 2.
     *
     * @return the <code>bool</code> value of the roll, true if success.
     */
    public boolean isTargetRollSuccess2isFailure(int target) {
        return (2 < getIntValue()) && (getIntValue() >= target);
    }


    /**
     * Simple check if the total roll of the virtual die is a success.
     * Automatic failure on a 2.
     *
     * @return the <code>bool</code> value of the roll, true if success.
     */
    public boolean isTargetRollSuccess2isFailure(TargetRoll target) {
        return (2 < getIntValue()) && (getIntValue() >= target.getValue());
    }

    /**
     * Get the individual dice values of the roll.
     * @return the <code>int[]</code> array of the individual dice values.
     */
    public abstract int[] getIntValues();


    /**
     * Returns the margin of success/failure of the roll compared to the target roll.
     * @param targetRoll the target roll to compare against
     * @return the margin of success/failure
     */
    public int getMarginOfSuccess(TargetRoll targetRoll) {
        return getIntValue() - targetRoll.getValue();
    }

    /**
     * Determines if the roll is a simple failure.
     * @return true if the roll is a simple failure, false otherwise
     */
    public boolean isSimpleFailure() {
        return getIntValue() == 2;
    }
}
