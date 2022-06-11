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

public class WarShipCostCalculator {

    public static double calculateCost(Warship warShip, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[24];
        int costIdx = 0;
        double cost = 0;

        // Control Systems
        // Bridge
        costs[costIdx++] += 200000 + 10 * warShip.getWeight();
        // Computer
        costs[costIdx++] += 200000;
        // Life Support
        costs[costIdx++] += 5000 * (warShip.getNCrew() + warShip.getNPassenger());
        // Sensors
        costs[costIdx++] += 80000;
        // Fire Control Computer
        costs[costIdx++] += 100000;
        // Gunnery Control Systems
        costs[costIdx++] += 10000 * warShip.getArcswGuns();
        // Structural Integrity
        costs[costIdx++] += 100000 * warShip.getSI();

        // Maneuvering Drive
        // Drive Unit
        costs[costIdx++] += 500 * warShip.getOriginalWalkMP() * (warShip.getWeight() / 100.0);
        // Engine
        costs[costIdx++] += 1000 * warShip.getOriginalWalkMP() * warShip.getWeight() * 0.06;
        // Engine Control Unit
        costs[costIdx++] += 1000;

        // KF Drive
        double[] driveCost = new double[6];
        int driveIdx = 0;
        double driveCosts = 0;
        // Drive Coil
        driveCost[driveIdx++] += 60000000.0 + (75000000.0 * warShip.getDocks(true));
        // Initiator
        driveCost[driveIdx++] += 25000000.0 + (5000000.0 * warShip.getDocks(true));
        // Controller
        driveCost[driveIdx++] += 50000000.0;
        // Tankage
        driveCost[driveIdx++] += 50000.0 * warShip.getKFIntegrity();
        // Sail
        driveCost[driveIdx++] += 50000.0 * (30 + (warShip.getWeight() / 20000.0));
        // Charging System
        driveCost[driveIdx++] += 500000.0 + (200000.0 * warShip.getDocks(true));

        for (int i = 0; i < driveIdx; i++) {
            driveCosts += driveCost[i];
        }

        driveCosts *= 5;
        if (warShip.hasLF()) {
            driveCosts *= 3;
        }

        costs[costIdx++] += driveCosts;

        // K-F Drive Support Systems
        costs[costIdx++] += 20000000 * (50 + warShip.getWeight() / 10000);

        // Additional Ships Systems
        // Attitude Thrusters
        costs[costIdx++] += 25000;
        // Docking Collars
        costs[costIdx++] += 100000 * warShip.getDocks();
        // Fuel Tanks
        costs[costIdx++] += (200 * warShip.getFuel()) / warShip.getFuelPerTon() * 1.02;

        // Armor
        costs[costIdx++] += warShip.getArmorWeight() * EquipmentType.getArmorCost(warShip.getArmorType(0));

        // Heat Sinks
        int sinkCost = 2000 + (4000 * warShip.getHeatType());
        costs[costIdx++] += sinkCost * warShip.getHeatSinks();

        // Escape Craft
        costs[costIdx++] += 5000 * (warShip.getLifeBoats() + warShip.getEscapePods());

        // Grav Decks
        double deckCost = 0;
        deckCost += 5000000 * warShip.getGravDeck();
        deckCost += 10000000 * warShip.getGravDeckLarge();
        deckCost += 40000000 * warShip.getGravDeckHuge();
        costs[costIdx++] += deckCost;

        // Transport Bays
        int baydoors = 0;
        long bayCost = 0;
        long quartersCost = 0;
        // Passenger and crew quarters and infantry bays are considered part of the structure
        // and don't add to the cost
        for (Bay next : warShip.getTransportBays()) {
            baydoors += next.getDoors();
            if (!next.isQuarters() && !(next instanceof InfantryBay) && !(next instanceof BattleArmorBay)) {
                bayCost += next.getCost();
            }
        }

        costs[costIdx++] += bayCost + (baydoors * 1000L);
        costs[costIdx++] = quartersCost;

        // Weapons and Equipment
        // HPG
        if (warShip.hasHPG()) {
            costs[costIdx++] += 1000000000;
        } else {
            costs[costIdx++] += 0;
        }
        // Weapons and Equipment
        costs[costIdx++] += CostCalculator.getWeaponsAndEquipmentCost(warShip, ignoreAmmo);

        // Sum Costs
        for (int i = 0; i < costIdx; i++) {
            cost += costs[i];
        }

        costs[costIdx] = -warShip.getPriceMultiplier(); // Negative indicates multiplier
        cost = Math.round(cost * warShip.getPriceMultiplier());

        String[] systemNames = { "Bridge", "Computer", "Life Support", "Sensors", "FCS", "Gunnery Control Systems",
                "Structural Integrity", "Drive Unit", "Engine", "Engine Control Unit",
                "KF Drive", "KF Drive Support System", "Attitude Thrusters", "Docking Collars",
                "Fuel Tanks", "Armor", "Heat Sinks", "Life Boats/Escape Pods", "Grav Decks",
                "Bays", "Quarters", "HPG", "Weapons/Equipment", "Weight Multiplier" };
        CostCalculator.fillInReport(costReport, warShip, ignoreAmmo, systemNames, 21, cost, costs);
        return cost;
    }
}
