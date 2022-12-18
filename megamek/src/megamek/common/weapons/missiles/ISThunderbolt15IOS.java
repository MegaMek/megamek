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
 * The Improved One-Shot Thunderbolt 15
 * @author Simon (Juliez)
 */
public class ISThunderbolt15IOS extends Thunderbolt15Weapon {

    public ISThunderbolt15IOS() {
        super();
        name = "Thunderbolt-15 (I-OS)";
        setInternalName(name);
        addLookupName("IS IOS Thunderbolt-15");
        addLookupName("ISThunderbolt15 (IOS)");
        addLookupName("IS Thunderbolt 15 (IOS)");
        addLookupName("ISTBolt15IOS");
        tonnage = 10.5;
        bv = 46;
        cost = 260000;
        flags = flags.or(F_ONESHOT);
    }
}
