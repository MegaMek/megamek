/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.strategicBattleSystems;

import java.util.Arrays;

import megamek.common.alphaStrike.AlphaStrikeElement;

/** Represents the Strategic BattleForce element types (IOps, page 328) */
public enum SBFElementType {

    UNKNOWN, BM, AS, MX, PM, V, BA, CI, MS, LA;

    /** @return the SBF Element Type for the given AS element. */
    public static SBFElementType getUnitType(AlphaStrikeElement element) {
        return switch (element.getASUnitType()) {
            case IM, BM -> BM;
            case PM -> PM;
            case MS -> MS;
            case BA -> BA;
            case CI -> CI;
            case AF, CF, SC -> AS;
            case CV -> V;
            case SV -> element.isAerospaceSV() ? AS : V;
            default -> LA;
        };
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
