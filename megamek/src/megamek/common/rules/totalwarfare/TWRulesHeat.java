package megamek.common.rules.totalwarfare;
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


import megamek.common.CriticalSlot;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.core.CoreRulesHeat;
import megamek.common.units.Mek;

import java.util.ArrayList;

public class TWRulesHeat extends CoreRulesHeat {
    // Attempts to stand generates heat
    public int standingHeat() {
        return 1;
    }

    // Life support damage and heat
    @Override
    public void checkLifeSupportHeat(ArrayList<Integer> heatLimitDamage,
          int damageHeat,
          boolean torsoMountedCockpit,
          boolean mtHeat, boolean bPainShunt) {
        if ((damageHeat >= 47) && mtHeat) {
            // mekwarrior takes 5 damage
            heatLimitDamage.add(47);
            heatLimitDamage.add(5);
        } else if ((damageHeat >= 39) && mtHeat) {
            // mekwarrior takes 4 damage
            heatLimitDamage.add(39);
            heatLimitDamage.add(4);
        } else if ((damageHeat >= 32) && mtHeat) {
            // mekwarrior takes 3 damage
            heatLimitDamage.add(32);
            heatLimitDamage.add(3);
        } else if (damageHeat >= 25) {
            // mekwarrior takes 2 damage
            heatLimitDamage.add(25);
            heatLimitDamage.add(2);
        } else if (damageHeat >= 15) {
            // mekwarrior takes 1 damage
            heatLimitDamage.add(15);
            heatLimitDamage.add(1);
        }
        if ((torsoMountedCockpit) &&
              !bPainShunt) {
            heatLimitDamage.set(1, heatLimitDamage.get(1) + 1);
        }
    }
}
