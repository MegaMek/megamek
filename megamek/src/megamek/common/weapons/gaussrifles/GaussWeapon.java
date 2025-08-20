/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.gaussrifles;

import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AmmoWeapon;

/**
 * @author Andrew Hunter Created on Oct 19, 2004
 */
public abstract class GaussWeapon extends AmmoWeapon {
    private static final long serialVersionUID = 8640523093316267351L;

    public GaussWeapon() {
        super();
        flags = flags.or(F_MEK_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON).or(F_PROTO_WEAPON)
              .or(F_BALLISTIC).or(F_DIRECT_FIRE).or(F_NO_FIRES);
        explosive = true;
        atClass = CLASS_AC;
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Add modes for powering down Gauss weapons PPC field inhibitors according to TacOps, p.102
        if (gameOptions.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GAUSS_WEAPONS)) {
            addMode("Powered Up");
            addMode("Powered Down");
            setInstantModeSwitch(false);
        } else {
            removeMode("Powered Up");
            removeMode("Powered Down");
        }
    }

}
