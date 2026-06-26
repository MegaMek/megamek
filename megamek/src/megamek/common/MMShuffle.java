/*

 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2004-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common;

import megamek.common.rolls.Roll;

/**
 * Subclass of the roll tracker for <code>Pool36Random</code> "entropy" sources
 *
 * @author Suvarov454
 * @since July 21, 2004, 7:49 AM
 */
public class MMShuffle extends Roll {

    /**
     * Record the two "dice" of this "roll".
     */
    private final int one;
    private final int two;

    /**
     * Record the order in which this "roll" was dealt from its shuffle.
     */
    private int deal;

    /**
     * Record the number of shuffles.
     */
    private long shuffle;

    /**
     * Create a new "roll" for this shuffle.
     *
     * @param first  - the first <code>int</code> "roll"
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
     * Record when this "roll" is dealt.
     *
     * @param dealt - the <code>int</code> order of this "roll" in its shuffle
     */
    public void setDeal(int dealt) {
        this.deal = dealt;
        this.shuffle++;
    }

    /**
     * Get the value of the roll. This is the total of each of the rolls of each virtual die.
     *
     * @return the <code>int</code> value of the roll.
     */
    @Override
    public int getIntValue() {
        return this.one + this.two;
    }

    /**
     * Get the individual value of each virtual die.
     *
     * @return the <code>int</code> values of the roll.
     */
    @Override
    public int[] getIntValues() {
        return new int[] { this.one, this.two };
    }

    /**
     * Get a <code>String</code> containing the roll for each of the virtual dice.
     *
     * @return the <code>String</code> value of the roll.
     */
    @Override
    public String toString() {
        return (this.one + this.two)
              + " ("
              + this.one
              + "+"
              + this.two
              + ")";
    }

    /**
     * Get a <code>String</code> report that can be parsed to analyse the roll.
     *
     * @return the <code>String</code> details of the roll.
     */
    @Override
    public String getReport() {
        return "Roll #" + this.id + " - range: [" + 1
              + "," + 6 + "], result: "
              + (this.one + this.two)
              + ", rolls: "
              + this.one
              + ", "
              + this.two
              + ", deal #"
              + this.deal
              + " of shuffle #"
              + this.shuffle;
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
        for (int loop = 0; loop < 36; loop++) {
            Roll.output(rng.d6(2));
        }
    }
}
