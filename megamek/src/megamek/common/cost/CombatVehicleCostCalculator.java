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
import megamek.common.verifier.SupportVeeStructure;
import megamek.common.verifier.TestEntity;

import java.util.ArrayList;

public class CombatVehicleCostCalculator {

    public static double calculateCost(Tank tank, CalculationReport costReport, boolean ignoreAmmo) {
        double[] costs = new double[13 + tank.locations()];
        int i = 0;
        // Chassis cost for Support Vehicles
        if (tank.isSupportVehicle()) {
            double chassisCost = 2500 * SupportVeeStructure.getWeightStructure(tank);
            if (tank.hasMisc(MiscType.F_AMPHIBIOUS)) {
                chassisCost *= 1.25;
            }
            if (tank.hasMisc(MiscType.F_ARMORED_CHASSIS)) {
                chassisCost *= 2.0;
            }
            if (tank.hasMisc(MiscType.F_BICYCLE)) {
                chassisCost *= 0.75;
            }
            if (tank.hasMisc(MiscType.F_CONVERTIBLE)) {
                chassisCost *= 1.1;
            }
            if (tank.hasMisc(MiscType.F_DUNE_BUGGY)) {
                chassisCost *= 1.25;
            }
            if (tank.hasMisc(MiscType.F_ENVIRONMENTAL_SEALING)) {
                chassisCost *= 1.75;
            }
            if (tank.hasMisc(MiscType.F_EXTERNAL_POWER_PICKUP)) {
                chassisCost *= 1.1;
            }
            if (tank.hasMisc(MiscType.F_HYDROFOIL)) {
                chassisCost *= 1.1;
            }
            if (tank.hasMisc(MiscType.F_MONOCYCLE)) {
                chassisCost *= 1.3;
            }
            if (tank.hasMisc(MiscType.F_OFF_ROAD)) {
                chassisCost *= 1.2;
            }
            if (tank.hasMisc(MiscType.F_PROP)) {
                chassisCost *= 0.75;
            }
            if (tank.hasMisc(MiscType.F_SNOWMOBILE)) {
                chassisCost *= 1.3;
            }
            if (tank.hasMisc(MiscType.F_STOL_CHASSIS)) {
                chassisCost *= 1.5;
            }
            if (tank.hasMisc(MiscType.F_SUBMERSIBLE)) {
                chassisCost *= 3.5;
            }
            if (tank.hasMisc(MiscType.F_TRACTOR_MODIFICATION)) {
                chassisCost *= 1.1;
            }
            if (tank.hasMisc(MiscType.F_TRAILER_MODIFICATION)) {
                chassisCost *= 0.75;
            }
            if (tank.hasMisc(MiscType.F_ULTRA_LIGHT)) {
                chassisCost *= 1.5;
            }
            if (tank.hasMisc(MiscType.F_VSTOL_CHASSIS)) {
                chassisCost *= 2;
            }
            costs[i++] = chassisCost;
        }

        // Engine Costs
        double engineCost = 0.0;
        if (tank.hasEngine()) {
            if (tank.isSupportVehicle()) {
                engineCost = 5000 * tank.getEngine().getWeightEngine(tank)
                        * Engine.getSVCostMultiplier(tank.getEngine().getEngineType());
            } else {
                engineCost = (tank.getEngine().getBaseCost() *
                        tank.getEngine().getRating() * tank.getWeight()) / 75.0;
            }
        }
        costs[i++] = engineCost;

        // armor
        if (tank.isSupportVehicle()) {
            int totalArmorPoints = 0;
            for (int loc = 0; loc < tank.locations(); loc++) {
                totalArmorPoints += tank.getOArmor(loc);
            }
            costs[i++] = totalArmorPoints
                    * EquipmentType.getSupportVehicleArmorCostPerPoint(tank.getBARRating(tank.firstArmorIndex()));
        } else {
            if (tank.hasPatchworkArmor()) {
                for (int loc = 0; loc < tank.locations(); loc++) {
                    costs[i++] = tank.getArmorWeight(loc)
                            * EquipmentType.getArmorCost(tank.getArmorType(loc));
                }

            } else {
                costs[i++] = tank.getArmorWeight()
                        * EquipmentType.getArmorCost(tank.getArmorType(0));
            }
        }

        // Compute final structural cost
        int structCostIdx = 0;
        if (tank.isSupportVehicle()) {
            structCostIdx = i++;
            costs[structCostIdx] = 0;
            for (int c = 0; c < structCostIdx; c++) {
                costs[structCostIdx] += costs[c];
            }
            double techRatingMultiplier = 0.5 + (tank.getStructuralTechRating() * 0.25);
            costs[structCostIdx] *= techRatingMultiplier;
        } else {
            // IS has no variations, no Endo etc., but non-naval superheavies have heavier structure
            if (!tank.isSuperHeavy() || tank.getMovementMode().equals(EntityMovementMode.NAVAL)
                    || tank.getMovementMode().equals(EntityMovementMode.SUBMARINE)) { // There are no superheavy hydrofoils
                costs[i++] = RoundWeight.nextHalfTon(tank.getWeight() / 10.0) * 10000;
            } else {
                costs[i++] = RoundWeight.nextHalfTon(tank.getWeight() / 5.0) * 10000;
            }
            double controlWeight = tank.hasNoControlSystems() ? 0.0 : RoundWeight.nextHalfTon(tank.getWeight() * 0.05); // ?
            // should be rounded up to nearest half-ton
            costs[i++] = 10000 * controlWeight;
        }

        double freeHeatSinks = (tank.hasEngine() ? tank.getEngine().getWeightFreeEngineHeatSinks() : 0);
        int sinks = TestEntity.calcHeatNeutralHSRequirement(tank);
        double turretWeight = 0;
        double paWeight = tank.getPowerAmplifierWeight();
        for (Mounted m : tank.getWeaponList()) {
            if ((m.getLocation() == tank.getLocTurret()) || (m.getLocation() == tank.getLocTurret2())) {
                turretWeight += m.getTonnage() / 10.0;
                if ((m.getLinkedBy() != null) && (m.getLinkedBy().getType() instanceof MiscType)
                        && m.getLinkedBy().getType().hasFlag(MiscType.F_PPC_CAPACITOR)) {
                    turretWeight += m.getLinkedBy().getTonnage() / 10.0;
                }
            }
        }
        turretWeight = RoundWeight.standard(turretWeight, tank);
        costs[i++] = 20000 * paWeight;
        costs[i++] = 2000 * Math.max(0, sinks - freeHeatSinks);
        costs[i++] = turretWeight * 5000;

        costs[i++] = CostCalculator.getWeaponsAndEquipmentCost(tank, ignoreAmmo) + tank.getExtraCrewSeats() * 100L;

        if (!tank.isSupportVehicle()) {
            double diveTonnage;
            switch (tank.getMovementMode()) {
                case HOVER:
                case HYDROFOIL:
                case VTOL:
                case SUBMARINE:
                case WIGE:
                    diveTonnage = Math.ceil(tank.getWeight() / 5.0) / 2.0;
                    break;
                default:
                    diveTonnage = 0.0;
                    break;
            }
            if (tank.getMovementMode() != EntityMovementMode.VTOL) {
                costs[i++] = diveTonnage * 20000;
            } else {
                costs[i++] = diveTonnage * 40000;
            }
        }

        double cost = 0; // calculate the total
        for (int x = structCostIdx; x < i; x++) {
            cost += costs[x];
        }

        // TODO Decouple cost calculation from addCostDetails and eliminate duplicate code in getPriceMultiplier
        if (tank.isOmni()) { // Omni conversion cost goes here.
            cost *= 1.25;
            costs[i++] = -1.25;
        } else {
            costs[i++] = 0;
        }


        double multiplier = 1.0;
        if (tank.isSupportVehicle()
                && (tank.getMovementMode().equals(EntityMovementMode.NAVAL)
                || tank.getMovementMode().equals(EntityMovementMode.HYDROFOIL)
                || tank.getMovementMode().equals(EntityMovementMode.SUBMARINE))) {
            multiplier += tank.getWeight() / 100000.0;
        } else {
            switch (tank.getMovementMode()) {
                case HOVER:
                case SUBMARINE:
                    multiplier += tank.getWeight() / 50.0;
                    break;
                case HYDROFOIL:
                    multiplier += tank.getWeight() / 75.0;
                    break;
                case NAVAL:
                case WHEELED:
                    multiplier += tank.getWeight() / 200.0;
                    break;
                case TRACKED:
                    multiplier += tank.getWeight() / 100.0;
                    break;
                case VTOL:
                    multiplier += tank.getWeight() / 30.0;
                    break;
                case WIGE:
                    multiplier += tank.getWeight() / 25.0;
                    break;
                case RAIL:
                case MAGLEV:
                    multiplier += tank.getWeight() / 250.0;
                    break;
                default:
                    break;
            }
        }
        cost *= multiplier;
        costs[i++] = -multiplier;

        if (!tank.isSupportVehicle()) {
            if (tank.hasWorkingMisc(MiscType.F_FLOTATION_HULL)
                    || tank.hasWorkingMisc(MiscType.F_ENVIRONMENTAL_SEALING)) {
                cost *= 1.25;
                costs[i++] = -1.25;

            }
            if (tank.hasWorkingMisc(MiscType.F_OFF_ROAD)) {
                cost *= 1.2;
                costs[i] = -1.2;
            }
        }

        ArrayList<String> left = new ArrayList<>();
        if (tank.isSupportVehicle()) {
            left.add("Chassis");
        }
        left.add("Engine");
        left.add("Armor");
        if (tank.isSupportVehicle()) {
            left.add("Final Structural Cost");
        } else {
            left.add("Internal Structure");
            left.add("Control Systems");
        }
        left.add("Power Amplifiers");
        left.add("Heat Sinks");
        left.add("Turret");
        left.add("Equipment");
        if (!tank.isSupportVehicle()) {
            left.add("Lift Equipment");
        }
        left.add("Omni Multiplier");
        left.add("Tonnage Multiplier");
        if (!tank.isSupportVehicle()) {

            left.add("Flotation Hull/Environmental Sealing multiplier");
            left.add("Off-Road Multiplier");
        }
        String[] systemNames = left.toArray(new String[0]);
        CostCalculator.fillInReport(costReport, tank, ignoreAmmo, systemNames, 7, cost, costs);
        return Math.round(cost);
    }
}
