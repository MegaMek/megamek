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

import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesExplosions;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

public class CoreRulesExplosions extends RulesExplosions {
    /**
     * {@inheritDoc}
     * Internal explosion damage to the internal structure is capped at 20, 10 with CASE and 1 with CASE II
     * (Core pp.96-97).
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
            if (damage <= cap) {
                return damage;
            }
            reportCap(mek, damage, caseIId ? 6134 : cased ? 6133 : 6132, reportVec);

            // Torso locations damage out the rear armor
            boolean torso = mek.locationIsTorso(loc);

            // With CASE or CASE II, a location that survives takes up to 10 points to its armor (Core pp.96-97).
            // Without CASE the armor is vented where the last point lands, see ventCappedExplosion. Whether the
            // location survives is judged after composite or reinforced structure modifies the capped damage, as
            // modifiers apply "after any damage cap is applied" (Core p.96; Core p.205).
            boolean survives = mek.hasCompositeStructure(loc) ? (mek.getInternal(loc) > cap * 2)
                  : mek.hasReinforcedStructure(loc) ? (mek.getReinforcedStructureCapacity(loc) > cap)
                  : (mek.getInternal(loc) > cap);
            if ((caseIId || cased) && survives) {
                int armorDamage = Math.min(damage - cap, 10);
                if (mek.getArmor(loc, torso) > armorDamage) {
                    mek.setArmor(mek.getArmor(loc, torso) - armorDamage, loc, torso);
                } else {
                    armorDamage = Math.max(0, mek.getArmor(loc, torso));
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
     * Core writes the rotary autocannon without the Total Warfare explosion when hit while jammed (Core p.183).
     */
    @Override
    public boolean jammedRotaryAutocannonExplodes() {
        return false;
    }

    /**
     * {@inheritDoc}
     * If an explosion was over the 20-point cap, the location where its last point is applied, if not destroyed by
     * it, loses all its armor; a torso loses only its rear armor (Core p.96).
     */
    @Override
    public void ventCappedExplosion(Mek mek, HitData hit, Vector<Report> reportVec) {
        int loc = hit.getLocation();
        boolean torso = mek.locationIsTorso(loc);
        int armorDamage = Math.max(0, mek.getArmor(loc, torso));
        mek.setArmor(IArmorState.ARMOR_DESTROYED, loc, torso);
        mek.damageThisPhase += armorDamage;
        Report report = new Report(6131);
        report.subject = mek.getId();
        report.indent(3);
        report.add((torso ? "Rear " : "") + mek.getLocationAbbr(loc));
        report.add(armorDamage);
        reportVec.addElement(report);
    }

    private static void reportCap(Entity entity, int damage, int messageId, Vector<Report> reportVec) {
        Report report = new Report(messageId);
        report.subject = entity.getId();
        report.indent(3);
        report.add(damage);
        reportVec.addElement(report);
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
     * CASE II applies a -1 to the critical hit check for internal explosions (Core p.91, p.97)
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
     * A component explosion "does 2 points of damage, multiplied by the number of slots that item occupies" (Core p.96),
     * for every explosive component.
     */
    @Override
    public int equipmentDamage(Mounted<?> mounted, WeaponType weaponType) {
        // A charged PPC capacitor explodes with its PPC for twice their combined slots (Core p.188)
        if (mounted.holdsCapacitorCharge()) {
            return (explodingSlots(mounted) + explodingSlots(mounted.getLinkedBy())) * 2;
        }
        return explodingSlots(mounted) * 2;
    }

    /** @return The critical slots the exploding equipment fills */
    private static int explodingSlots(Mounted<?> mounted) {
        if ((mounted.getEntity() instanceof Mek) && (mounted.getEntity().isSuperHeavy())) {
            // Superheavy Meks have compressed criticals. They only explode based on the number of Crit Slots they take
            // up, not the base amount of crits they need. Core p.240
            return mounted.getEntity().occupiedCriticalSlots(mounted);
        }

        return mounted.getNumCriticalSlots();
    }

    /**
     * {@inheritDoc}
     * Unexpended pods do not cause an internal explosion (Core p.189)
     */
    @Override
    public boolean arePodsExplosive(Mounted<?> mounted) {
        return false;
    }

    /**
    * {@inheritDoc}
    * Internal explosion damage is capped (Core p.96).
    */
    @Override
    public boolean explosionsAreReduced() {
        return true;
    }
}
