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

import megamek.common.SimpleTechLevel;

/**
 * The One-Shot Thunderbolt 10
 * @author Simon (Juliez)
 */
public class ISThunderbolt10OS extends Thunderbolt10Weapon {

    public ISThunderbolt10OS() {
        super();
        name = "Thunderbolt 10 (OS)";
        setInternalName(name);
        addLookupName("IS OS Thunderbolt-10");
        addLookupName("ISThunderbolt10 (OS)");
        addLookupName("IS Thunderbolt 10 (OS)");
        addLookupName("ISTBolt10OS");
        tonnage = 7.5;
        bv = 25;
        cost = 87500;
        flags = flags.or(F_ONESHOT);
    }
}