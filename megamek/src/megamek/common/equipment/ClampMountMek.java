/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org).
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.equipment;

import java.io.Serial;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.battleArmor.BattleArmorHandles;
import megamek.common.units.Entity;

/**
 * Represents the space on a standard Mek (i.e. one that is not an OmniMek) used by Battle Armor squads equipped with
 * Magnetic Clamps to attach themselves for transport. This transporter gets assigned to all of a player's standard Meks
 * in the Exchange Phase if any Battle Armor squad equipped with a Magnetic Clamp is on that player's side.
 */
public class ClampMountMek extends BattleArmorHandles {
    @Serial
    private static final long serialVersionUID = -5687854937528642266L;

    private static final String NO_VACANCY_STRING = "A BA squad with magnetic clamps is loaded";
    private static final String HAVE_VACANCY_STRING = "One BA-MagClamp squad";

    @Override
    public String getUnusedString() {
        return (carriedUnit != Entity.NONE) ? NO_VACANCY_STRING : HAVE_VACANCY_STRING;
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        return getLoadedUnits().size();
    }

    @Override
    public boolean canLoad(Entity unit) {
        return (carriedUnit == Entity.NONE)
              && (unit instanceof BattleArmor)
              && ((BattleArmor) unit).hasMagneticClamps();
    }

    @Override
    public String toString() {
        return "ClampMountMek - troopers:" + carriedUnit;
    }
}
