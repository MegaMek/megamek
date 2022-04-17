/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.cost;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.weapons.infantry.InfantryWeapon;

import java.text.NumberFormat;

public class CostCalculator {

    /**
     * Calculates the total cost of weapons and equipment of the given entity. Ammo costs are added
     * depending on the ignoreAmmo parameter.
     *
     * @param entity The unit to calculate costs for
     * @param ignoreAmmo When true, ammo is not included
     * @return The total C-bill cost of weapons and equipment (not systems such as cockpit)
     */
    static long getWeaponsAndEquipmentCost(Entity entity, boolean ignoreAmmo) {
        return getWeaponsAndEquipmentCost(entity, new DummyCalculationReport(), ignoreAmmo);
    }

    /**
     * Calculates the total cost of weapons and equipment of the given entity. Ammo costs are added
     * depending on the ignoreAmmo parameter. For each weapon and equipment a line is added to
     * the given CalculationReport. No header or other addition is made to the report.
     *
     * @param costReport The CalculationReport to fill in
     * @param entity The unit to calculate costs for
     * @param ignoreAmmo When true, ammo is not included
     * @return The total C-bill cost of weapons and equipment (not systems such as cockpit)
     */
    static long getWeaponsAndEquipmentCost(Entity entity, CalculationReport costReport, boolean ignoreAmmo) {
        long cost = 0;
        NumberFormat commafy = NumberFormat.getInstance();

        for (Mounted mounted : entity.getEquipment()) {
            if (ignoreAmmo && (mounted.getType() instanceof AmmoType)
                    && (!(((AmmoType) mounted.getType()).getAmmoType() == AmmoType.T_COOLANT_POD))) {
                continue;
            }
            if (mounted.isWeaponGroup()) {
                continue;
            }
            // BA Manipulators are considered part of the structure costs and must be excluded here
            if ((mounted.getType() instanceof MiscType)
                    && mounted.getType().hasFlag(MiscType.F_BA_MANIPULATOR)) {
                continue;
            }
            long itemCost = (long) mounted.getCost();
            if (!ignoreAmmo && entity.isSupportVehicle() && (mounted.getSize() > 1)
                    && (mounted.getType() instanceof InfantryWeapon)) {
                itemCost += Double.valueOf((mounted.getSize() - 1d)
                        * ((InfantryWeapon) mounted.getType()).getAmmoCost()).longValue();
            }

            cost += itemCost;
            if (itemCost > 0) {
                costReport.addLine(mounted.getName(), "", commafy.format(itemCost));
            }
        }
        int count = entity.implicitClanCASE();
        if (count > 0) {
            long itemCost = 50000;
            cost += count * itemCost;
            for (int i = 0; i < count; i++) {
                costReport.addLine("CASE", "", commafy.format(itemCost));
            }
        }
        // Large craft have a separate section for bays
        if (!entity.isLargeCraft()) {
            long seatCost = 0;
            long quartersCost = 0;
            long bayCost = 0;
            for (Bay bay : entity.getTransportBays()) {
                if (bay instanceof StandardSeatCargoBay) {
                    seatCost += bay.getCost();
                } else if (bay.isQuarters()) {
                    quartersCost += bay.getCost();
                } else {
                    bayCost += bay.getCost() + 1000L * bay.getDoors();
                }
            }
            if (seatCost > 0) {
                cost += seatCost;
                costReport.addLine("Seating", "", commafy.format(seatCost));
            }
            if (quartersCost > 0) {
                cost += quartersCost;
                costReport.addLine("Quarters", "", commafy.format(quartersCost));
            }
            if (bayCost > 0) {
                cost += bayCost;
                costReport.addLine("Bays", "", commafy.format(bayCost));
            }
        }
        return cost;
    }

    /**
     * Fills in the cost calculation report by adding a header for the entity and lines for
     * each entry in systemNames and costs. The length of costs must be at least equal to the
     * length of systemNames and the costs should match the systemName per index for the report
     * to make sense. Each line will contain the systemName on the left and the cost on the right.
     * On the array index given by equipIndex the weapons and equipment will be inserted.
     *
     * @param costReport The CalculationReport to fill in
     * @param entity The unit to calculate costs for
     * @param ignoreAmmo When true, ammo is not included in the weapons and equipment
     * @param equipIndex The array index to insert weapons and equipment in
     * @param cost The total cost of the unit
     * @param systemNames An array of cost type names such as "Cockpit"
     * @param costs An array of costs matching the systemNames
     */
    static void fillInReport(CalculationReport costReport, Entity entity, boolean ignoreAmmo,
                             String[] systemNames, int equipIndex, double cost, double[] costs) {
        NumberFormat commafy = NumberFormat.getInstance();
        costReport.addHeader("Cost Calculations For " + entity.getChassis() + " " + entity.getModel());
        for (int l = 0; l < systemNames.length; l++) {
            if (l == equipIndex) {
                CostCalculator.getWeaponsAndEquipmentCost(entity, costReport, ignoreAmmo);
            } else {
                String result = commafy.format(costs[l]);
                if (costs[l] == 0) {
                    result = "N/A";
                } else if (costs[l] < 0) {
                    result = "x " + commafy.format(-costs[l]);
                }
                costReport.addLine(systemNames[l], "", result);
            }
        }
        costReport.addResultLine("Total Cost:", "", commafy.format(cost));
    }


    /**
     * Adds a note to the given CalculationReport that no report is available for units of the
     * given entity's type. Null parameters can be safely passed to this method.
     *
     * @param costReport The CalculationReport to add the note to
     * @param entity The unit to calculate costs for
     */
    public static void addNoReportNote(@Nullable CalculationReport costReport, @Nullable Entity entity) {
        if (costReport != null) {
            if (entity != null) {
                costReport.addHeader("Cost Calculations For");
                costReport.addHeader(entity.getChassis() + " " + entity.getModel());
                costReport.addLine("There is currently no report available for units of this type.");
            } else {
                costReport.addLine("Could not access the unit.");
            }
        }
    }
}
