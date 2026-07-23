/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 */
package megamek.common.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CostCalculatorReportTest {
    private static final String UNIT_PATH = "testresources/megamek/common/units/";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void supportVehicleReportIdentifiesStructuralInputsAndUsesRoundedTotal() throws Exception {
        Tank supportVehicle = assertInstanceOf(Tank.class, loadUnit("Dromedary Water Transport.blk"));
        RecordingCalculationReport report = new RecordingCalculationReport();

        double returnedCost = CombatVehicleCostCalculator.calculateCost(supportVehicle, report, true);

        assertNotNull(report.result("Final Structural Cost"));
        assertNotNull(report.result("Chassis"));
        assertNotNull(report.result("Engine"));
        assertNotNull(report.result("Armor"));
        assertNull(report.result("Chassis"));
        assertNull(report.result("Engine"));
        assertNull(report.result("Armor"));
        assertEquals(NumberFormat.getInstance().format(returnedCost), report.total());
    }

    @Test
    void dropShipReportColumnsRemainAlignedForEveryDockingCollarType() throws Exception {
        CostRun noCollar = calculateDropShipCost(Dropship.COLLAR_NO_BOOM);
        CostRun standard = calculateDropShipCost(Dropship.COLLAR_STANDARD);
        CostRun prototype = calculateDropShipCost(Dropship.COLLAR_PROTOTYPE);

        assertEquals("N/A", noCollar.report().result("Docking Collar"));
        assertEquals(NumberFormat.getInstance().format(10000), standard.report().result("Docking Collar"));
        assertEquals(NumberFormat.getInstance().format(1010000), prototype.report().result("Docking Collar"));
        assertEquals(noCollar.report().result("Bays"), standard.report().result("Bays"));
        assertEquals(noCollar.report().result("Bays"), prototype.report().result("Bays"));
        assertEquals(noCollar.report().result("Quarters"), standard.report().result("Quarters"));
        assertEquals(noCollar.report().result("Life Boats/Escape Pods"),
              standard.report().result("Life Boats/Escape Pods"));
        assertEquals("x 28", noCollar.report().result("Final Multiplier"));
        assertEquals("x 28", standard.report().result("Final Multiplier"));
        assertEquals("x 28", prototype.report().result("Final Multiplier"));
        assertEquals(280000, standard.cost() - noCollar.cost());
        assertEquals(28280000, prototype.cost() - noCollar.cost());
        assertReportTotalMatchesReturnedCost(noCollar);
        assertReportTotalMatchesReturnedCost(standard);
        assertReportTotalMatchesReturnedCost(prototype);
    }

    private static CostRun calculateDropShipCost(int collarType) throws Exception {
        Dropship dropShip = assertInstanceOf(Dropship.class, loadUnit("Union (3055).blk"));
        dropShip.setCollarType(collarType);
        RecordingCalculationReport report = new RecordingCalculationReport();
        return new CostRun(DropShipCostCalculator.calculateCost(dropShip, report, true), report);
    }

    private static void assertReportTotalMatchesReturnedCost(CostRun run) {
        assertEquals(NumberFormat.getInstance().format(run.cost()), run.report().total());
    }

    private static Entity loadUnit(String filename) throws Exception {
        return new MekFileParser(new File(UNIT_PATH + filename)).getEntity();
    }

    private record CostRun(double cost, RecordingCalculationReport report) {}

    private static final class RecordingCalculationReport extends DummyCalculationReport {
        private final Map<String, String> results = new LinkedHashMap<>();
        private String total;

        @Override
        public CalculationReport addLine(String type, String calculation, String result) {
            results.put(type, result);
            return this;
        }

        @Override
        public CalculationReport addResultLine(String type, String calculation, String result) {
            if ("Total Cost:".equals(type)) {
                total = result;
            }
            return this;
        }

        String result(String type) {
            return results.get(type);
        }

        String total() {
            return total;
        }
    }
}