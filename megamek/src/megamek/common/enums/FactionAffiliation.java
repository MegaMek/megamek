/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;

// --- Faction Affiliation Enum ---
public enum FactionAffiliation {
    NONE(0, "NONE"),
    IS(1, "IS"),
    CLAN(2, "CLAN"),
    MIXED(3, "MIXED");

    private final int index;
    private final String name;
    private static final Map<Integer, FactionAffiliation> INDEX_LOOKUP = new HashMap<>();

    static {
        for (FactionAffiliation fa : values()) {
            INDEX_LOOKUP.put(fa.index, fa);
        }
    }

    FactionAffiliation(int idx, String name) {
        this.index = idx;
        this.name = name;
    }

    public int getIndex() {return index;}

    public String getName() {return name;}
}
