/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.Arrays;
import java.util.ResourceBundle;

import megamek.MegaMek;

public enum BuildingType {
    UNKNOWN("BuildingType.UNKNOWN.text", -1, -1),
    LIGHT("BuildingType.LIGHT.text", 1, 15),
    MEDIUM("BuildingType.MEDIUM.text", 2, 40),
    HEAVY("BuildingType.HEAVY.text", 3, 90),
    HARDENED("BuildingType.HARDENED.text", 4, 120),
    WALL("BuildingType.WALL.text", 5, 120);

    private final String name;
    private final int type;
    private final int defaultCF;

    BuildingType(String name, int type, int defaultCF) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
              MegaMek.getMMOptions().getLocale());
        this.name = resources.getString(name);
        this.type = type;
        this.defaultCF = defaultCF;
    }

    public int getDefaultCF() {
        return defaultCF;
    }

    public int getTypeValue() {
        return type;
    }

    public static BuildingType getType(final int ordinal) {
        return Arrays.stream(BuildingType.values())
              .filter(type -> type.ordinal() == ordinal)
              .findFirst()
              .orElse(UNKNOWN);
    }

    @Override
    public String toString() {
        return name;
    }
}
