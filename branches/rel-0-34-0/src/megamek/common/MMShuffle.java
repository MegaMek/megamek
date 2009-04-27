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
 * Created on July 21, 2004, 7:49 AM
 */

package megamek.common;

/**
 * Subclass of the roll tracker for <code>Pool36Random</code> "entropy"
 * sources
 * 
 * @author Suvarov454
 */
public class MMShuffle extends Roll {

    /**
     * Record the two "dice" of this "roll".
     */
    private int one;
    private int two;

    /**
     * Record the order in which this "roll" was delt from its shuffle.
     */
    private int deal;

    /**
     * Record the number of shuffles.
     */
    private long shuffle;

    /**
     * Create a new "roll" for this shuffle.
     * 
     * @param shuffleId - the <code>long</code> id for this shuffle.
     * @param first - the first <code>int</code> "roll"
     * @param second - the second <code>int</code> "roll"
     */
    public MMShuffle(int first, int second) {
        // All shuffles are for 2d6.
        super(6, 1);

        // Record our input.
        this.one = first;
        this.two = second;
        this.shuffle = 0;
    }

    /**
     * Record when this "roll" is delt.
     * 
     * @param delt - the <code>int</code> order of this "roll" in its shuffle
     */
    public void setDeal(int delt) {
        this.deal = delt;
        this.shuffle++;
    }

    /**
     * Get the value of the roll. This is the total of each of the rolls of each
     * virtual die.
     * 
     * @return the <code>int</code> value of the roll.
     */
    public int getIntValue() {
        return this.one + this.two;
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
        buffer.append(this.one + this.two);

        // Add the two "dice".
        buffer.append(" (");
        buffer.append(this.one);
        buffer.append("+");
        buffer.append(this.two);
        buffer.append(")");

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
        buffer.append("Roll #").append(this.id).append(" - range: [").append(1)
                .append(",").append(6).append("], result: ").append(
                        this.one + this.two);

        // Report the two "dice".
        buffer.append(", rolls: ");
        buffer.append(this.one);
        buffer.append(", ");
        buffer.append(this.two);

        // Now report the order of the shuffle.
        buffer.append(", deal #");
        buffer.append(this.deal);
        buffer.append(" of shuffle #");
        buffer.append(this.shuffle);

        // Return the string.
        return buffer.toString();
    }

    /**
     * Test harness for this class.
     */
    public static void main(String[] args) {
        int whichRNG = MMRandom.R_POOL36;
        MMRandom rng = MMRandom.generate(whichRNG);

        // Roll and output the virtual dice.
        Roll.output(rng.d6(2));

        // Get a second roll.
        Roll.output(rng.d6(2));

        // Get a roll of a single die.
        Roll.output(rng.d6(1));

        // Get a second roll of a single die.
        Roll.output(rng.d6());

        // Handle 36 more "rolls".
        for (int loop = 0; loop < 36; loop++)
            Roll.output(rng.d6(2));
    }

}
