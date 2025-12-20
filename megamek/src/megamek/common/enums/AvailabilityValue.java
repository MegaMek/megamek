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

import static megamek.common.internationalization.I18n.getTextAt;

import java.util.HashMap;
import java.util.Map;

// --- Availability Enum ---
public enum AvailabilityValue {
    A("A", 0),
    B("B", 1),
    C("C", 2),
    D("D", 3),
    E("E", 4),
    F("F", 5),
    F_STAR("F_STAR", 6),
    X("X", 7);

    private static final String RESOURCE_BUNDLE = "megamek.common.AvailabilityValue";

    private final String lookupName;
    private final int index;
    private final String name;
    private final String description;
    private static final Map<Integer, AvailabilityValue> INDEX_LOOKUP = new HashMap<>();
    private static final Map<String, AvailabilityValue> NAME_LOOKUP = new HashMap<>();

    static {
        for (AvailabilityValue tr : values()) {
            INDEX_LOOKUP.put(tr.index, tr);
            NAME_LOOKUP.put(tr.lookupName, tr);
        }
    }

    AvailabilityValue(String lookupName, int index) {
        this.lookupName = lookupName;
        this.index = index;
        this.name = generateLabel();
        this.description = generateDescription();
    }

    private String generateLabel() {
        return getTextAt(RESOURCE_BUNDLE, "AvailabilityValue." + lookupName + ".label");
    }

    private String generateDescription() {
        return getTextAt(RESOURCE_BUNDLE, "AvailabilityValue." + lookupName + ".description");
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

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

    public static AvailabilityValue fromName(String lookupName) {
        AvailabilityValue availabilityValue = NAME_LOOKUP.get(lookupName);
        if (availabilityValue == null) {
            throw new IllegalArgumentException("Invalid AvailabilityValue name: " + lookupName);
        }

        return availabilityValue;
    }
}
