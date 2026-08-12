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


import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;
import megamek.common.rules.RulesArmor;
import megamek.server.totalWarfare.TWDamageManager;

import java.util.Vector;

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
     * Hardened and ABA armor prevent AP. Core p.201
     */
    @Override
    public boolean allowArmorPiercing(TWDamageManager.ModsInfo mods) {
        return (mods.hardenedArmor || mods.abaArmor) ? false : true;
    }

    /**
     * {@inheritDoc}
     * Impact armor does not reduce anything. Core p.201
     */
    @Override
    public int impactArmorMod() {return 0;}

    /**
     * {@inheritDoc}
     * Impact Resistant Armor breach. Does not apply in Core. p.201
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
     * Reflective armor does not have a modifier on AP rounds
     */
    @Override
    public boolean reflectiveAP(boolean reflectiveArmor) {return false;}

    /**
     * {@inheritDoc}
     * ABA armor stops TACs from generating a crit. Core p.201
     */
    @Override
    public boolean blockTAC(int armorType) {
        return (armorType == ArmorType.T_ARMOR_ANTI_PENETRATIVE_ABLATION);
    }

    /**
     * {@inheritDoc}
     * Impact armor reduces damage from falls, collisions, and buildings by half. 1/3 for physical
     */
    @Override
    public int reduceImpactDamage(int entityId, HitData hit, int damage, Vector<Report> reportVec, int damageType) {
        Report report;
        if (hit.isFallDamage() || damageType == HitData.DAMAGE_PHYSICAL_NONATTACK) {
            damage = Math.max(1, damage / 2);
            report = new Report(6099);
            report.subject = entityId;
            report.indent(3);
            report.add(damage);
            reportVec.addElement(report);

            return damage;
        }
        // CORE collision and building damage currently are as below, need to be as above
        
        // As long as there is even 1 point of armor in this location, reduce _all_ damage
        // to 2 points for every whole 3 points applied (IntOps pg 88).
        damage = Math.max(1, (2 * (damage / 3)) + (damage % 3));
        report = new Report(6089);
        report.subject = entityId;
        report.indent(3);
        report.add(damage);
        reportVec.addElement(report);
        return damage;
    }
}
