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


import megamek.client.ui.Messages;
import megamek.common.ToHitData;
import megamek.common.equipment.AmmoType;
import megamek.common.rules.core.CoreRulesAmmo;
import megamek.server.totalWarfare.TWDamageManager;

public class TWRulesAmmo extends CoreRulesAmmo {
    // Return the modifier for armor piercing based on size
    @Override
    public int armorPiercingMod(AmmoType inType) {
        switch (inType.getRackSize()) {
            case 2:
                return -4;
            case 4:
            case 5:
            case 6:
                return -3;
            case 8:
            case 10:
            case 15:
                return -2;
            case 20:
                return -1;
        }
        return 0;
    }

    // Armor Piercing attack modifiers
    @Override
    public void armorPiercingAttackMod(AmmoType.AmmoTypeEnum ammoType, ToHitData toHit, boolean AP) {
        switch (ammoType) {
            case AmmoType.AmmoTypeEnum.AC:
            case AmmoType.AmmoTypeEnum.LAC:
            case AmmoType.AmmoTypeEnum.AC_IMP:
            case AmmoType.AmmoTypeEnum.PAC:
                if (AP) {
                    toHit.addModifier(1, Messages.getString("WeaponAttackAction.ApAmmo"));
                }
        }
    }

    // Do nothing. Not in TW
    @Override
    public void narcHomingTarget(ToHitData toHit) {}

    // Acid (AX) missiles are -2 on the cluster roll.
    @Override
    public int getAXMissileModifier() {return -2;}

    // They only ignore damage reduction, no bonus
    @Override
    public int getAXMissileDamage(int armor, TWDamageManager.ModsInfo mods, int damage) {
        return damage;
    }

    // Semi-guided can eliminate movement modifiers
    @Override
    public int getSemiGuidedAdjustment(int modifierValue, boolean movementMod, boolean terrainMod) {
        // Semi guided eliminates movement modifier
        if (movementMod) {
            return modifierValue;
        }
        return 0;
    }

    // Semi-guided when tag is present does not ignore cover
    @Override
    public boolean semiGuidedIgnoresCover() {
        return false;
    }

    // Semi-guided does not modify number of missiles
    @Override
    public int getSemiGuidedNMissiles(boolean taggedTarget, boolean indirect) {
        return 0;
    }
}
