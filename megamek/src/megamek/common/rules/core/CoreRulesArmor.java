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
import megamek.common.enums.HitDamageType;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;
import megamek.common.rules.RulesArmor;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.server.totalWarfare.TWDamageManager;

public class CoreRulesArmor extends RulesArmor {
    /**
     * {@inheritDoc}
     * Core rules heat armor halves heat weapon damage. This allows for weapons to be marked for heat.
     * Core rules p.202
     */
    @Override
    public boolean allowHeatWeapon(boolean heat_weapon) {
        return heat_weapon;
    }

    /**
     * {@inheritDoc}
     * Anti-penetrative ablation armor negates "any armor-piercing special effects" (Core p.201). Ballistic-reinforced
     * and hardened armor also "negate AP ammo armor-piercing checks" (Core p.191). Against armor-piercing missiles, such
     * as tandem-charge SRMs, which are not in Core, the other armors keep the TO:AUE rules.
     */
    @Override
    public boolean allowArmorPiercing(TWDamageManager.ModsInfo mods, HitDamageType damageType) {
        if (mods.abaArmor) {
            return false;
        }
        if (damageType != HitDamageType.DAMAGE_ARMOR_PIERCING) {
            return allowsTacticalOperationsArmorPiercing(mods);
        }
        return !(mods.hardenedArmor || mods.ballisticArmor);
    }

    /**
     * {@inheritDoc}
     * Ferro-lamellor armor reduces damage "by 1 point for every 5 points (or fraction thereof) delivered per hit"
     * (Core p.202), armor-piercing ammunition included: only ablation, ballistic-reinforced and hardened armor treat it
     * as standard damage (Core p.191). Acid (AX) damage "is not reduced by special armor rules" (Core p.192).
     */
    @Override
    public boolean ferroLamellorArmorReduces(HitDamageType damageType) {
        return (damageType != HitDamageType.DAMAGE_ARMOR_PIERCING_MISSILE)
              && !damageType.ignoresArmorDamageReduction()
              && (damageType != HitDamageType.DAMAGE_AX);
    }

    /**
     * {@inheritDoc}
     * Impact-resistant armor gives no other modifier. Core p.191, p.202
     */
    @Override
    public int impactArmorMod() {return 0;}

    /**
     * {@inheritDoc}
     * Impact-resistant armor gives no breach modifier in Core (p.202).
     */
    @Override
    public int impactArmorBreach() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * Does a lance penetrate on hit? Not if it is ABA, but all others yes. Core p.201
     */
    @Override
    public boolean checkLancePenetration(int armorType) {
        return armorType != EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION;
    }

    /**
     * {@inheritDoc}
     * Core p.202 gives laser-reflective armor no heat effect; see {@link #heatAfterDissipatingArmor} for
     * heat-dissipating armor.
     */
    @Override
    public int reduceHeatDamageByArmor(int armorType, int heatDamage) {
        return heatDamage;
    }

    /**
     * {@inheritDoc}
     * Core p. 202: a Mek with heat-dissipating armor is still affected by Extreme Heat.
     */
    @Override
    public int reduceExtremeHeat(Entity target, int heat) {
        return heat;
    }

    /**
     * {@inheritDoc}
     * Core p.202: heat-dissipating armor reduces the heat of heat-causing weapons and other outside heat sources to 0.
     * Like any special armor it protects the location struck while that location or facing has armor (Core p.201);
     * heat that strikes no location, such as fire or magma, is negated while the Mek has any amount of it (Core p.202).
     */
    @Override
    public int heatAfterDissipatingArmor(Entity target, int heat, int location, boolean rear) {
        boolean dissipated = (location == Entity.LOC_NONE)
              ? (target instanceof Mek mek) && mek.hasAnyHeatDissipatingArmor()
              : (target.getArmorType(location) == EquipmentType.T_ARMOR_HEAT_DISSIPATING)
                    && (target.getArmor(location, rear) > 0);
        return dissipated ? 0 : heat;
    }

    /**
     * {@inheritDoc}
     * Laser-reflective armor has no modifier against armor-piercing rounds (Core p.191, p.202)
     */
    @Override
    public boolean reflectiveAP(boolean reflectiveArmor) {return false;}

    /**
     * {@inheritDoc}
     * Anti-penetrative ablation armor ignores through-armor critical checks (Core p.91, p.201).
     */
    @Override
    public boolean blockTAC(int armorType) {
        return (armorType == ArmorType.T_ARMOR_ANTI_PENETRATIVE_ABLATION);
    }

    @Override
    public int hardenedLocationCriticalModifier(HitData hit) {
        if (hit.getGeneralDamageType() == HitDamageType.DAMAGE_IGNORES_DMG_REDUCTION) {
            return 0; // Re-engineered lasers treat hardened armor as standard (Core p.185; BMM p.101).
        }
        // Core p.91, errata v0.2: the modifier applies to the TAC check only.
        return hit.isThroughArmorCritical() ? -2 : 0;
    }

    /**
     * {@inheritDoc}
     * Against armor-piercing ammunition, ablation, ballistic-reinforced and hardened armor "provide no damage reduction
     * or other special protective bonuses of any kind" (Core p.191).
     */
    @Override
    public boolean criticalProtectionsApplyAgainst(HitDamageType damageType) {
        return damageType != HitDamageType.DAMAGE_ARMOR_PIERCING;
    }

    /**
     * {@inheritDoc}
     * Core ablation armor ignores through-armor critical checks and negates armor-piercing checks instead (Core p.201),
     * and gives no protection against armor-piercing ammunition (Core p.191).
     */
    @Override
    public int ablationArmorPiercingModifier() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * The errata limits the exception to "If an attack destroys a location, remaining damage from that attack that
     * transfers" (Core p.191, errata v0.2), so a location destroyed before passes the check on.
     */
    @Override
    public boolean armorPiercingCheckPassesDestroyedLocation() {
        return true;
    }

    /**
     * {@inheritDoc}
     * Impact-resistant armor halves damage from building collapses, collisions and falls (rounding remaining damage
     * down) and removes one point per three points of physical attack damage (Core p.202).
     */
    @Override
    public int reduceImpactDamage(int entityId,
                                  HitData hit,
                                  int damage,
                                  Vector<Report> reportVec,
                                  HitDamageType damageType) {
        Report report;
        if (hit.isFallDamage() || damageType == HitDamageType.DAMAGE_PHYSICAL_NONATTACK) {
            damage = Math.max(1, damage / 2);
            report = new Report(6099);
            report.subject = entityId;
            report.indent(3);
            report.add(damage);
            reportVec.addElement(report);

            return damage;
        }
        return reduceImpactDamageByThird(entityId, damage, reportVec);
    }

    /**
     * {@inheritDoc}
     * Reflective armor reduces damage by half, rounded up. Core p.202
     */
    @Override
    public int reflectiveArmorReduction(final int damage) {
        return (int) Math.ceil(((double) damage) / 2);
    }

    /**
     * {@inheritDoc}
     * Reactive armor reduces damage by half, rounded up. Core p.202
     */
    @Override
    public int reactiveArmorReduction(final int damage) {
        return (int) Math.ceil(((double) damage) / 2);
    }

    /**
     * {@inheritDoc}
     * Ballistic reinforced armor reduces damage by half, rounded up. Core p.201
     */
    @Override
    public int ballisticReinforcedArmorReduction(final int damage) {
        return (int) Math.ceil(((double) damage) / 2);
    }

    /**
     * {@inheritDoc}
     * Attacks of the DB or M types (Core p.201), except armor-piercing ammunition, against which it provides "no
     * damage reduction" (Core p.191), and acid (AX) missiles, whose damage "is not reduced by special armor rules"
     * (Core p.192). Armor-piercing missiles are not in Core and keep the IO:AE p.81 reduction.
     */
    @Override
    public boolean ballisticReinforcedArmorReduces(HitDamageType damageType) {
        return (damageType == HitDamageType.DAMAGE_BALLISTIC)
              || (damageType == HitDamageType.DAMAGE_MISSILE)
              || (damageType == HitDamageType.DAMAGE_ARMOR_PIERCING_MISSILE);
    }

    @Override
    public boolean blocksAblationTaserEffect() {
        return true;
    }
}
