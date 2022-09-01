/*
 *
 *  * Copyright (c) 26.09.21, 11:15 - The MegaMek Team. All Rights Reserved.
 *  *
 *  * This file is part of MegaMek.
 *  *
 *  * MegaMek is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * MegaMek is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package megamek.common.strategicBattleSystems;

import megamek.common.*;
import megamek.common.alphaStrike.AlphaStrikeElement;

import java.util.Arrays;

/** Represents the Strategic Battleforce element types (IOps, page 328) */
public enum SBFElementType {

    BM, AS, MX, PM, V, BA, CI, MS, LA;

    //TODO: test if both getUnitTypes agree for all types
    /**
     * Returns the SBF Element Type for the given AS element. This relies on
     * the element's movement modes to distinguish SV elements into AS / V.
     */
    public static SBFElementType getUnitType(Entity en) {
        if (en instanceof Mech) {
            return BM;
        } else if (en instanceof Protomech) {
            return PM;
        } else if (en instanceof Tank) {
            return V;
        } else if (en instanceof BattleArmor) {
            return BA;
        } else if (en instanceof Infantry) {
            return CI;
        } else if (en instanceof FixedWingSupport) {
            return AS;
        } else if (en instanceof Jumpship) {
            return LA;
        } else if (en instanceof Dropship) {
            return LA;
        } else if (en instanceof Aero) {
            return AS;
        }
        // TODO: MS when implemented
        return null;
    }

    /**
     * Returns the SBF Element Type for the given AS element. This relies on
     * the element's movement modes to distinguish SV elements into AS / V.
     */
    public static SBFElementType getUnitType(AlphaStrikeElement element) {
        switch (element.getASUnitType()) {
            case IM:
            case BM:
                return BM;
            case PM:
                return PM;
            case MS:
                return MS;
            case BA:
                return BA;
            case CI:
                return CI;
            case AF:
            case CF:
                return AS;
            case CV:
                return V;
            case SV:
                if (element.getMovementModes().contains("a") || element.getMovementModes().contains("k")
                        || element.getMovementModes().contains("i") || element.getMovementModes().contains("p")) {
                    return AS;
                } else {
                    return V;
                }
            default:
                return LA;
        }
    }

    /** Returns true if this SBF Element Type is equal to any of the given Types. */
    public boolean isAnyOf(SBFElementType type, SBFElementType... furtherTypes) {
        return (this == type) || Arrays.stream(furtherTypes).anyMatch(t -> this == t);
    }

    /** Returns true if this SBF Element Type represents a ground unit. */
    public boolean isGround() {
        return isAnyOf(BM, MX, PM, V, BA, CI, MS);
    }

    /** Returns true if this SBF Element Type represents an aerospace unit. */
    public boolean isAerospace() {
        return !isGround();
    }
}
