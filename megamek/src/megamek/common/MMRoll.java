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

import org.apache.logging.log4j.LogManager;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Subclass of the roll tracker for <code>MMRandom</code> entropy sources.
 * 
 * @author Suvarov454
 * @since July 21, 2004, 7:43 AM
 */
public class MMRoll extends Roll {

    /**
     * The running total of all the rolls of each virtual die.
     */
    private int total;

    /**
     * a vector of the result for each roll of the dice
     */
    private Vector<Integer> all = new Vector<>();

    /**
     * In some cases, we may only keep the highest subset of the total dice
     */
    private int keep = -1;
    
    /**
     * Most tolls use standard six sided dice.
     * 
     * @param rng - the <code>MMRandom</code> that produces random numbers.
     */
    public MMRoll(MMRandom rng) {
        super(6, 1);
        this.total = rng.randomInt(this.faces) + this.min;
        all.addElement(this.total);
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
        all.addElement(this.total);
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
        all.addElement(this.total);
    }
    
   /**
    * Create a set of virtual dice with the given number of faces that start
    * with the given value, where only a subset of the highest will be kept.
    * 
    * @param rng - the <code>MMRandom</code> that produces random numbers.
    * @param count - the <code>int</code> number of results possible on each
    *            virtual die.
    * @param start - the <code>int</code> value that is the start of the
    *            value set of each virtual die.
    * @param keep - the <code>int</code> number of dice to keep from the total rolled
    */
    public MMRoll(MMRandom rng, int count, int start, int keep) {
        super(count, start);
        this.total = rng.randomInt(this.faces) + this.min;
        all.addElement(this.total);
        this.keep = keep;
    }

    /**
     * Add the result from the given RNG source.
     * 
     * @param rng - the <code>MMRandom</code> that produces random numbers.
     */
    public void addRoll(MMRandom rng) {

        // Store the result for later processing.
        int result = rng.randomInt(this.faces) + this.min;

        all.addElement(result);

        // Add the current virtual die's roll to the running total.
        this.total += result;
        
        //if we are only keeping a subset then total will be different
        if (keep != -1 && all.size() >= keep) {
            this.total = 0;
            all.sort(Collections.reverseOrder());
            for (int i = 0; i < keep; i++) {
                this.total += all.get(i);
            }
        }
    }

    /**
     * Get the value of the roll. This is the total of each of the rolls of each
     * virtual die.
     * 
     * @return the <code>int</code> value of the roll.
     */
    @Override
    public int getIntValue() {
        return this.total;
    }

    /**
     * Get a <code>String</code> containing the roll for each of the virtual
     * dice.
     * 
     * @return the <code>String</code> value of the roll.
     */
    @Override
    public String toString() {
        // Build a buffer as we go.
        StringBuffer buffer = new StringBuffer();

        // Start off the report (this is all the report a single die needs).
        buffer.append(this.total);

        // Handle more than one die.
        if (all.size() > 1) {
            Enumeration<Integer> iter = all.elements();
            buffer.append(" (");
            buffer.append(iter.nextElement().toString());
            while (iter.hasMoreElements()) {
                buffer.append(",");
                buffer.append(iter.nextElement().toString());
            }
            buffer.append(")");
        }
        
        if (keep != -1) {
            buffer.append(" [");
            buffer.append(keep);
            buffer.append(" highest]");
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
    @Override
    public String getReport() {

        // Build a buffer as we go.
        StringBuffer buffer = new StringBuffer();

        // Include the id.
        buffer.append("Roll #").append(this.id).append(" - range: [").append(
                this.min).append(",").append(this.faces + this.min - 1).append(
                "], result: ").append(this.total);

        // Handle more than one die.
        if (all.size() > 1) {
            Enumeration<Integer> iter = all.elements();
            buffer.append(", rolls: ");
            buffer.append(iter.nextElement().toString());
            while (iter.hasMoreElements()) {
                buffer.append(", ");
                buffer.append(iter.nextElement().toString());
            }
        }

        if (keep != -1) {
            buffer.append(" (Keep ");
            buffer.append(keep);
            buffer.append( " highest rolls)");
        }
        
        // Return the string.
        return buffer.toString();
    }

    /**
     * FIXME : Convert to actual unit testing
     * Test harness for this class.
     * 
     * @param args - the array of <code>String</code> arguments: first is the
     *            number of rolls (defaults to two), second is number of sides
     *            (defaults to six sides), third is the starting number
     *            (defaults to one for six sided dice, zero for anything else).
     */
    public static void main(String[] args) {
        MMRandom rng;

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
                LogManager.getLogger().error("You must specify at least one roll.");
                System.exit(2);
            } else if (sides < 2) {
                LogManager.getLogger().error("You must specify at least two faces.");
                System.exit(3);
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("You must only supply integers.", ex);
            System.exit(1);
        }

        count = 2;
        
        // Generate the RNG
        rng = MMRandom.generate(whichRNG);

        // Roll the virtual dice.
        MMRoll roll = new MMRoll(rng, sides, start);
        for (int loop = 1; loop < count; loop++) {
            roll.addRoll(rng);
        }

        // Output results.
        Roll.output(roll);

        // Get a second roll.
        MMRoll roll2 = new MMRoll(rng, sides, start);
        for (int loop = 1; loop < count; loop++) {
            roll2.addRoll(rng);
        }

        // Output second results.
        Roll.output(roll2);

    }
}
