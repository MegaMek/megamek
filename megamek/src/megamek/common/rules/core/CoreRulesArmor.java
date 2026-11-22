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
            return super.allowArmorPiercing(mods, damageType);
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
              && (damageType != HitDamageType.DAMAGE_IGNORES_DMG_REDUCTION)
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
     * Core writes impact-resistant armor without a breach modifier (Core p.202), and Core breaches are underwater only
     * (Core p.127). Vacuum breaches are not in Core and keep the +1 "including underwater and vacuum" (IO:AE p.82).
     */
    @Override
    public int impactArmorBreach(boolean vacuum) {
        return vacuum ? 1 : 0;
    }

    /**
     * {@inheritDoc}
     * Does a lance penetrate on hit? Not if it is ABA, but all others yes. Core p.201
     */
    @Override
    public boolean checkLancePenetration(int armorType) {
        return (armorType == EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION) ? false : true;
    }

    /**
     * {@inheritDoc}
     * Does armor reduce heat? Core p.202
     */
    @Override
    public int reduceHeatDamageByArmor(int armorType, int heatDamage) {
        return (armorType == EquipmentType.T_ARMOR_HEAT_DISSIPATING) ? 0 : heatDamage;
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
    public int ablationArmorCriticalModifier() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * Impact-resistant armor halves damage from building collapses, collisions and falls (rounding remaining damage
     * down) and reduces physical attack damage as {@link RulesArmor#reduceImpactDamage} does (Core p.202).
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
        return super.reduceImpactDamage(entityId, hit, damage, reportVec, damageType);
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
}
