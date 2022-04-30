/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import java.util.Random;

/**
 * Used by Compute to generate random numbers, usually dice rolls. The base
 * class is abstract, having a number of concrete subclasses that it will give
 * using the generate() method.
 * 
 * @author Ben
 * @since April 27, 2003, 11:29 PM
 */
public abstract class MMRandom {
    public static final int R_DEFAULT = 1;

    public static final int R_SUN = 0;
    public static final int R_CRYPTO = 1;
    public static final int R_POOL36 = 2;

    /**
     * Gives you the type asked for, defaulting to SunRandom if there are any
     * errors.
     */
    static MMRandom generate(int type) {
        LogManager.getLogger().info("Generating RNG type #" + type);
        try {
            switch (type) {
                case R_CRYPTO:
                    return new CryptoRandom();
                case R_POOL36:
                    return new Pool36Random();
                case R_SUN:
                default:
                    return new SunRandom();
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to create desired RNG " + type + ", using SunRandom instead.", ex);
            return new SunRandom();
        }
    }

    /**
     * Simulates six-sided die rolls.
     * 
     * @param nDice - the <code>int</code> number of dice to roll. If this
     *            value is less than or equal to zero, an
     *            <code>IllegalArgumentException</code> will be thrown.
     * @return a <code>Roll</code> object containing the roll results.
     * @throws IllegalArgumentException will be thrown if the input is &lt;= 0.
     */
    public Roll d6(int nDice) {
        if (0 >= nDice) {
            throw new IllegalArgumentException("Must ask for a positive number of rolls, not " + nDice);
        }

        // Use the Roll object to record the rolls.
        MMRoll roll = new MMRoll(this, 6, 1);
        for (int i = 1; i < nDice; i++) {
            roll.addRoll(this);
        }
        return roll;
    }

    public Roll d6(int nDice, int keep) {
        if (0 >= nDice) {
            throw new IllegalArgumentException("Must ask for a positive number of rolls, not " + nDice);
        }
        if (keep >= nDice) {
            throw new IllegalArgumentException("the number of dice to keep must be less than the number rolled");
        }
        // Use the Roll object to record the rolls.
        MMRoll roll = new MMRoll(this, 6, 1, keep);
        for (int i = 1; i < nDice; i++) {
            roll.addRoll(this);
        }
        return roll;
    }
    
    /**
     * A single die
     */
    public Roll d6() {
        return d6(1);
    }

    /**
     * Returns a random <code>int</code> in the range from 0 to one less than
     * the supplied max value.
     * 
     * @param maxValue - the smallest <code>int</code> value which will exceed
     *            any random number returned by this method.
     * @return a random <code>int</code> from the value set [0, maxValue).
     */
    public abstract int randomInt(int maxValue);

    /**
     * Returns a random <code>float</code> in the range of 0 to 1
     * @return a random <code>float</code> from the value set [0, 1]
     */
    public abstract float randomFloat();

    /**
     * Uses com.sun.java.util.collections.Random
     */
    static class SunRandom extends MMRandom {
        Random random = new Random();

        @Override
        public int randomInt(int maxValue) {
            return random.nextInt(maxValue);
        }

        @Override
        public float randomFloat() {
            return random.nextFloat();
        }
    }

    /**
     * Uses java.security.SecureRandom
     */
    static class CryptoRandom extends MMRandom {
        java.security.SecureRandom random;

        /**
         * Construct, making a new thread to init the RNG
         */
        public CryptoRandom() throws NoSuchMethodException {
            // hack: just check to see if there's java.util.Random@nextInt(int)
            new java.util.Random().getClass().getMethod("nextInt",
                    new Class[] { Integer.TYPE });

            // all clear, get on with the normal init
            random = new java.security.SecureRandom();

            Thread initRNG = new Thread(() -> random.nextInt(), "Random Number Init (CryptoRandom)");
            initRNG.start();
        }

        @Override
        public int randomInt(int maxValue) {
            return random.nextInt(maxValue);
        }

        @Override
        public float randomFloat() {
            return random.nextFloat();
        }
    }

    /**
     * Behaves like SunRandom for everything but d6(2) calls. Then, it takes
     * numbers from an array of the 36 possible results of two dice, shuffled.
     */
    static class Pool36Random extends SunRandom {
        public static final int NUM_SHUFFLES = 360;

        MMShuffle[] pool = new MMShuffle[36];
        int index = 0;

        public Pool36Random() {
            initPool();
            shufflePool();
        }

        /** Watches for 2 as nDice and then does its special thing. */
        @Override
        public Roll d6(int nDice) {
            if (nDice != 2) {
                return super.d6(nDice);
            }
            // check pool
            if (index >= pool.length) {
                shufflePool();
            }
            // return next pool number
            return pool[index++];
        }

        /** Initializes the dice pool with the possible results of two dice. */
        void initPool() {
            index = 0;
            for (int i = 1; i <= 6; i++) {
                for (int j = 1; j <= 6; j++) {
                    pool[index++] = new MMShuffle(i, j);
                }
            }
        }

        /**
         * Swaps two of the numbers in the pool NUM_SHUFFLES times. Resets the
         * index. Uses the regular RNG to shuffle (OH NO!)
         */
        void shufflePool() {
            MMShuffle temp;
            int src;
            int dest;

            // alakazam!
            for (int i = 0; i < NUM_SHUFFLES; i++) {
                src = this.randomInt(pool.length);
                dest = this.randomInt(pool.length);

                temp = pool[src];
                pool[src] = pool[dest];
                pool[dest] = temp;
            }

            // Label each of the "rolls" with their new deal order.
            for (int j = 0; j < pool.length; j++) {
                pool[j].setDeal(j + 1);
            }

            // reset index
            index = 0;
        }
    }
}
