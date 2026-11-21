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


import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.enums.HitDamageType;
import megamek.common.equipment.EquipmentType;
import megamek.common.rules.RulesArmor;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.server.totalWarfare.TWDamageManager;

public class TWRulesArmor extends RulesArmor {
    /**
     * Allow heat weapons for heat armor.
     * TW does not need to know about heat weapons for armor.
     *
     * @param heat_weapon true if the weapon is a heat weapon
     * @return true if heat weapons are allowed
     */
    @Override
    public boolean allowHeatWeapon(boolean heat_weapon) {
        return false;
    }

    /**
     * Does armor allow armor piercing.
     * Hardened, FerroLam, and Reactive prevent AP ammo.
     *
     * @param mods the modifications info
     * @return true if armor piercing is allowed
     */
    @Override
    public boolean allowArmorPiercing(TWDamageManager.ModsInfo mods) {
        if (mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * Impact Resistant Armor breach.
     */
    @Override
    public int impactArmorBreach() {
        return 1;
    }

    /**
     * Impact resistant armor.
     * Impact armor reduces crit rolls.
     *
     * @return the impact armor modifier
     */
    @Override
    public int impactArmorMod() {return 1;}

    /**
     * Does a lance penetrate the armor.
     * Hardened and ferro lam prevent penetration.
     *
     * @param armorType the type of armor
     * @return true if a lance penetrates the armor
     */
    @Override
    public boolean checkLancePenetration(int armorType) {
        return (armorType == EquipmentType.T_ARMOR_HARDENED || armorType == EquipmentType.T_ARMOR_FERRO_LAMELLOR) ?
              false : true;
    }

    /**
     * {@inheritDoc}
     * BMM p.114: laser-reflective armor halves the heat of the flamers, lasers, PPCs and plasma weapons that strike it,
     * rounding down to a minimum of 1.
     */
    @Override
    public int reduceHeatDamageByArmor(int armorType, int heatDamage) {
        if ((armorType == EquipmentType.T_ARMOR_REFLECTIVE) && (heatDamage > 0)) {
            return Math.max(1, (int) Math.floor(heatDamage / 2.0));
        }
        return heatDamage;
    }

    /**
     * {@inheritDoc}
     * IO:AE p.81: "As long as the location struck still has at least 1 point of armor remaining, heat-dissipating armor
     * reduces the external heat effects ... by half. Round this reduced heat effect down to the nearest point". Heat
     * that strikes no location, such as fire, names no location struck; we read it as needing the armor in every
     * location.
     */
    @Override
    public int heatAfterDissipatingArmor(Entity target, int heat, int location, boolean rear) {
        boolean dissipated = (location == Entity.LOC_NONE)
              ? (target instanceof Mek mek) && mek.hasIntactHeatDissipatingArmor()
              : (target.getArmorType(location) == EquipmentType.T_ARMOR_HEAT_DISSIPATING)
                    && (target.getArmor(location, rear) > 0);
        return dissipated ? heat / 2 : heat;
    }

    /**
     * Does reflective armor cause modifiers for AP?
     * In TW, reflective armor will cause AP crit chance changes.
     *
     * @param reflectiveArmor true if the armor is reflective
     * @return true if reflective armor affects AP modifiers
     */
    @Override
    public boolean reflectiveAP(boolean reflectiveArmor) {
        return reflectiveArmor;
    }

    /**
     * Block TAC (Targeting Auto-Correlator).
     *
     * @param armorType the type of armor
     * @return true if TAC is blocked
     */
    @Override
    public boolean blockTAC(int armorType) {
        return false;
    }

    /**
     * How does impact armor reduce damage.
     *
     * @param entityId the entity ID
     * @param hit the hit data
     * @param damage the damage amount
     * @param reportVec vector of reports describing the damage reduction
     * @param damageType the type of damage
     * @return the reduced damage amount
     */
    @Override
    public int reduceImpactDamage(int entityId,
                                  HitData hit,
                                  int damage,
                                  Vector<Report> reportVec,
                                  HitDamageType damageType) {
        // As long as there is even 1 point of armor in this location, reduce _all_ damage
        // to 2 points for every whole 3 points applied (IntOps pg 88).
        damage = Math.max(1, (2 * (damage / 3)) + (damage % 3));
        Report report = new Report(6089);
        report.subject = entityId;
        report.indent(3);
        report.add(damage);
        reportVec.addElement(report);
        return damage;
    }

    /**
     * {@inheritDoc}
     * Round down. TO:AUE p.93
     */
    @Override
    public int reflectiveArmorReduction(final int damage) {
        return (int) Math.floor(((double) damage) / 2);
    }

    /**
     * {@inheritDoc}
     * Round down. TO:AUE p.93
     */
    @Override
    public int reactiveArmorReduction(final int damage) {
        return (int) Math.floor(((double) damage) / 2);
    }

    /**
     * {@inheritDoc}
     * Round down. TO:AUE p.93
     */
    @Override
    public int ballisticReinforcedArmorReduction(final int damage) {
        return Math.max(1, damage / 2);
    }
}
