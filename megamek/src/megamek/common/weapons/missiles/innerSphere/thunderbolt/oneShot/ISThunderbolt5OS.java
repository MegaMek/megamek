/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.missiles.innerSphere.thunderbolt.oneShot;

import megamek.common.weapons.missiles.thuunderbolt.Thunderbolt5Weapon;

/**
 * The One-Shot Thunderbolt 5
 *
 * @author Simon (Juliez)
 */
public class ISThunderbolt5OS extends Thunderbolt5Weapon {

    public ISThunderbolt5OS() {
        super();
        name = "Thunderbolt 5 (OS)";
        setInternalName(name);
        addLookupName("IS OS Thunderbolt-5");
        addLookupName("ISThunderbolt5 (OS)");
        addLookupName("IS Thunderbolt 5 (OS)");
        tonnage = 3.5;
        bv = 13;
        cost = 25000;
        flags = flags.or(F_ONE_SHOT);
    }
}
