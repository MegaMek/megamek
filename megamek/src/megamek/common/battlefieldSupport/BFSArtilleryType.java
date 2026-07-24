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
package megamek.common.battlefieldSupport;

import megamek.common.annotations.Nullable;

/**
 * The artillery piece carried by a Battlefield Support Asset with the Artillery Special. The type is shown on the Asset
 * card as the Artillery Special's parameter (for example {@code Artillery (LT)} for a Long Tom).
 * <p>
 * This enum records the type token; artillery resolution is handled separately from the Asset data model.
 */
public enum BFSArtilleryType {
    THUMPER("T", "Thumper"),
    SNIPER("S", "Sniper"),
    LONG_TOM("LT", "Long Tom");

    private final String code;
    private final String displayName;

    BFSArtilleryType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /** @return the short code shown on the card (for example {@code LT}) */
    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Parses the given string into a {@link BFSArtilleryType}, matching the enum name, the short code, or the display
     * name (case-insensitively, ignoring spaces). Returns {@code null} if no match is found.
     *
     * @param value the string to parse
     *
     * @return the matching type, or {@code null} if unrecognized
     */
    public static @Nullable BFSArtilleryType fromString(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip().replaceAll("\\s", "");
        for (BFSArtilleryType type : values()) {
            if (type.name().replaceAll("_", "").equalsIgnoreCase(normalized)
                  || type.code.equalsIgnoreCase(normalized)
                  || type.displayName.replaceAll("\\s", "").equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }
}
