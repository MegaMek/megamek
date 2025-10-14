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

import megamek.common.enums.Faction;
import megamek.common.enums.TechRating;
import megamek.common.weapons.missiles.thuunderbolt.Thunderbolt10Weapon;

/**
 * The Improved One-Shot Thunderbolt 10
 *
 * @author Simon (Juliez)
 */
public class ISThunderbolt10IOS extends Thunderbolt10Weapon {

    public ISThunderbolt10IOS() {
        super();
        name = "Thunderbolt 10 (I-OS)";
        setInternalName(name);
        addLookupName("IS IOS Thunderbolt-10");
        addLookupName("ISThunderbolt10 (IOS)");
        addLookupName("IS Thunderbolt 10 (IOS)");
        addLookupName("ISTBolt10IOS");
        tonnage = 6.5;
        bv = 25;
        cost = 140000;
        flags = flags.or(F_ONE_SHOT);
        techAdvancement.setTechRating(TechRating.B)
              .setISAdvancement(3056, 3081, 3085, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC)
              .setISApproximate(false, true, false, false, false);
    }
}
