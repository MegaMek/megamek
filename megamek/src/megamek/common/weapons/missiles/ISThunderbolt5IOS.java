/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons.missiles;

/**
 * The Improved One-Shot Thunderbolt 5
 * @author Simon (Juliez)
 */
public class ISThunderbolt5IOS extends Thunderbolt5Weapon {

    public ISThunderbolt5IOS() {
        super();
        name = "Thunderbolt 5 (I-OS)";
        setInternalName(name);
        addLookupName("IS IOS Thunderbolt-5");
        addLookupName("ISThunderbolt5 (IOS)");
        addLookupName("IS Thunderbolt 5 (IOS)");
        tonnage = 2.5;
        bv = 13;
        cost = 40000;
        flags = flags.or(F_ONESHOT);
        techAdvancement.setTechRating(RATING_B)
                .setISAdvancement(3056, 3081, 3085, DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC)
                .setISApproximate(false, true, false, false, false);
    }
}