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
package megamek.common.enums;

import megamek.common.annotations.Nullable;

/**
 * A Manei Domini rank, and the cybernetic allowance that comes with it.
 *
 * <p>Every Manei Domini receives implants; how many and how advanced depends on their rank
 * (<i>Jihad Hot Spots: 3072</i>, pp. 121, 123-124, Rules Annex: Manei Domini Classes / Manei Domini
 * Nomenclature). The ranks run Alpha to Omicron, Alpha being the most junior.</p>
 *
 * <p>Named to avoid a clash with MekHQ's own {@code ManeiDominiRank}, which is persisted in campaign
 * files and cannot be replaced from here. A consumer holding both maps between them by name - the
 * constants are deliberately identical.</p>
 *
 * @see ManeiDominiImplants
 */
public enum ManeiDominiAugmentationRank {
    ALPHA(2, 3, 2),
    BETA(3, 4, 2),
    OMEGA(3, 4, 3),
    TAU(3, 5, 4),
    DELTA(4, 7, 4),
    SIGMA(4, 8, 4),
    OMICRON(6, 10, 5);

    private final int minimumImplants;
    private final int maximumImplants;
    private final int maximumImplantLevel;

    ManeiDominiAugmentationRank(int minimumImplants, int maximumImplants, int maximumImplantLevel) {
        this.minimumImplants = minimumImplants;
        this.maximumImplants = maximumImplants;
        this.maximumImplantLevel = maximumImplantLevel;
    }

    /**
     * @return the fewest implants a warrior of this rank carries
     */
    public int getMinimumImplants() {
        return minimumImplants;
    }

    /**
     * @return the most implants a warrior of this rank carries
     */
    public int getMaximumImplants() {
        return maximumImplants;
    }

    /**
     * @return the highest implant level this rank may be issued
     */
    public int getMaximumImplantLevel() {
        return maximumImplantLevel;
    }

    /**
     * Matches a rank by name, for a consumer bridging its own Manei Domini rank type to this one.
     *
     * @param name the rank name, matched without regard to case
     *
     * @return the matching rank, or {@code null} if the name is not one of these ranks - which
     *       includes the "none" a consumer may use for a warrior who is not Manei Domini at all
     */
    public static @Nullable ManeiDominiAugmentationRank parseFromString(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (ManeiDominiAugmentationRank rank : values()) {
            if (rank.name().equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return null;
    }
}
