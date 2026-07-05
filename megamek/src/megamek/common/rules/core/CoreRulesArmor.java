package megamek.common.rules.core;
/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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


import megamek.common.Report;
import megamek.common.rules.RulesArmor;
import megamek.common.units.Entity;
import megamek.server.totalWarfare.TWDamageManager;

import java.util.Vector;

public class CoreRulesArmor extends RulesArmor {
    // Core rules heat armor halves heat weapon damage. This allows for weapons to be marked for heat.
    // Core rules p.202
    public boolean allowHeatWeapon(boolean heat_weapon) {
        return heat_weapon;
    }

    // Hardened and ABA armor prevent AP
    public boolean allowArmorPiercing(TWDamageManager.ModsInfo mods) {
        if (mods.hardenedArmor || mods.abaArmor) {
            return false;
        }
        return true;
    }

    // Impact armor does not reduce anything. p.201
    public int impactArmorMod() {return 0;}

    // Impact Resistant Armor breach. Does not apply in Core. p.201
    public int impactArmorBreach(Entity entity, Vector<Report> vDesc) {
        return 0;
    }
}
