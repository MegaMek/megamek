/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.units;

import java.io.Serial;
import java.io.Serializable;

/**
 * Per-unit state for the Magnetic Pulse (MP, TO:AUE p.182) and Improved Magnetic Pulse (iATM IMP) missile effects.
 * Holds the effect counters and the heat-accumulation remainders, and derives the to-hit, movement and hostile-ECM
 * modifiers from the number of warheads that have hit the unit.
 * <p>
 * Held by {@link Entity} as a single field; Entity exposes the effects through thin delegating methods, and the various
 * subsystems (to-hit, movement, ECM, heat, unit display) read through that API. This keeps the effect's state and math
 * in one focused, testable place.
 * </p>
 */
public class MagneticPulseState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Warheads per +1 outside heat for the two standard Magnetic Pulse rates (TO:AUE p.182). */
    public static final int LRM_HEAT_DIVISOR = 5;
    public static final int SRM_HEAT_DIVISOR = 3;

    /** iATM IMP effects step once per this many warheads (heat, to-hit, movement, ECM threshold). */
    private static final int IMP_STEP = 3;

    // Standard Magnetic Pulse: rounds of +1 to-hit remaining, plus per-rate heat remainders so heat
    // accumulates exactly across salvos without mixing the LRM (5) and SRM (3) rates.
    private int standardRounds = 0;
    private int lrmHeatRemainder = 0;
    private int srmHeatRemainder = 0;

    // Improved Magnetic Pulse: warheads that hit this turn / the previous turn, plus its heat remainder.
    private int impThisTurn = 0;
    private int impLastTurn = 0;
    private int impHeatRemainder = 0;

    // --- Standard Magnetic Pulse ---

    /**
     * Records a standard MP hit: the +1 to-hit lasts through the End Phase of the following turn. Additional hits while
     * the effect is already active do not extend or restart the window (TO:AUE p.182), so the counter is only started
     * when the unit is not already affected.
     */
    public void applyStandardPulse() {
        if (standardRounds == 0) {
            standardRounds = 2;
        }
    }

    public int getStandardRounds() {
        return standardRounds;
    }

    /**
     * Accumulates standard MP heat, carrying the sub-threshold remainder across salvos in the same turn. The remainder
     * is tracked separately per rate, so an MML firing both modes does not cross the 5- and 3-warhead rates.
     *
     * @param missiles    warheads that hit in this salvo
     * @param heatDivisor warheads per +1 heat ({@link #LRM_HEAT_DIVISOR} or {@link #SRM_HEAT_DIVISOR})
     *
     * @return the outside heat to bank for this salvo
     */
    public int computeStandardHeat(int missiles, int heatDivisor) {
        if (heatDivisor == SRM_HEAT_DIVISOR) {
            int total = missiles + srmHeatRemainder;
            srmHeatRemainder = total % heatDivisor;
            return (total - srmHeatRemainder) / heatDivisor;
        }
        int total = missiles + lrmHeatRemainder;
        lrmHeatRemainder = total % heatDivisor;
        return (total - lrmHeatRemainder) / heatDivisor;
    }

    // --- Improved Magnetic Pulse (iATM IMP) ---

    public void addImpHits(int missiles) {
        impThisTurn += missiles;
    }

    /**
     * @param missiles warheads that hit in this salvo
     *
     * @return the outside heat to bank for an IMP salvo: +1 per 3 warheads, remainder carried across salvos in the same
     *       turn
     */
    public int computeImpHeat(int missiles) {
        int total = missiles + impHeatRemainder;
        impHeatRemainder = total % IMP_STEP;
        return (total - impHeatRemainder) / IMP_STEP;
    }

    /** @return warheads currently affecting the unit (this turn plus the previous turn). */
    public int getActiveImpHits() {
        return impThisTurn + impLastTurn;
    }

    /**
     * @param isProtoMek whether the affected unit is a ProtoMek (higher cap)
     *
     * @return the +to-hit penalty from IMP missiles: +1 per 3 warheads, capped at +2 (or +3 for ProtoMeks). Applies to
     *       fusion and non-fusion units alike.
     */
    public int getImpToHitModifier(boolean isProtoMek) {
        int activeHits = getActiveImpHits();
        if (activeHits <= 0) {
            return 0;
        }
        return Math.min(isProtoMek ? 3 : 2, activeHits / IMP_STEP);
    }

    /**
     * @param isFusion   whether the affected unit is fusion-powered
     * @param isProtoMek whether the affected unit is a ProtoMek (higher cap)
     *
     * @return the Walk/Jump MP reduction from IMP missiles: -1 per 3 warheads, capped at -2 (or -3 for ProtoMeks).
     *       Non-fusion units ignore this reduction.
     */
    public int getImpMpReduction(boolean isFusion, boolean isProtoMek) {
        if (!isFusion) {
            return 0;
        }
        int activeHits = getActiveImpHits();
        if (activeHits <= 0) {
            return 0;
        }
        return Math.min(isProtoMek ? 3 : 2, activeHits / IMP_STEP);
    }

    /** @return {@code true} once at least 3 IMP warheads are affecting the unit (hostile ECM threshold). */
    public boolean isImpEcmAffected() {
        return getActiveImpHits() >= IMP_STEP;
    }

    public int getImpLastTurn() {
        return impLastTurn;
    }

    public void setImpLastTurn(int impLastTurn) {
        this.impLastTurn = impLastTurn;
    }

    // --- Lifecycle ---

    /**
     * Advances the Magnetic Pulse effects one round: decrements the standard MP window and rolls the IMP hit count over
     * so this turn's hits become the previous turn's. Called from {@link Entity#newRound(int)}.
     */
    public void newRound() {
        if (standardRounds > 0) {
            standardRounds--;
        }
        impLastTurn = impThisTurn;
        impThisTurn = 0;
        // Heat remainders accumulate only within a turn, so clear them; otherwise a sub-threshold
        // remainder would carry into the next turn and add heat that turn's hits did not earn.
        lrmHeatRemainder = 0;
        srmHeatRemainder = 0;
        impHeatRemainder = 0;
    }
}
