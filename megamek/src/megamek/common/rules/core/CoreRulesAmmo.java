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

import megamek.common.ToHitData;
import megamek.common.equipment.AmmoType;
import megamek.common.rules.RulesAmmo;
import megamek.server.totalWarfare.TWDamageManager;

public class CoreRulesAmmo extends RulesAmmo {

    /**
     * {@inheritDoc} Different modifiers. Core Rules p.191
     */
    @Override
    public int armorPiercingMod(AmmoType inType) {
        switch (inType.getRackSize()) {
            case 2:
                return -3;
            case 4:
            case 5:
                return -2;
            case 6:
            case 8:
            case 10:
            case 15:
            case 20:
                return -1;
        }
        return 0;
    }

    /**
     * {@inheritDoc} There is no modifier in Core. p.190
     */
    @Override
    public void armorPiercingAttackMod(AmmoType.AmmoTypeEnum ammoType, ToHitData toHit, boolean AP) {
        // Do nothing. there is no attack modifier
    }

    /**
     * {@inheritDoc} Reduce to-hit if the target has a narc pod and not under ECM. Core p.193
     */
    @Override
    public void narcHomingTarget(ToHitData toHit) {
        toHit.addModifier(-1, "target has a narc pod");
    }

    /**
     * {@inheritDoc} Acid (AX) missiles are -1 on the cluster roll. Core p.192
     */
    @Override
    public int getAXMissileModifier() {
        return -1;
    }

    /**
     * {@inheritDoc} Acid (AX) missiles do more damage to some kinds of armor. Core p.192
     */
    @Override
    public int getAXMissileDamage(int armor, TWDamageManager.ModsInfo mods, int damage) {
        if ((mods.ferroLamellorArmor || mods.ballisticArmor || mods.reactiveArmor || mods.reflectiveArmor)
              && armor > 2) {
            damage = 3;
        }

        return damage;
    }

    /**
     * {@inheritDoc} Semi-guided can reduce terrain modifiers. Core p.193
     */
    @Override
    public int getSemiGuidedAdjustment(int modifierValue, boolean movementMod, boolean terrainMod) {
        // Semi guided reduces terrain modifiers by up to 2 (minimum 0)
        if (terrainMod) {
            if (modifierValue >= 2) {
                return 2;
            } else if (modifierValue == 1) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * {@inheritDoc} Semi-guided when tag is present ignores partial cover except from water or buildings. Core p.193
     */
    @Override
    public boolean semiGuidedIgnoresCover() {
        return true;
    }

    /**
     * {@inheritDoc} Semi-guided changes number of missiles depending on tag and direct/indirect. Core p.193
     */
    @Override
    public int getSemiGuidedNMissiles(boolean taggedTarget, boolean indirect) {
        if (!taggedTarget) {
            return -1;
        } else if (!indirect && taggedTarget) {
            return 2;
        }
        return 0;
    }
}
