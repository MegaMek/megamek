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

public class JumpShipCostCalculator {

    public static double calculateCost(Jumpship jumpShip, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[23];
        int costIdx = 0;
        double cost = 0;

        // Control Systems
        // Bridge
        costs[costIdx++] += 200000 + 10 * jumpShip.getWeight();
        // Computer
        costs[costIdx++] += 200000;
        // Life Support
        costs[costIdx++] += 5000 * (jumpShip.getNCrew() + jumpShip.getNPassenger());
        // Sensors
        costs[costIdx++] += 80000;
        // Fire Control Computer
        costs[costIdx++] += 100000;
        // Gunnery Control Systems
        costs[costIdx++] += 10000 * jumpShip.getArcswGuns();
        // Structural Integrity
        costs[costIdx++] += 100000 * jumpShip.getSI();

        // Station-Keeping Drive
        // Engine
        costs[costIdx++] += 1000 * jumpShip.getWeight() * 0.012;
        // Engine Control Unit
        costs[costIdx++] += 1000;

        // KF Drive
        double[] driveCost = new double[6];
        int driveIdx = 0;
        double driveCosts = 0;
        // Drive Coil
        driveCost[driveIdx++] += 60000000.0 + (75000000.0 * jumpShip.getDocks(true));
        // Initiator
        driveCost[driveIdx++] += 25000000.0 + (5000000.0 * jumpShip.getDocks(true));
        // Controller
        driveCost[driveIdx++] += 50000000.0;
        // Tankage
        driveCost[driveIdx++] += 50000.0 * jumpShip.getKFIntegrity();
        // Sail
        driveCost[driveIdx++] += 50000.0 * (30 + (jumpShip.getWeight() / 7500.0));
        // Charging System
        driveCost[driveIdx++] += 500000.0 + (200000.0 * jumpShip.getDocks(true));

        for (int i = 0; i < driveIdx; i++) {
            driveCosts += driveCost[i];
        }

        if (jumpShip.hasLF()) {
            driveCosts *= 3;
        }

        costs[costIdx++] += driveCosts;

        // K-F Drive Support Systems
        costs[costIdx++] += 10000000 * (jumpShip.getWeight() / 10000);

        // Additional Ships Systems
        // Attitude Thrusters
        costs[costIdx++] += 25000;

        // Docking Collars
        costs[costIdx++] += 100000 * jumpShip.getDocks();

        // Fuel Tanks
        costs[costIdx++] += (200 * jumpShip.getFuel()) / jumpShip.getFuelPerTon() * 1.02;

        // Armor
        costs[costIdx++] += jumpShip.getArmorWeight() * EquipmentType.getArmorCost(jumpShip.getArmorType(0));

        // Heat Sinks
        int sinkCost = 2000 + (4000 * jumpShip.getHeatType());
        costs[costIdx++] += sinkCost * jumpShip.getHeatSinks();

        // Escape Craft
        costs[costIdx++] += 5000 * (jumpShip.getLifeBoats() + jumpShip.getEscapePods());

        // Grav Decks
        double deckCost = 0;
        deckCost += 5000000 * jumpShip.getGravDeck();
        deckCost += 10000000 * jumpShip.getGravDeckLarge();
        deckCost += 40000000 * jumpShip.getGravDeckHuge();
        costs[costIdx++] += deckCost;

        // Transport Bays
        int baydoors = 0;
        long bayCost = 0;
        long quartersCost = 0;
        // Passenger and crew quarters and infantry bays are considered part of the structure
        // and don't add to the cost
        for (Bay next : jumpShip.getTransportBays()) {
            baydoors += next.getDoors();
            if (!next.isQuarters() && !(next instanceof InfantryBay) && !(next instanceof BattleArmorBay)) {
                bayCost += next.getCost();
            }
        }

        costs[costIdx++] += bayCost + (baydoors * 1000L);
        costs[costIdx++] = quartersCost;

        // HPG
        if (jumpShip.hasHPG()) {
            costs[costIdx++] += 1000000000;
        } else {
            costs[costIdx++] += 0;
        }

        // Weapons and Equipment
        costs[costIdx++] += CostCalculator.getWeaponsAndEquipmentCost(jumpShip, ignoreAmmo);

        // Sum Costs
        for (int i = 0; i < costIdx; i++) {
            cost += costs[i];
        }

        costs[costIdx] = -jumpShip.getPriceMultiplier(); // Negative indicates multiplier
        cost = Math.round(cost * jumpShip.getPriceMultiplier());

        String[] systemNames = { "Bridge", "Computer", "Life Support", "Sensors", "FCS", "Gunnery Control Systems",
                "Structural Integrity", "Engine", "Engine Control Unit",
                "KF Drive", "KF Drive Support System", "Attitude Thrusters", "Docking Collars",
                "Fuel Tanks", "Armor", "Heat Sinks", "Life Boats/Escape Pods", "Grav Decks",
                "Bays", "Quarters", "HPG", "Weapons/Equipment", "Weight Multiplier" };
        CostCalculator.fillInReport(costReport, jumpShip, ignoreAmmo, systemNames, 20, cost, costs);
        return cost;
    }
}
