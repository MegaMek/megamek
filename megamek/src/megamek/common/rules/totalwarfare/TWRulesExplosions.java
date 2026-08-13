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


import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesExplosions;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

import java.util.Vector;

public class TWRulesExplosions extends RulesExplosions {

    /**
     * {@inheritDoc}
     * No special handling
     */
    @Override
    public int explosionDamageReduction(Mek mek, HitData hit, int damage, boolean ammoExplosion,
          Vector<Report> reportVec) {
        return applyCASEIIDamageReduction(mek, hit, damage, ammoExplosion, reportVec);
    }

    /**
     * Determine how much damage will be reduced by CASE II equipment
     *
     * @param entity        Entity that we are damaging
     * @param hit           HitData recording aspects of the incoming damage
     * @param damage        Actual amount of incoming damage
     * @param ammoExplosion Whether damage was caused by an ammo explosion
     * @param reportVec     Vector of Reports containing prior reports; usually modded and returned
     *
     * @return int          total of damage remaining after reduction by CASE II
     */
    @Override
    public int applyCASEIIDamageReduction(Entity entity, HitData hit, int damage, boolean ammoExplosion,
          Vector<Report> reportVec) {
        // Check for CASE II right away. If so, reduce damage to 1 and let it hit the IS. Also, remove as much of the
        // rear armor as allowed by the damage. If arm/leg/head, Then they lose all their armor if it's less than the
        // explosion damage.
        int entityId = entity.getId();
        Report report;

        if (ammoExplosion && entity.hasCASEII(hit.getLocation())) {
            // 1 point of damage goes to IS
            damage--;
            // Remaining damage prevented by CASE II
            report = new Report(6126);
            report.subject = entityId;
            report.add(damage);
            report.indent(3);
            reportVec.addElement(report);
            int loc = hit.getLocation();
            if ((entity instanceof Mek) &&
                  ((loc == Mek.LOC_HEAD) || ((Mek) entity).isArm(loc) || entity.locationIsLeg(loc))) {
                int half = (int) Math.ceil(entity.getOArmor(loc, false) / 2.0);
                if (damage > half) {
                    damage = half;
                }
                if (damage >= entity.getArmor(loc, false)) {
                    // Remember the exact amount of armor damage for PSR purposes
                    damage = entity.getArmor(loc, false);
                    entity.setArmor(IArmorState.ARMOR_DESTROYED, loc, false);
                } else {
                    entity.setArmor(entity.getArmor(loc, false) - damage, loc, false);
                }
            } else {
                if (damage >= entity.getArmor(loc, true)) {
                    // Remember the exact amount of armor damage for PSR purposes
                    damage = entity.getArmor(loc, true);
                    entity.setArmor(IArmorState.ARMOR_DESTROYED, loc, true);
                } else {
                    entity.setArmor(entity.getArmor(loc, true) - damage, loc, true);
                }
            }

            // The armor blown out contributes towards the 20+ PSR
            entity.damageThisPhase += damage;

            if (entity.getInternal(hit) > 0) {
                // Mek takes 1 point of IS damage
                damage = 1;
            } else {
                damage = 0;
            }

            Roll diceRoll = Compute.rollD6(2);
            report = new Report(6127);
            report.subject = entity.getId();
            report.add(diceRoll);
            reportVec.add(report);

            if (diceRoll.getIntValue() >= 8) {
                hit.setEffect(HitData.EFFECT_NO_CRITICAL_SLOTS);
            }
        }

        return damage;
    }

    /**
     * CASE II check crit chance for explosions.
     * CASE II handles this in the CASEII Damage Reduction
     *
     * @param hasCaseII true if the entity has CASE II
     * @param ammoExplosion true if this is an ammo explosion
     * @return the critical hit modifier
     */
    @Override
    public int explosionCASEIImod(boolean hasCaseII, boolean ammoExplosion) {
        return 0;
    }


    /**
     * How much damage to equipment explosions do.
     *
     * @param mounted the mounted equipment
     * @param weaponType the weapon type
     * @return the damage amount
     */
    @Override
    public int equipmentDamage(Mounted<?> mounted, WeaponType weaponType) {
        if (weaponType.hasFlag(WeaponType.F_PPC) && (mounted.hasChargedCapacitor() != 0)) {
            if (mounted.isFired()) {
                if (mounted.hasChargedCapacitor() == 2) {
                    return 15;
                }
                return 0;
            }
            if (mounted.hasChargedCapacitor() == 2) {
                return 30;
            }
            return 15;
        }

        if ((weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.MPOD) && mounted.isFired()) {
            return 0;
        }

        return weaponType.getExplosionDamage();
    }

    /**
     * Pods are explosive if they have not shot yet
     * @param mounted the mounted weapon
     * @return false if it has already shot, otherwise true
     */
    @Override
    public boolean arePodsExplosive(Mounted<?> mounted) {
        if (mounted.getLinked() == null || mounted.getLinked().getUsableShotsLeft() == 0) {
            return false;
        }
        return true;
    }
}
