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
    UNKNOWN("BuildingType.UNKNOWN.text", -1, -1, -1, -1),
    LIGHT("BuildingType.LIGHT.text", 1, 15, 1, 15),
    MEDIUM("BuildingType.MEDIUM.text", 2, 40, 16, 40),
    HEAVY("BuildingType.HEAVY.text", 3, 90, 41, 90),
    HARDENED("BuildingType.HARDENED.text", 4, 120, 91, 150),
    WALL("BuildingType.WALL.text", 5, 120, 91, 150);

    private final String name;
    private final int type;
    private final int defaultCF;
    private final int minimumCF;
    private final int maximumCF;

    BuildingType(String name, int type, int defaultCF, int minimumCF, int maximumCF) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
              MegaMek.getMMOptions().getLocale());
        this.name = resources.getString(name);
        this.type = type;
        this.defaultCF = defaultCF;
        this.minimumCF = minimumCF;
        this.maximumCF = maximumCF;
    }

    /**
     * @return the Construction Factor a hex of this building is assumed to have when the board does not say
     *       otherwise: 15, 40, 90 and 120 for light, medium, heavy and hardened (Total Warfare, p. 168)
     */
    public int getDefaultCF() {
        return defaultCF;
    }

    /**
     * The lowest Construction Factor a building of this type is built with.
     *
     * <p>Together with {@link #getMaximumCF()} this is the band the type covers, from Total Warfare, p. 168
     * (Building Type/Original CF). Damage takes a building below its band without making it a different type of
     * building, so this describes what one is built to, not what it must currently stand at.</p>
     *
     * @return the lowest Construction Factor for this type, or {@code -1} when the type is not known
     */
    public int getMinimumCF() {
        return minimumCF;
    }

    /**
     * The highest Construction Factor a building of this type may be built with.
     *
     * <p>Each type covers a band of Construction Factors rather than a single value - light 1-15, medium 16-40,
     * heavy 41-90 and hardened 91-150 (Total Warfare, p. 168, Building Type/Original CF). A building stronger than
     * its band is not that type of building; it is the next type up. Damage takes a building below its band, so this
     * bounds what a building may be built or repaired to, not what it may currently stand at.</p>
     *
     * <p>Walls are not in that table. They are treated as hardened here, being the sturdiest thing the table
     * describes.</p>
     *
     * @return the highest Construction Factor for this type, or {@code -1} when the type is not known
     */
    public int getMaximumCF() {
        return maximumCF;
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
