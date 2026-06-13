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

public enum Era {
    SL(0),
    SW(1),
    CLAN(2),
    DA(3);

    private final int index;
    private static final Map<Integer, Era> INDEX_LOOKUP = new HashMap<>();

    static {
        for (Era era : values()) {
            INDEX_LOOKUP.put(era.index, era);
        }
    }

    Era(int idx) {this.index = idx;}

    public int getIndex() {return index;}

    public static Era fromIndex(int idx) {
        Era era = INDEX_LOOKUP.get(idx);
        if (era == null) {throw new IllegalArgumentException("Invalid Era index: " + idx);}
        return era;
    }
}
