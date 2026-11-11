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
import java.util.LinkedHashMap;
import java.util.Map;

import megamek.common.board.CubeCoords;

/** Persistent sinking and stranded-deck state, separate from the authored structure (TO:AUE pp.27-29, 40-41). */
public final class MobileStructureNavalState implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private boolean sinking;
    private boolean settled;
    private int startedRound;
    private int lastSinkingRound;
    private int firstSinkingRound;
    private int lastDescentRound = -1;
    private int depthsPerTurn;
    private int lastFootingRound = -1;
    private final Map<CubeCoords, Integer> baseOffsets = new LinkedHashMap<>();
    private final Map<Integer, CubeCoords> strandedVessels = new LinkedHashMap<>();

    public boolean isSinking() { return sinking; }
    public boolean isSettled() { return settled; }
    public int getDepthsPerTurn() { return depthsPerTurn; }
    public boolean sankThisRound(int round) { return sinking && lastDescentRound == round; }
    public void descended(int round) { lastDescentRound = round; }
    public Map<Integer, CubeCoords> getStrandedVessels() { return strandedVessels; }
    public Map<CubeCoords, Integer> getBaseOffsets() { return baseOffsets; }

    public boolean checkFootingThisRound(int round) {
        if (!sankThisRound(round) || lastFootingRound == round) { return false; }
        lastFootingRound = round;
        return true;
    }

    public void beginSinking(int round, int rate) {
        beginSinking(round, rate, false);
    }

    public void beginSinking(int round, int rate, boolean movementPhase) {
        if (!sinking && !settled) {
            sinking = true;
            startedRound = round;
            firstSinkingRound = round + (movementPhase ? 0 : 1);
            lastSinkingRound = firstSinkingRound - 1;
            depthsPerTurn = Math.max(1, rate);
        }
    }

    /** Movement casualties descend at that phase's end; casualties in later phases wait for the next movement. */
    public boolean beginSinkingTurn(int round) {
        int firstRound = firstSinkingRound == 0 ? startedRound + 1 : firstSinkingRound;
        if (!sinking || round < firstRound || round <= lastSinkingRound) {
            return false;
        }
        lastSinkingRound = round;
        return true;
    }

    public void finishSinking() {
        sinking = false;
        settled = true;
    }
}
