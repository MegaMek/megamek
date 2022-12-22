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
 * The One-Shot Thunderbolt 15
 * @author Simon (Juliez)
 */
public class ISThunderbolt15OS extends Thunderbolt15Weapon {

    public ISThunderbolt15OS() {
        super();
        name = "Thunderbolt 15 (OS)";
        setInternalName(name);
        addLookupName("IS OS Thunderbolt-15");
        addLookupName("ISThunderbolt15 (OS)");
        addLookupName("IS Thunderbolt 15 (OS)");
        addLookupName("ISTBolt15OS");
        tonnage = 11.5;
        bv = 46;
        cost = 162500;
        flags = flags.or(F_ONESHOT);
    }
}
