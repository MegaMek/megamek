package megamek.common.rules;
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

import megamek.common.ToHitData;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;

public abstract class RulesPhysical {

    // Do shields boost punch damage
    public abstract int getShieldDamageBoost(Entity entity, int armLoc);

    // Return the claw to-hit modifier
    public abstract int getClawToHitModifier();

    // Should the shield reset with phase change
    public abstract boolean phaseChangeShield();

    // What is the to-hit modifier for attacking when there is a shield on the arm
    public abstract void getShieldToHitModifier(ToHitData toHit, Entity attacker, Mounted<?> weapon);

    // Can retractable blades be used during punch attacks
    public abstract boolean retractableBladeArmCheck(boolean toRetractableBlake);

    // Does a retractable blade break when used during the punch attack
    public abstract boolean checkRetractableBladeBroke();

    // Does a missed mace attack cause a PSR
    public abstract boolean getMaceMissedPSR();
}
