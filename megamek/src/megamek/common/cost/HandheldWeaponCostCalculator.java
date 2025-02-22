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
