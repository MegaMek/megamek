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
