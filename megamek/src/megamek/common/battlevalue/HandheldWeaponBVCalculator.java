/*
 * MegaMek - Copyright (C) 2025 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package megamek.common.battlevalue;

import megamek.common.Entity;
import megamek.common.HandheldWeapon;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

public class HandheldWeaponBVCalculator extends BVCalculator {
    HandheldWeaponBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected void processDefensiveValue() {
        processArmor();
        // HHWs can mount AMS, so here we are.
        processDefensiveEquipment();
    }

    @Override
    protected void processArmor() {
        var armor = entity.getTotalArmor();
        var armorBV = entity.getTotalArmor() * 2;
        defensiveValue += armorBV;
        bvReport.addLine("Armor:", formatForReport(armor) + " (Total Armor Factor) x 2", "= " + formatForReport(armorBV));
    }

    @Override
    protected void processOffensiveValue() {
        processWeapons();
        processAmmo();
        processOffensiveEquipment();
    }
}
