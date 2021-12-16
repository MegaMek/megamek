/*
 * Copyright (c) 2019 The Megamek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.WeaponType;

/**
 * Helper class that contains common functionality for flamer-type weapons.
 * @author NickAragua
 *
 */
public class FlamerHandlerHelper {   
    /**
     * Handles flamer heat damage.
     */
    public static void doHeatDamage(Entity entityTarget, Vector<Report> vPhaseReport, WeaponType wtype, int subjectId, HitData hit) {
        Report r = new Report(3400);
        r.subject = subjectId;
        r.indent(2);
        int heatDamage = wtype.getDamage();
        
        // ER flamers don't do as much heat damage
        if (wtype.hasFlag(WeaponType.F_ER_FLAMER)) {
            heatDamage = Math.max(1, heatDamage / 2);
        }
        
        boolean heatDamageReducedByArmor = false;
        int actualDamage = heatDamage;
        
        // armor can't reduce damage if there isn't any
        if (entityTarget.getArmor(hit) > 0) {
            // heat dissipating armor divides heat damage by 2
            if (entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HEAT_DISSIPATING) {
                actualDamage = heatDamage / 2;
                heatDamageReducedByArmor = true;
            // reflective armor divides heat damage by 2, with a minimum of 1
            } else if (entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REFLECTIVE) {
                actualDamage = Math.max(1, heatDamage / 2);
                heatDamageReducedByArmor = true;
            }

        } 
        
        if (heatDamageReducedByArmor) {
            entityTarget.heatFromExternal += actualDamage;
            r.add(actualDamage);
            r.choose(true);
            r.messageId=3406;
            r.add(heatDamage);
            r.add(EquipmentType.armorNames[entityTarget.getArmorType(hit.getLocation())]);
        } else {
            entityTarget.heatFromExternal += heatDamage;
            r.add(heatDamage);
            r.choose(true);
        }
        
        vPhaseReport.addElement(r);
    }
}
