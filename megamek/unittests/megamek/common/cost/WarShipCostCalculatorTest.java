/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Jumpship;
import megamek.common.units.Warship;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Engine and jump sail costs are priced on the ship's actual engine and sail tonnage (SO:AA p.146). Primitive
 * JumpShips have heavier engines and sails before 2300, and none had a sail before 2200 (IO:AE p.123-124).
 */
class WarShipCostCalculatorTest {

    /** SO:AA p.146: WarShips, and Primitive JumpShips costed as WarShips, use a final multiplier of 2. */
    private static final double WARSHIP_MULTIPLIER = 2.0;
    /** SO:AA p.146: a compact core multiplies all K-F drive components, including the sail, by 5. */
    private static final double COMPACT_CORE_MULTIPLIER = 5.0;
    /** SO:AA p.146: Sail costs 50,000 x Sail Tonnage. */
    private static final double SAIL_COST_PER_TON = 50000.0;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void primitiveEngineBefore2150CostsTheHeavierPrimitiveEngine() {
        // IO:AE p.123: the 2148 Aquilla example, 100,000 tons x Safe Thrust 1 x 0.120 = 12,000-ton engine
        Warship ship = createWarship(100000, 1, 2148, Jumpship.DRIVE_CORE_PRIMITIVE, false);
        RecordingCalculationReport report = new RecordingCalculationReport();

        WarShipCostCalculator.calculateCost(ship, report, true);

        assertEquals(format(12000 * 1000.0), report.result("Engine"));
    }

    @Test
    void modernWarShipEngineCostIsUnchanged() {
        // SO:AA p.135: 480,000 tons x Safe Thrust 2 x 0.06 = 57,600-ton engine
        Warship ship = createWarship(480000, 2, 2520, Jumpship.DRIVE_CORE_COMPACT, true);
        RecordingCalculationReport report = new RecordingCalculationReport();

        WarShipCostCalculator.calculateCost(ship, report, true);

        assertEquals(format(57600 * 1000.0), report.result("Engine"));
    }

    @Test
    void primitiveShipWithoutSailPaysNoSailCost() {
        // IO:AE p.123: no K-F ship mounted a jump sail before 2200, so a 2148 ship's sail costs nothing
        Warship ship = createWarship(100000, 1, 2148, Jumpship.DRIVE_CORE_PRIMITIVE, false);
        RecordingCalculationReport report = new RecordingCalculationReport();

        WarShipCostCalculator.calculateCost(ship, report, true);

        // No docking collars and no sail: coil 60M + initiator 25M + controller 50M + charging 0.5M, all x 5
        assertEquals(format((60000000.0 + 25000000.0 + 50000000.0 + 500000.0) * COMPACT_CORE_MULTIPLIER),
              report.result("KF Drive"));
    }

    @Test
    void primitiveSailCostUsesThePrimitiveSailWeight() {
        // IO:AE p.124: 2230-2259 primitive sail weighs 150 + (Tonnage / 4,000); 150 + 25 = 175 tons
        Warship withoutSail = createWarship(100000, 1, 2240, Jumpship.DRIVE_CORE_PRIMITIVE, false);
        Warship withSail = createWarship(100000, 1, 2240, Jumpship.DRIVE_CORE_PRIMITIVE, true);

        assertEquals(expectedSailCost(175), cost(withSail) - cost(withoutSail), 0.5);
    }

    @Test
    void modernWarShipSailCostUsesTheModernSailWeight() {
        // SO:AA p.137: WarShip sail weighs 30 + (Tonnage / 20,000); 30 + 24 = 54 tons
        Warship withoutSail = createWarship(480000, 2, 2520, Jumpship.DRIVE_CORE_COMPACT, false);
        Warship withSail = createWarship(480000, 2, 2520, Jumpship.DRIVE_CORE_COMPACT, true);

        assertEquals(expectedSailCost(54), cost(withSail) - cost(withoutSail), 0.5);
    }

    private static double expectedSailCost(double sailTons) {
        return SAIL_COST_PER_TON * sailTons * COMPACT_CORE_MULTIPLIER * WARSHIP_MULTIPLIER;
    }

    private static Warship createWarship(double tonnage, int safeThrust, int buildYear, int driveCoreType,
          boolean hasSail) {
        Warship ship = new Warship();
        ship.setWeight(tonnage);
        ship.setOriginalWalkMP(safeThrust);
        ship.setYear(buildYear);
        ship.setOriginalBuildYear(buildYear);
        ship.setDriveCoreType(driveCoreType);
        ship.setSail(hasSail);
        return ship;
    }

    private static double cost(Warship ship) {
        return WarShipCostCalculator.calculateCost(ship, new DummyCalculationReport(), true);
    }

    private static String format(double value) {
        NumberFormat commaFormatter = NumberFormat.getInstance();
        commaFormatter.setMaximumFractionDigits(3);
        return commaFormatter.format(value);
    }

    private static final class RecordingCalculationReport extends DummyCalculationReport {
        private final Map<String, String> results = new LinkedHashMap<>();

        @Override
        public CalculationReport addLine(String type, String calculation, String result) {
            results.put(type, result);
            return this;
        }

        String result(String type) {
            return results.get(type);
        }
    }
}
