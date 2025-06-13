/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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
        return Arrays.stream(BuildingType.values()).filter(type -> type.ordinal() == ordinal).findFirst().orElse(UNKNOWN);
    }
    
    @Override
    public String toString() {
        return name;
    }
}
