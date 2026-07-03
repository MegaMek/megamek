package megamek.common.rules.totalwarfare;
/*
 * Copyright (C) 2026 James Magnan (bmazur@sev.org)
 * Copyright (C) 2004-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.Messages;
import megamek.common.ToHitData;
import megamek.common.equipment.Mounted;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.core.CoreRulesPhysical;
import megamek.common.units.Entity;

public class TwRulesPhysical extends CoreRulesPhysical {

    // Shields do not provide punch damage bonus
    @Override
    public int getShieldDamageBoost(Entity entity, int armLoc) { return 0; }

    // Shields make it harder to shoot
    @Override
    public void getShieldToHitModifier(ToHitData toHit, Entity attacker, Mounted<?> weapon) {
        // time to check passive defense and no defense
        if (attacker.hasLoweredShield(weapon.getLocation(), weapon.isRearMounted())) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.PassiveShield"));
        } else if (attacker.hasNoDefenseShield(weapon.getLocation())) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Shield"));
        }
    }

    // Do claws modify the to-hit number
    @Override
    public int getClawToHitModifier() { return 1; }
    
    // Shields stay for round
    @Override
    public boolean phaseChangeShield() { return false; }

    // Retractable blades are only used if TO option is enabled
    @Override
    public boolean retractableBladeArmCheck(boolean toRetractableBlade) {
        if (toRetractableBlade) { return true; }
        return false;
    }
}
