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
 * The Improved One-Shot Thunderbolt 20
 * @author Simon (Juliez)
 */
public class ISThunderbolt20IOS extends Thunderbolt20Weapon {

    public ISThunderbolt20IOS() {
        super();
        name = "Thunderbolt 20 (I-OS)";
        setInternalName(name);
        addLookupName("IS IOS Thunderbolt-20");
        addLookupName("ISThunderbolt20 (IOS)");
        addLookupName("ISTBolt20IOS");
        addLookupName("IS Thunderbolt 20 (IOS)");
        tonnage = 14.5;
        bv = 61;
        cost = 360000;
        flags = flags.or(F_ONESHOT);
    }
}