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

package megamek.common.cost;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.HandheldWeapon;

public class HandheldWeaponCostCalculator {
    public static double calculateCost(HandheldWeapon hhw, CalculationReport report, boolean ignoreAmmo) {
        var equipCost = CostCalculator.getWeaponsAndEquipmentCost(hhw, report, ignoreAmmo);
        var structureCost = CostCalculator.getWeaponsAndEquipmentCost(hhw, false);
        CostCalculator.fillInReport(report, hhw, ignoreAmmo, new String[]{"Structure", "Equipment"}, 1, equipCost+structureCost, new double[]{structureCost, equipCost});
        return equipCost + structureCost;
    }
}
