/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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
 * @author Sebastian Brocks
 */
public class ISThunderBolt10 extends Thunderbolt10Weapon {
    private static final long serialVersionUID = 6781882739979127656L;

    public ISThunderBolt10() {
        super();
        name = "Thunderbolt 10";
        setInternalName(name);
        addLookupName("IS Thunderbolt-10");
        addLookupName("ISThunderbolt10");
        addLookupName("IS Thunderbolt 10");
        addLookupName("ISTBolt10");
        tonnage = 7;
        bv = 127;
        cost = 175000;
    }
}