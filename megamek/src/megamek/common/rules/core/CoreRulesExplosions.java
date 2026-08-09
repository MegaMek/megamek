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

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesExplosions;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;

import java.util.Vector;

public class CoreRulesExplosions extends RulesExplosions {
    /**
     * {@inheritDoc}
     * Damage reduction for CASE, CASE II, explosions
     */
    @Override
    public int explosionDamageReduction(Mek mek, HitData hit, int damage, boolean ammoExplosion,
          Vector<Report> reportVec) {
            if (!ammoExplosion) {
                return damage;
            }

            int loc = hit.getLocation();

            boolean cased = mek.locationHasCase(hit.getLocation());
            boolean caseIId = mek.hasCASEII(hit.getLocation());

            Report report;

            int cap = caseIId ? 1 : cased ? 10 : 20;
            if (damage < cap) {
                return damage;
            }

            report = new Report(caseIId ? 6134 : cased ? 6133 : 6132);
            report.subject = mek.getId();
            report.indent(3);
            report.add(damage);
            reportVec.addElement(report);

            // Torso locations damage out the rear armor
            boolean torso = mek.locationIsTorso(loc);

            if (mek.getInternal(loc) > cap) { // Location survives, blow out armor
                int armorDamage = damage - cap;
                int armorDamageCap = 10;
                if ((caseIId || cased) &&
                      (mek.getArmor(loc, torso) > armorDamage ||
                            mek.getArmor(loc,torso) > armorDamageCap)) {
                    if (armorDamage > armorDamageCap) {
                        armorDamage = armorDamageCap;
                    }
                    if (mek.getArmor(loc, torso) < armorDamage) {
                        armorDamage = mek.getArmor(loc, torso);
                    }
                    mek.setArmor(mek.getArmor(loc, torso) - armorDamage, loc, torso);
                } else {
                    armorDamage = mek.getArmor(loc, torso);
                    mek.setArmor(IArmorState.ARMOR_DESTROYED, loc, torso);
                }

                mek.damageThisPhase += armorDamage;
                report = new Report(6131);
                report.subject = mek.getId();
                report.indent(3);
                report.add((torso ? "Rear " : "") + mek.getLocationAbbr(loc));
                report.add(armorDamage);
                reportVec.addElement(report);
            }

            return cap;
    }

    /**
     * {@inheritDoc}
     * Determine how much damage will be reduced by CASE II equipment
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
            int armorDamage = damage;
            int armorDamageCap = 10;

            if (armorDamage > armorDamageCap) {
                armorDamage = armorDamageCap;
            }

            if (entity instanceof Mek) {
                boolean torso = ((Mek) entity).locationIsTorso(loc);
                if (entity.getArmor(loc, torso) > armorDamage) {
                    entity.setArmor(entity.getArmor(loc, torso) - armorDamage, loc, torso);
                } else {
                    armorDamage = entity.getArmor(loc, torso);
                    entity.setArmor(IArmorState.ARMOR_DESTROYED, loc, torso);
                }
            } else {
                // Only used if it is not a mek.
                if (armorDamage >= entity.getArmor(loc, true)) {
                    // Remember the exact amount of armor damage for PSR purposes
                    armorDamage = entity.getArmor(loc, true);
                    entity.setArmor(IArmorState.ARMOR_DESTROYED, loc, true);
                } else {
                    entity.setArmor(entity.getArmor(loc, true) - armorDamage, loc, true);
                }
            }

            // The armor blown out contributes towards the 20+ PSR
            entity.damageThisPhase += armorDamage;

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
     * {@inheritDoc}
     * CASE II reduces the crit chance for ammo explosions
     */
    @Override
    public int explosionCASEIImod(boolean hasCaseII, boolean ammoExplosion) {
        if (hasCaseII && ammoExplosion) {
            return -1;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * How much damage to equipment explosions do
     */
    @Override
    public int equipmentDamage(Mounted<?> mounted, WeaponType weaponType) {
        // Charged PPC capacitors when hit can cause an explosion.
        if (weaponType.hasFlag(WeaponType.F_PPC) && (mounted.hasChargedCapacitor() != 0)) {
            if (mounted.isFired()) {
                return 0;
            }
        }

        if (mounted.getEntity().getWeightClass() == EntityWeightClass.WEIGHT_SUPER_HEAVY) {
            // Superheavies have compressed criticals. They only explode based on the number of Crit Slots they take
            // up, not the base amount of crits they need. Core p.240
            int critSlots = 0;
            // Only do this if the weapon can cross locations
            if (weaponType.isSpreadable()) {
                for (int loc = 0; loc < mounted.getEntity().locations(); loc++) {
                    for (int slot = 0; slot < mounted.getEntity().getNumberOfCriticalSlots(loc); slot++) {
                        final CriticalSlot crit = mounted.getEntity().getCritical(loc, slot);
                        if ((crit != null) && ((crit.getMount() == mounted) || (crit.getMount2() == mounted))) {
                            critSlots++;
                        }
                    }
                }
            } else {
                // We skip a bunch of extra work by doing this
                for (int slot = 0; slot < mounted.getEntity().getNumberOfCriticalSlots(mounted.getLocation()); slot++) {
                    final CriticalSlot crit = mounted.getEntity().getCritical(mounted.getLocation(), slot);
                    if ((crit != null) && ((crit.getMount() == mounted) || (crit.getMount2() == mounted))) {
                        critSlots++;
                    }
                }
            }
            return (critSlots * 2);
        }

        return mounted.getNumCriticalSlots() * 2;
    }

    /**
     * {@inheritDoc}
     * These are not explosive in core. return false.
     */
    @Override
    public boolean arePodsExplosive(Mounted<?> mounted) {
        return false;
    }
    
    /**
    * {@inheritDoc}
    * Explosions are reduced under Core rules.
    */
    @Override
    public boolean explosionsAreReduced() {
        return true;
    }
}
