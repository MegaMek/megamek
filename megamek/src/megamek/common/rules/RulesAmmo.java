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
import megamek.common.equipment.AmmoType;
import megamek.server.totalWarfare.TWDamageManager;

public abstract class RulesAmmo {
    /**
     * Return the Armor Piercing modifier for crit checks.
     * @param inType The ammo type of the weapon
     * @return the modifier for the crit roll
     */
    public abstract int armorPiercingMod(AmmoType inType);

    /**
     * Armor Piercing Ammo attack Modifier.
     * 
     * @param ammoType ammo type of the shot
     * @param toHit to-hit object
     * @param AP is it armor piercing
     */
    public abstract void armorPiercingAttackMod(AmmoType.AmmoTypeEnum ammoType, ToHitData toHit, boolean AP);

    /**
     * Does NARC affect the target number.
     *
     * @param toHit to-hit object
     */
    public abstract void narcHomingTarget(ToHitData toHit);

    /**
     * Acid (AX) missiles reduce cluster roll.
     *
     * @return AX missile modifier
     */
    public abstract int getAXMissileModifier();

    /**
     * Acid (AX) missiles damage.
     *
     * @param armor armor value
     * @param mods modifiers info
     * @param damage base damage
     * @return modified AX missile damage
     */
    public abstract int getAXMissileDamage(int armor, TWDamageManager.ModsInfo mods, int damage);

    /**
     * Semi-Guided missiles need special handling.
     *
     * @param modifierValue base modifier
     * @param movementMod movement modifier applied
     * @param terrainMod terrain modifier applied
     * @return adjusted semi-guided modifier
     */
    public abstract int getSemiGuidedAdjustment(int modifierValue, boolean movementMod, boolean terrainMod);

    /**
     * Does semi-guided ignore cover for a tagged entity.
     *
     * @return true if cover is ignored
     */
    public abstract boolean semiGuidedIgnoresCover();

    /**
     * Does the semi-guided impact the number of missiles.
     *
     * @param taggedTarget whether the target is tagged
     * @param indirect whether the attack is indirect
     * @return number of missiles for semi-guided
     */
    public abstract int getSemiGuidedNMissiles(boolean taggedTarget, boolean indirect);

    /**
     * This exists to return the to-hit modifier for AP ammo.
     * It does not check anything else, it just is the modifier.
     * It calls armorPiercingAttackMod(AmmoTypeEnum, ToHit, AP)
     * @return
     */
    public int armorPiercingAttackMod() {
        ToHitData toHit = new ToHitData();
        armorPiercingAttackMod(AmmoType.AmmoTypeEnum.AC, toHit, true);
        return toHit.getValue();
    }
}
