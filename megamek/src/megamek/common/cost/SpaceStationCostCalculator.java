/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.common.cost;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.SpaceStation;
import megamek.common.bays.BattleArmorBay;
import megamek.common.bays.Bay;
import megamek.common.bays.InfantryBay;
import megamek.common.equipment.ArmorType;

public class SpaceStationCostCalculator {

    public static double calculateCost(SpaceStation spaceStation, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[21];
        int costIdx = 0;
        double cost = 0;

        // Control Systems
        // Bridge
        costs[costIdx++] += 200000 + 10 * spaceStation.getWeight();
        // Computer
        costs[costIdx++] += 200000;
        // Life Support
        costs[costIdx++] += 5000 * (spaceStation.getNCrew() + spaceStation.getNPassenger());
        // Sensors
        costs[costIdx++] += 80000;
        // Fire Control Computer
        costs[costIdx++] += 100000;
        // Gunnery Control Systems
        costs[costIdx++] += 10000 * spaceStation.getArcswGuns();
        // Structural Integrity
        costs[costIdx++] += 100000 * spaceStation.getSI();

        // Station-Keeping Drive
        // Engine
        costs[costIdx++] += 1000 * spaceStation.getWeight() * 0.012;
        // Engine Control Unit
        costs[costIdx++] += 1000;

        // Additional Ships Systems
        // Attitude Thrusters
        costs[costIdx++] += 25000;
        // Docking Collars
        costs[costIdx++] += 100000 * spaceStation.getDocks();
        // Fuel Tanks
        costs[costIdx++] += (200 * spaceStation.getFuel()) / spaceStation.getFuelPerTon() * 1.02;

        // Armor
        costs[costIdx++] += spaceStation.getArmorWeight() * ArmorType.forEntity(spaceStation).getCost();

        // Heat Sinks
        int sinkCost = 2000 + (4000 * spaceStation.getHeatType());
        costs[costIdx++] += sinkCost * spaceStation.getHeatSinks();

        // Escape Craft
        costs[costIdx++] += 5000 * (spaceStation.getLifeBoats() + spaceStation.getEscapePods());

        // Grav Decks
        double deckCost = 0;
        deckCost += 5000000 * spaceStation.getGravDeck();
        deckCost += 10000000 * spaceStation.getGravDeckLarge();
        deckCost += 40000000 * spaceStation.getGravDeckHuge();
        costs[costIdx++] += deckCost;

        // Transport Bays
        int bayDoors = 0;
        long bayCost = 0;
        long quartersCost = 0;
        // Passenger and crew quarters and infantry bays are considered part of the structure
        // and don't add to the cost
        for (Bay next : spaceStation.getTransportBays()) {
            bayDoors += next.getDoors();
            if (!next.isQuarters() && !(next instanceof InfantryBay) && !(next instanceof BattleArmorBay)) {
                bayCost += next.getCost();
            }
        }

        costs[costIdx++] += bayCost + (bayDoors * 1000L);
        costs[costIdx++] = quartersCost;

        // Weapons and Equipment
        // HPG
        if (spaceStation.hasHPG()) {
            costs[costIdx++] += 1000000000;
        } else {
            costs[costIdx++] += 0;
        }
        // Weapons and Equipment
        costs[costIdx++] += CostCalculator.getWeaponsAndEquipmentCost(spaceStation, ignoreAmmo);

        // Sum Costs
        for (int i = 0; i < costIdx; i++) {
            cost += costs[i];
        }

        costs[costIdx] = -spaceStation.getPriceMultiplier(); // Negative indicates multiplier
        cost = Math.round(cost * spaceStation.getPriceMultiplier());

        String[] systemNames = { "Bridge", "Computer", "Life Support", "Sensors", "FCS", "Gunnery Control Systems",
                                 "Structural Integrity", "Engine", "Engine Control Unit", "Attitude Thrusters",
                                 "Docking Collars", "Fuel Tanks", "Armor", "Heat Sinks", "Life Boats/Escape Pods",
                                 "Grav Decks", "Bays", "Quarters", "HPG", "Weapons/Equipment", "Weight Multiplier" };
        CostCalculator.fillInReport(costReport, spaceStation, ignoreAmmo, systemNames, 18, cost, costs);
        return cost;
    }
}
