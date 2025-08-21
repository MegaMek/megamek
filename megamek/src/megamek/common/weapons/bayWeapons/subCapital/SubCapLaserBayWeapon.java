/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.bayWeapons.subCapital;

import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.weapons.bayWeapons.BayWeapon;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class SubCapLaserBayWeapon extends BayWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public SubCapLaserBayWeapon() {
        super();
        // tech levels are a little tricky
        this.name = "Sub-Capital Laser Bay";
        this.setInternalName(EquipmentTypeLookup.SCL_BAY);
        this.heat = 0;
        this.damage = DAMAGE_VARIABLE;
        this.shortRange = 12;
        this.mediumRange = 24;
        this.longRange = 40;
        this.extremeRange = 50;
        this.tonnage = 0.0;
        this.bv = 0;
        this.cost = 0;
        this.flags = flags.or(F_ENERGY);
        this.atClass = CLASS_CAPITAL_LASER;
        this.capital = true;
        this.subCapital = true;
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_SUBCAPITAL;
    }
}
