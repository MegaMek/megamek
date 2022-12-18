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
 * The One-Shot Thunderbolt 20
 * @author Simon (Juliez)
 */
public class ISThunderbolt20OS extends Thunderbolt20Weapon {

    public ISThunderbolt20OS() {
        super();
        name = "Thunderbolt 20 (OS)";
        setInternalName(name);
        addLookupName("IS OS Thunderbolt-20");
        addLookupName("ISThunderbolt20 (OS)");
        addLookupName("ISTBolt20OS");
        addLookupName("IS Thunderbolt 20 (OS)");
        tonnage = 15.5;
        bv = 61;
        cost = 225000;
    }
}
