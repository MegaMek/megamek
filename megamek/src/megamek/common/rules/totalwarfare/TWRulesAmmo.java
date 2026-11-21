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


import megamek.common.ToHitData;
import megamek.common.equipment.AmmoType;
import megamek.common.rules.RulesAmmo;
import megamek.server.totalWarfare.TWDamageManager;

public class TWRulesAmmo extends RulesAmmo {

    /**
     * Do nothing. Not in TW.
     *
     * @param toHit the to-hit data
     */
    @Override
    public void narcHomingTarget(ToHitData toHit) {
    }

    /**
     * Acid (AX) missiles are -2 on the cluster roll.
     *
     * @return the AX missile cluster modifier
     */
    @Override
    public int getAXMissileModifier() {
        return -2;
    }

    /**
     * They only ignore damage reduction, no bonus.
     *
     * @param armor  the armor value
     * @param mods   the modifications info
     * @param damage the damage amount
     * @return the adjusted damage for AX missiles
     */
    @Override
    public int getAXMissileDamage(int armor,
                                  TWDamageManager.ModsInfo mods,
                                  int damage) {
        return damage;
    }

    /**
     * Semi-guided can eliminate movement modifiers.
     *
     * @param modifierValue the modifier value
     * @param movementMod   true if this is a movement modifier
     * @param terrainMod    true if this is a terrain modifier
     * @return the adjusted modifier for semi-guided ammunition
     */
    @Override
    public int getSemiGuidedAdjustment(int modifierValue,
                                       boolean movementMod,
                                       boolean terrainMod) {
        // Semi guided eliminates movement modifier
        if (movementMod) {
            return modifierValue;
        }
        return 0;
    }

    /**
     * Semi-guided when tag is present does not ignore cover.
     *
     * @return true if semi-guided ignores cover
     */
    @Override
    public boolean semiGuidedIgnoresCover() {
        return false;
    }

    /**
     * Semi-guided does not modify number of missiles.
     *
     * @param taggedTarget true if the target is tagged
     * @param indirect     true if the attack is indirect
     * @return the modification to the number of missiles for semi-guided
     */
    @Override
    public int getSemiGuidedNMissiles(boolean taggedTarget,
                                      boolean indirect) {
        return 0;
    }
}
