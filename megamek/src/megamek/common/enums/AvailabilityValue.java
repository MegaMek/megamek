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

// --- Availability Enum ---
public enum AvailabilityValue {
    A(0, "A"), B(1, "B"), C(2, "C"), D(3, "D"), E(4, "E"), F(5, "F"), F_STAR(6, "F*"), X(7, "X");

    private final int index;
    private final String name;
    private static final Map<Integer, AvailabilityValue> INDEX_LOOKUP = new HashMap<>();
    private static final Map<String, AvailabilityValue> NAME_LOOKUP = new HashMap<>();

    static {
        for (AvailabilityValue tr : values()) {
            INDEX_LOOKUP.put(tr.index, tr);
            NAME_LOOKUP.put(tr.name, tr);
        }
    }

    AvailabilityValue(int idx, String name) {
        this.index = idx;
        this.name = name;
    }

    public int getIndex() {return index;}

    public String getName() {return name;}

    public boolean isBetterThan(AvailabilityValue other) {
        return this.index > other.index;
    }

    public boolean isBetterOrEqualThan(AvailabilityValue other) {
        return this.index >= other.index;
    }

    public static AvailabilityValue fromIndex(int idx) {
        AvailabilityValue tr = INDEX_LOOKUP.get(idx);
        if (tr == null) {throw new IllegalArgumentException("Invalid AvailabilityValue index: " + idx);}
        return tr;
    }

    public static AvailabilityValue fromName(String name) {
        AvailabilityValue tr = NAME_LOOKUP.get(name);
        if (tr == null) {throw new IllegalArgumentException("Invalid AvailabilityValue name: " + name);}
        return tr;
    }
}
