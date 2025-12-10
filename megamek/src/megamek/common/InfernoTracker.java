/*
 * Copyright (C) 2002-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;
import java.io.Serializable;

import megamek.common.interfaces.RoundUpdated;

/**
 * This class records and defines the effects of hits by Inferno rounds on units and hexes. It does not *apply* the
 * effect, it just defines it. <p> It makes use of an inner class to define an Inferno round. This inner class should
 * not be directly accessed, but instead referred to by the constants:
 * <code>STANDARD_ROUND</code> and <code>INFERNO_IV_ROUND</code>.
 */
public class InfernoTracker implements Serializable, RoundUpdated {
    @Serial
    private static final long serialVersionUID = -5256053831078922473L;

    /**
     * The number of turns of standard Inferno burn remaining.
     */
    private int turnsLeftToBurn = 0;

    /**
     * The number of turns of Inferno IV burn remaining.
     */
    private int turnsIVLeftToBurn = 0;

    /**
     * The hit from a standard Inferno round.
     */
    public static final Inferno STANDARD_ROUND = new Inferno(6, 3);

    /**
     * The hit from an Inferno IV round.
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
     * @param round - the <code>Inferno</code> round that hits. If this value is not <code>STANDARD_ROUND</code> or
     *              <code>INFERNO_IV_ROUND</code>, then an
     *              <code>IllegalArgumentException</code> will be thrown.
     * @param hits  - the <code>int</code> number of rounds that hit. If a negative number is passed, then an
     *              <code>IllegalArgumentException</code> will be thrown.
     */
    public void add(Inferno round, int hits) {
        // Make sure the # of hits is valid.
        if (hits < 0) {
            throw new IllegalArgumentException("InfernoTracker can't track negative hits.");
        }

        // Add a number of turns to the appropriate track, based on the round that hit.
        switch (round.heatPerRound()) {
            case 6:
                this.turnsLeftToBurn += round.burnRoundsPerHit() * hits;
                break;
            case 10:
                this.turnsIVLeftToBurn += round.burnRoundsPerHit() * hits;
                break;
            default:
                throw new IllegalArgumentException("Unknown Inferno round added to the InfernoTracker.");
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
     * @return <code>true</code> if there are inferno rounds that are still burning.
     *       <code>false</code> if no inferno rounds have hit yet, or if they have burned out.
     */
    public boolean isStillBurning() {
        return (turnsLeftToBurn > 0) || (turnsIVLeftToBurn > 0);
    }

    /**
     * The infernos have burned for a round.
     */
    @Override
    public void newRound(int roundNumber) {
        // BMRr, pg. 77 makes me think that Inferno IVs
        // burn in parallel with standard rounds.
        if (turnsIVLeftToBurn > 0) {
            turnsIVLeftToBurn--;
        } else if (turnsLeftToBurn > 0) {
            // Decrement the standard rounds' track.
            turnsLeftToBurn--;
        }
    }

    /**
     * Determine the total number of turns left for the Infernos to burn.
     *
     * @return The <code>int</code> number of turns that this unit or hex will suffer the effects of an Inferno round
     *       (either standard <b>or</b> Inferno IV). This number will be positive when the
     *       <code>isStillBurning()</code> method returns <code>true</code>.
     *       It will not be negative.
     */
    public int getTurnsLeftToBurn() {
        return turnsLeftToBurn + turnsIVLeftToBurn;
    }

    /**
     * Determine just the number of turns left for Arrow IV Infernos to burn.
     *
     * @return The <code>int</code> number of turns that this unit or hex will suffer the effects of an Inferno IV round
     *       (ignore any standard inferno rounds). This number will be positive when the
     *       <code>isStillBurning()</code> method returns <code>true</code>.
     *       It will not be negative.
     */
    public int getArrowIVTurnsLeftToBurn() {
        // Add the number of standard burn turns to Inferno IV turns.
        return turnsIVLeftToBurn;
    }

    /**
     * Determine the number of heat points generated in the current turn.
     *
     * @return the <code>int</code> number of heat points added this turn. This value will be positive when
     *       <code>isStillBurning()</code> returns <code>true</code>. It will not be negative.
     */
    public int getHeat() {
        int result = 0;

        // Use Inferno IV heat, if any is burning.
        if (turnsIVLeftToBurn > 0) {
            result = 10;
        } else if (turnsLeftToBurn > 0) {
            // Decrement the standard rounds' track.
            result = 6;
        }

        return result;
    }

    public void setTurnsLeftToBurn(int turnsLeftToBurn) {
        this.turnsLeftToBurn = turnsLeftToBurn;
    }
}
