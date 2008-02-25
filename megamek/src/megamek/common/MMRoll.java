/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

/*
 * MMRoll.java
 *
 * Created on July 21, 2004, 7:43 AM
 */

package megamek.common;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Subclass of the roll tracker for <code>MMRandom</code> entropy sources.
 * 
 * @author Suvarov454
 */
public class MMRoll extends Roll {

    /**
     * The running total of all of the rolls of each virtual die.
     */
    private int total;

    /**
     * Most rolls are for one or two virtual dice.
     */
    private boolean hasSecond = false;
    private int second;

    /**
     * Sometimes we have more than two virtual dice.
     */
    private Vector<Integer> all = null;

    /**
     * Most tolls use standard six sided dice.
     * 
     * @param rng - the <code>MMRandom</code> that produces random numbers.
     */
    public MMRoll(MMRandom rng) {
        super(6, 1);
        this.total = rng.randomInt(this.faces) + this.min;
    }

    /**
     * Most other rolls have a minimum value of zero.
     * 
     * @param rng - the <code>MMRandom</code> that produces random numbers.
     * @param max - the smallest <code>int</code> value that is higher than
     *            all rolls; all rolls will be in the value set [0, max).
     */
    public MMRoll(MMRandom rng, int max) {
        super(max, 0);
        this.total = rng.randomInt(this.faces) + this.min;
    }

    /**
     * Create a set of virtual dice with the given number of faces that start
     * with the given value.
     * 
     * @param rng - the <code>MMRandom</code> that produces random numbers.
     * @param count - the <code>int</code> number of results possible on each
     *            virtual die.
     * @param start - the <code>int</code> value that is the start of the
     *            value set of each virtual die.
     */
    public MMRoll(MMRandom rng, int count, int start) {
        super(count, start);
        this.total = rng.randomInt(this.faces) + this.min;
    }

    /**
     * Add the result from the given RNG source.
     * 
     * @param rng - the <code>MMRandom</code> that produces random numbers.
     */
    public void addRoll(MMRandom rng) {

        // Store the result for later processing.
        int result = rng.randomInt(this.faces) + this.min;

        // Most rolls only use one or two dice.
        if (!this.hasSecond) {
            this.hasSecond = true;
            this.second = result;
        } else {
            // Allocate the result Vector, if needed.
            if (null == all) {
                all = new Vector<Integer>();
                // Add the first virtual die's roll.
                all.addElement(new Integer(this.total - this.second));
                // Add the second virtual die's roll.
                all.addElement(new Integer(this.second));
            }
            // Add the current virtual die's roll.
            all.addElement(new Integer(result));
        }

        // Add the current virtual die's roll to the running total.
        this.total += result;
    }

    /**
     * Get the value of the roll. This is the total of each of the rolls of each
     * virtual die.
     * 
     * @return the <code>int</code> value of the roll.
     */
    public int getIntValue() {
        return this.total;
    }

    /**
     * Get a <code>String</code> containing the roll for each of the virtual
     * dice.
     * 
     * @return the <code>String</code> value of the roll.
     */
    public String toString() {
        // Build a buffer as we go.
        StringBuffer buffer = new StringBuffer();

        // Start off the report (this is all the report a single die needs).
        buffer.append(this.total);

        // Handle more than two dice.
        if (null != all) {
            Enumeration<Integer> iter = all.elements();
            buffer.append(" (");
            buffer.append(iter.nextElement().toString());
            while (iter.hasMoreElements()) {
                buffer.append(",");
                buffer.append(iter.nextElement().toString());
            }
            buffer.append(")");
        }

        // Handle a pair of dice.
        else if (this.hasSecond) {
            buffer.append(" (");
            buffer.append(this.total - this.second);
            buffer.append("+");
            buffer.append(this.second);
            buffer.append(")");
        }

        // Return the string.
        return buffer.toString();
    }

    /**
     * Get a <code>String</code> report that can be parsed to analyse the
     * roll.
     * 
     * @return the <code>String</code> details of the roll.
     */
    public String getReport() {

        // Build a buffer as we go.
        StringBuffer buffer = new StringBuffer();

        // Include the id.
        buffer.append("Roll #").append(this.id).append(" - range: [").append(
                this.min).append(",").append(this.faces + this.min - 1).append(
                "], result: ").append(this.total);

        // Handle more than two dice.
        if (null != all) {
            Enumeration<Integer> iter = all.elements();
            buffer.append(", rolls: ");
            buffer.append(iter.nextElement().toString());
            while (iter.hasMoreElements()) {
                buffer.append(", ");
                buffer.append(iter.nextElement().toString());
            }
        }

        // Handle a pair of dice.
        else if (this.hasSecond) {
            buffer.append(", rolls: ");
            buffer.append(this.total - this.second);
            buffer.append(", ");
            buffer.append(this.second);
        }

        // Return the string.
        return buffer.toString();
    }

    /**
     * Test harness for this class.
     * 
     * @param args - the array of <code>String</code> arguments: first is the
     *            number of rolls (defaults to two), second is number of sides
     *            (defaults to six sides), third is the starting number
     *            (defaults to one for six sided dice, zero for anything else).
     */
    public static void main(String[] args) {
        MMRandom rng = null;

        // Parse the input.
        int count = 2;
        int sides = 6;
        int start = 1;
        int whichRNG = MMRandom.R_DEFAULT;
        try {
            if (null == args || 0 == args.length) {
                count = 2;
            } else if (1 == args.length) {
                count = Integer.parseInt(args[0]);
            } else if (2 == args.length) {
                count = Integer.parseInt(args[0]);
                sides = Integer.parseInt(args[1]);
                start = 0;
            } else {
                count = Integer.parseInt(args[0]);
                sides = Integer.parseInt(args[1]);
                start = Integer.parseInt(args[2]);
            }

            // Make sure that we got good input.
            if (count < 1) {
                System.err.println("You must specify at least one roll.");
                System.exit(2);
            }
            if (sides < 2) {
                System.err.println("You must specify at least two faces.");
                System.exit(3);
            }
        } catch (NumberFormatException nfe) {
            System.err.println("You must only supply integers.");
            System.err.println(nfe.getMessage());
            System.exit(1);
        }

        // Generate the RNG
        rng = MMRandom.generate(whichRNG);

        // Roll the virtual dice.
        MMRoll roll = new MMRoll(rng, sides, start);
        for (int loop = 1; loop < count; loop++)
            roll.addRoll(rng);

        // Output results.
        Roll.output(roll);

        // Get a second roll.
        MMRoll roll2 = new MMRoll(rng, sides, start);
        for (int loop = 1; loop < count; loop++)
            roll2.addRoll(rng);

        // Output second results.
        Roll.output(roll2);

    }

}
