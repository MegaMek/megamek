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
 * The rules category of a Battlefield Support Asset. This is NOT printed on the Asset card, but it must be recorded
 * because some Battlefield Support rules only affect Assets of a particular type.
 * <p>
 * The asset type is deliberately independent of the Asset's movement mode
 * ({@link megamek.common.units.EntityMovementMode}); unusual combinations such as VTOL-movement infantry are permitted
 * by the data model even where rules for them do not yet exist. Combat and Support vehicles are not distinguished -
 * both are {@link #VEHICLE}.
 */
public enum BFSAssetType {
    VEHICLE("Vehicle"),
    CONV_INFANTRY("Conventional Infantry"),
    BATTLE_ARMOR("Battle Armor"),
    EMPLACEMENT("Emplacement");

    private final String displayName;

    BFSAssetType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Parses the given string into a {@link BFSAssetType}, tolerating the enum name, the display name and a handful of
     * common aliases (case-insensitively). Returns {@code null} if no match is found.
     *
     * @param value the string to parse
     *
     * @return the matching type, or {@code null} if unrecognized
     */
    public static @Nullable BFSAssetType fromString(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        for (BFSAssetType type : values()) {
            if (type.name().equalsIgnoreCase(stripped) || type.displayName.equalsIgnoreCase(stripped)) {
                return type;
            }
        }
        return switch (stripped.toLowerCase().replaceAll("[\\s_]", "")) {
            case "tank", "combatvehicle", "supportvehicle" -> VEHICLE;
            case "infantry", "convinfantry", "conventionalinfantry" -> CONV_INFANTRY;
            case "battlearmor", "ba" -> BATTLE_ARMOR;
            case "gunemplacement", "building" -> EMPLACEMENT;
            default -> null;
        };
    }
}
