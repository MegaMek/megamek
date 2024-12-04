/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.modifiers;

import java.util.List;

@SuppressWarnings("unused") // These are for MHQ use for salvage quality Parts
public class EquipmentModifiers {

    /**
     * Creates a weapon jam modifier for salvage quality missile and ballistic weapons, CO p.215. It makes the weapon jam on a to hit roll
     * of 2 or 3.
     *
     * @see WeaponJamModifier
     */
    public static WeaponJamModifier createSalvageQualityWeaponJamModifier() {
        return new WeaponJamModifier(List.of(2, 3), EquipmentModifier.Reason.SALVAGE_QUALITY);
    }

    /**
     * Creates a to-hit modifier that adds a +1 to-hit penalty for salvage quality weapons, CO p.215.
     *
     * @see ToHitModifier
     */
    public static ToHitModifier createSalvageQualityToHitModifier() {
        return new ToHitModifier(1, EquipmentModifier.Reason.SALVAGE_QUALITY);
    }

    /**
     * Creates a weapon modifier that makes the weapon deal 1 less damage for salvage quality ranged weapons, CO p.215.
     *
     * @see DamageModifier
     */
    public static DamageModifier createSalvageQualityRangedDamageModifier() {
        return new DamageModifier(-1, EquipmentModifier.Reason.SALVAGE_QUALITY);
    }

    /**
     * Creates a weapon modifier that makes the weapon deal 2 less damage for salvage quality melee weapons, CO p.215.
     *
     * @see DamageModifier
     */
    public static DamageModifier createSalvageQualityMeleeDamageModifier() {
        return new DamageModifier(-2, EquipmentModifier.Reason.SALVAGE_QUALITY);
    }

    /**
     * Creates a weapon modifier that makes the weapon create 2 extra heat when fired, as applicable to energy weapons, CO p.215.
     *
     * @see HeatModifier
     */
    public static HeatModifier createSalvageQualityHeatModifier() {
        return new HeatModifier(2, EquipmentModifier.Reason.SALVAGE_QUALITY);
    }

    /**
     * Creates a unit modifier that prevents it from torso twisting based on a salvage quality Gyro, CO p.215. Note that this modifier works
     * for all unit types even though it should only be applied to Meks.
     *
     * @see NoTwistModifier
     */
    public static NoTwistModifier createSalvageQualityGyroNoTwistModifier() {
        return new NoTwistModifier(EquipmentModifier.Reason.SALVAGE_QUALITY, SystemModifier.EntitySystem.GYRO);
    }

    /**
     * Creates a unit modifier that prevents it from turret twisting based on salvage quality Controls, CO p.215. Note that this modifier
     * works for all unit types even though it should only be applied to tanks.
     *
     * @see NoTwistModifier
     */
    public static NoTwistModifier createSalvageQualityControlsNoTwistModifier() {
        return new NoTwistModifier(EquipmentModifier.Reason.SALVAGE_QUALITY, SystemModifier.EntitySystem.CONTROLS);
    }
}
