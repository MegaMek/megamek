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


import java.util.List;

import megamek.common.ToHitData;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.server.totalWarfare.TWDamageManager;

public abstract class RulesAmmo {

    /**
     * Return the Armor Piercing modifier for crit checks.
     *
     * @param inType The ammo type of the weapon
     * @return the modifier for the crit roll
     */
    public abstract int armorPiercingMod(AmmoType inType);

    public void updateAmmoBVs() {
        List<EquipmentType> equipmentTypes = EquipmentType.allTypes();
        for (EquipmentType equipmentType : equipmentTypes) {
            // Only call the update for ammos which are alternate (they have a base type)
            if (equipmentType instanceof AmmoType && ((AmmoType) equipmentType).getBaseAmmo() != null) {
                ((AmmoType) equipmentType).updateBV();
            }
        }
    }

    /**
     * Armor Piercing Ammo attack Modifier.
     *
     * @param ammoType ammo type of the shot
     * @param toHit    to-hit object
     * @param AP       is it armor piercing
     */
    public abstract void armorPiercingAttackMod(AmmoType.AmmoTypeEnum ammoType,
                                                ToHitData toHit,
                                                boolean AP);

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
     * @param armor  armor value
     * @param mods   modifiers info
     * @param damage base damage
     * @return modified AX missile damage
     */
    public abstract int getAXMissileDamage(int armor,
                                           TWDamageManager.ModsInfo mods,
                                           int damage);

    /**
     * Semi-Guided missiles need special handling.
     *
     * @param modifierValue base modifier
     * @param movementMod   movement modifier applied
     * @param terrainMod    terrain modifier applied
     * @return adjusted semi-guided modifier
     */
    public abstract int getSemiGuidedAdjustment(int modifierValue,
                                                boolean movementMod,
                                                boolean terrainMod);

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
     * @param indirect     whether the attack is indirect
     * @return number of missiles for semi-guided
     */
    public abstract int getSemiGuidedNMissiles(boolean taggedTarget,
                                               boolean indirect);

    /**
     * This exists to return the to-hit modifier for AP ammo. It does not check anything else, it just is the modifier.
     * It calls armorPiercingAttackMod(AmmoTypeEnum, ToHit, AP)
     *
     * @return
     */
    public int armorPiercingAttackMod() {
        ToHitData toHit = new ToHitData();
        armorPiercingAttackMod(AmmoType.AmmoTypeEnum.AC, toHit, true);
        return toHit.getValue();
    }

    /**
     * This is called when alternate ammos need their BV checked. It is called by the child classes as well
     *
     * @param munition AmmoType of the item being considered
     * @return the BV that should be used (default is the base BV)
     */
    public double getAmmoBVAdjusted(final AmmoType munition) {
        if (munition == null) {
            return 0;
        }
        // If there is no base munition, no need to check this
        if (munition.getBaseAmmo() == null) {
            return munition.getBaseBV();
        }

        AmmoType.AmmoTypeEnum ammoTypeEnum = munition.getAmmoType();
        AmmoType baseAmmoType = munition.getBaseAmmo();

        if (((ammoTypeEnum == AmmoType.AmmoTypeEnum.LONG_TOM) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.LONG_TOM_CANNON) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.SNIPER) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.SNIPER_CANNON) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.THUMPER) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.THUMPER_CANNON)) &&
            munition.getMunitionType().contains(AmmoType.Munitions.M_FAE)) {
            return baseAmmoType.getBaseBV() * 1.4;
        }
        if (((ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM_IMP) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.MML) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.NLRM)) &&
            (munition.getMunitionType().contains(AmmoType.Munitions.M_SWARM_I))) {
            return baseAmmoType.getBaseBV() * 1.2;
        }
        if (((ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM_IMP) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.MML) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.SRM) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.SRM_IMP) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.NLRM)) &&
            (munition.getMunitionType().contains(AmmoType.Munitions.M_ARAD))) {
            return baseAmmoType.getBaseBV() * 1.3;
        }

        if (((ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM_IMP) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.MML) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.SRM) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.SRM_IMP) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.NLRM)) &&
            ((munition.getMunitionType().contains(AmmoType.Munitions.M_HEAT_SEEKING)) ||
             (munition.getMunitionType().contains(AmmoType.Munitions.M_FOLLOW_THE_LEADER)))) {
            return baseAmmoType.getBaseBV() * 1.5;
        }

        if (munition.getMunitionType().contains(AmmoType.Munitions.M_FASCAM)) {
            // TO:AR, p.152 and TO:AUE, pp.197,198
            int rackSize = baseAmmoType.getRackSize();
            if (ammoTypeEnum == AmmoType.AmmoTypeEnum.ARROW_IV) {
                rackSize = munition.isClan() ? 30 : 20;
            }
            return rackSize * munition.getShots() / 5.0 * 4;
        }

        if (((ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM_IMP) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.MML) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.NLRM)) &&
            (munition.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_ACTIVE))) {
            // TO:AUE, pp.185,197,198
            return baseAmmoType.getRackSize() * munition.getShots() / 5.0 * 6;
        }

        if (((ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM_IMP) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.MML) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.NLRM)) &&
            (munition.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_AUGMENTED))) {
            // TO:AUE, pp.185,197,198: Half the rack size on 7 hexes; standard mines
            return Math.ceil(baseAmmoType.getRackSize() / 2.0) * 7 * munition.getShots() / 5.0 * 4;
        }

        if (((ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM_IMP) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.MML) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.NLRM)) &&
            (munition.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_INFERNO) ||
             munition.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB))) {
            // TO:AUE, pp.185,197,198
            return baseAmmoType.getRackSize() * munition.getShots();
        }

        if (munition.getMunitionType().contains(AmmoType.Munitions.M_VIBRABOMB_IV)) {
            // TO:AR 152 and TO:AUE 197,198
            return 20 * munition.getShots();
        }

        if (((ammoTypeEnum == AmmoType.AmmoTypeEnum.MML) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.LRM) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.SRM) ||
             (ammoTypeEnum == AmmoType.AmmoTypeEnum.SRM_IMP)) &&
            ((munition.getMunitionType().contains(AmmoType.Munitions.M_TANDEM_CHARGE)))) {
            return baseAmmoType.getBaseBV() * 2.0;
        }

        if (munition.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
            double bv = 0;
            if (ammoTypeEnum == AmmoType.AmmoTypeEnum.MML) {
                if (baseAmmoType.getRackSize() == 3) {
                    bv = 6;
                } else if (baseAmmoType.getRackSize() == 5) {
                    bv = baseAmmoType.hasFlag(AmmoType.F_MML_LRM) ? 9 : 8;
                } else if (baseAmmoType.getRackSize() == 7) {
                    bv = baseAmmoType.hasFlag(AmmoType.F_MML_LRM) ? 12 : 11;
                } else if (baseAmmoType.getRackSize() == 9) {
                    bv = baseAmmoType.hasFlag(AmmoType.F_MML_LRM) ? 17 : 15;
                }
            } else {
                if (baseAmmoType.getRackSize() == 2) {
                    bv = 4;
                } else if (baseAmmoType.getRackSize() == 4) {
                    bv = 7;
                } else if (baseAmmoType.getRackSize() == 5) {
                    bv = 9;
                } else if (baseAmmoType.getRackSize() == 6) {
                    bv = 10;
                } else if (baseAmmoType.getRackSize() == 10) {
                    bv = 17;
                } else if (baseAmmoType.getRackSize() == 15) {
                    bv = 26;
                } else if (baseAmmoType.getRackSize() == 20) {
                    bv = 35;
                }
            }
            return bv;
        }

        if (munition.getMunitionType().contains(AmmoType.Munitions.M_LISTEN_KILL)) {
            double bv = 0;
            if (ammoTypeEnum == AmmoType.AmmoTypeEnum.MML) {
                if (baseAmmoType.getRackSize() == 3) {
                    bv = baseAmmoType.hasFlag(AmmoType.F_MML_LRM) ? 9 : 4;
                } else if (baseAmmoType.getRackSize() == 5) {
                    bv = baseAmmoType.hasFlag(AmmoType.F_MML_LRM) ? 15 : 7;
                } else if (baseAmmoType.getRackSize() == 7) {
                    bv = baseAmmoType.hasFlag(AmmoType.F_MML_LRM) ? 21 : 10;
                } else if (baseAmmoType.getRackSize() == 9) {
                    bv = baseAmmoType.hasFlag(AmmoType.F_MML_LRM) ? 27 : 13;
                }
            } else {
                if (baseAmmoType.getRackSize() == 2) {
                    bv = 6;
                } else if (baseAmmoType.getRackSize() == 4) {
                    bv = 12;
                } else if (baseAmmoType.getRackSize() == 6) {
                    bv = 18;
                } else if (baseAmmoType.getRackSize() == 5) {
                    bv = 7;
                } else if (baseAmmoType.getRackSize() == 10) {
                    bv = 14;
                } else if (baseAmmoType.getRackSize() == 15) {
                    bv = 21;
                } else if (baseAmmoType.getRackSize() == 20) {
                    bv = 28;
                }
            }
        }
        return baseAmmoType.getBaseBV();
    }
}
