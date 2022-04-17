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
import megamek.common.*;

public class DropShipCostCalculator {

    public static double calculateCost(Dropship dropShip, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[20];
        int costIdx = 0;
        double cost = 0;

        // Control Systems
        // Bridge
        costs[costIdx++] += 200000 + (10 * dropShip.getWeight());
        // Computer
        costs[costIdx++] += 200000;
        // Life Support
        costs[costIdx++] += 5000 * (dropShip.getNCrew() + dropShip.getNPassenger());
        // Sensors
        costs[costIdx++] += 80000;
        // Fire Control Computer
        costs[costIdx++] += 100000;
        // Gunnery Control Systems
        costs[costIdx++] += 10000 * dropShip.getArcswGuns();

        // Structural Integrity
        costs[costIdx++] += 100000 * dropShip.getSI();

        // Additional Flight Systems
        // Attitude Thruster
        costs[costIdx++] += 25000;
        // Landing Gear
        costs[costIdx++] += 10 * dropShip.getWeight();
        // Docking Collar
        if (dropShip.getCollarType() == Dropship.COLLAR_STANDARD) {
            costs[costIdx++] += 10000;
        } else if (dropShip.getCollarType() == Dropship.COLLAR_PROTOTYPE) {
            costs[costIdx++] += 1010000;
        }

        // Engine
        double engineMultiplier = 0.065;
        if (dropShip.isClan()) {
            engineMultiplier = 0.061;
        }
        double engineWeight = dropShip.getOriginalWalkMP() * dropShip.getWeight() * engineMultiplier;
        costs[costIdx++] += engineWeight * 1000;
        // Drive Unit
        costs[costIdx++] += (500 * dropShip.getOriginalWalkMP() * dropShip.getWeight()) / 100.0;

        // Fuel Tanks
        costs[costIdx++] += (200 * dropShip.getFuel()) / dropShip.getFuelPointsPerTon() * 1.02;

        // Armor
        costs[costIdx++] += dropShip.getArmorWeight() * EquipmentType.getArmorCost(dropShip.getArmorType(0));

        // Heat Sinks
        int sinkCost = 2000 + (4000 * dropShip.getHeatType());
        costs[costIdx++] += sinkCost * dropShip.getHeatSinks();

        // Weapons and Equipment
        costs[costIdx++] += CostCalculator.getWeaponsAndEquipmentCost(dropShip, ignoreAmmo);

        // Transport Bays
        int baydoors = 0;
        long bayCost = 0;
        long quartersCost = 0;
        // Passenger and crew quarters and infantry bays are considered part of the structure
        // and don't add to the cost
        for (Bay next : dropShip.getTransportBays()) {
            baydoors += next.getDoors();
            if (!next.isQuarters() && !(next instanceof InfantryBay) && !(next instanceof BattleArmorBay)) {
                bayCost += next.getCost();
            }
        }

        costs[costIdx++] += bayCost + (baydoors * 1000L);
        costs[costIdx++] = quartersCost;

        // Life Boats and Escape Pods
        costs[costIdx++] += 5000 * (dropShip.getLifeBoats() + dropShip.getEscapePods());

        // TODO Decouple cost calculation from addCostDetails and eliminate duplicate code in getPriceMultiplier
        double weightMultiplier = 36.0;
        if (dropShip.isSpheroid()) {
            weightMultiplier = 28.0;
        }

        // Sum Costs
        for (int i = 0; i < costIdx; i++) {
            cost += costs[i];
        }

        costs[costIdx] = -weightMultiplier; // Negative indicates multiplier
        cost = Math.round(cost * weightMultiplier);

        String[] systemNames = { "Bridge", "Computer", "Life Support", "Sensors", "FCS", "Gunnery Control Systems",
                "Structural Integrity", "Attitude Thruster", "Landing Gear", "Docking Collar",
                "Engine", "Drive Unit", "Fuel Tanks", "Armor", "Heat Sinks", "Weapons/Equipment", "Bays",
                "Quarters", "Life Boats/Escape Pods", "Weight Multiplier" };
        CostCalculator.fillInReport(costReport, dropShip, ignoreAmmo, systemNames, 14, cost, costs);

        return cost;
    }
}
