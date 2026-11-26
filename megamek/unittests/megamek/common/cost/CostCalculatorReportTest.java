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

import java.io.File;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.TechConstants;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.BipedMek;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        assertEquals(NumberFormat.getInstance().format(returnedCost), report.total());
    }

    @Test
    void combatVehicleReportKeepsOffRoadMultiplierInItsOwnColumn() throws Exception {
        Tank tank = assertInstanceOf(Tank.class, loadUnit("Bulldog Medium Tank.blk"));
        tank.addEquipment(EquipmentType.get("ISOffRoadChassis"), Tank.LOC_BODY);
        RecordingCalculationReport report = new RecordingCalculationReport();

        double returnedCost = CombatVehicleCostCalculator.calculateCost(tank, report, true);

        assertEquals("N/A", report.result("Flotation Hull/Environmental Sealing Multiplier"));
        assertEquals("x 1.2", report.result("Off-Road Multiplier"));
        assertReportTotalMatchesReturnedCost(returnedCost, report);
    }

    @Test
    void combatVehicleReportExplainsExtraCrewSeatCost() throws Exception {
        Tank tank = assertInstanceOf(Tank.class, loadUnit("Bulldog Medium Tank.blk"));
        tank.setExtraCrewSeats(3);
        RecordingCalculationReport report = new RecordingCalculationReport();

        double returnedCost = CombatVehicleCostCalculator.calculateCost(tank, report, true);

        assertEquals("300", report.result("Extra Crew Seats"));
        assertReportTotalMatchesReturnedCost(returnedCost, report);
    }

    @Test
    void variableTurretCostUsesTheMountedLocation() throws Exception {
        Mek mek = assertInstanceOf(Mek.class, loadUnit("Quickdraw QKD-8X.mtf"));
        RecordingCalculationReport report = new RecordingCalculationReport();

        double returnedCost = MekCostCalculator.calculateCost(mek, report, true);

        Mounted<?> headTurret = mek.getMisc().stream()
              .filter(mount -> mount.getType().hasFlag(MiscType.F_HEAD_TURRET))
              .findFirst()
              .orElseThrow();
        assertEquals(10000, headTurret.getCost());
        assertReportTotalMatchesReturnedCost(returnedCost, report);
    }

    @Test
    void frankenMekGyroCostUsesRetainedEngineRating() {
        Mek mek = new BipedMek();
        mek.setWeight(50);
        mek.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
        mek.setFrankenMek(true);
        mek.setEngine(new Engine(245, Engine.NORMAL_ENGINE, 0));
        mek.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        RecordingCalculationReport report = new RecordingCalculationReport();

        assertEquals(4, mek.getOriginalWalkMP());
        mek.getCost(report, true);
        assertEquals(NumberFormat.getInstance().format(900000), report.result("Gyro"));
    }

    @Test
    void jumpJetCostCountsTheInstalledJetsEvenWhenModularArmorCostsJumpMP() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(50);
        mek.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        mek.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        for (int location : new int[] { Mek.LOC_LEFT_LEG, Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG, Mek.LOC_RIGHT_LEG }) {
            mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.JUMP_JET), location);
        }
        mek.addEquipment(EquipmentType.get("ISModularArmor"), Mek.LOC_CENTER_TORSO);
        RecordingCalculationReport report = new RecordingCalculationReport();

        assertEquals(3, mek.getOriginalJumpMP());
        mek.getCost(report, true);
        assertEquals(NumberFormat.getInstance().format(160000), report.result("Jump Jets"));
    }

    /**
     * Four jets from a 50-ton donor torso give a 100-ton FrankenMek 1 Jumping MP (CO p. 213). Primitive prototype jets
     * cost tonnage x 200 x Jumping MP squared (IO:AE p. 181), standard jets 200 x jets squared x tonnage (TM p. 278).
     */
    @ParameterizedTest
    @CsvSource({ EquipmentTypeLookup.PROTOTYPE_JUMP_JET + ", 20000", EquipmentTypeLookup.JUMP_JET + ", 320000" })
    void primitivePrototypeJumpJetsCostByTheJumpingMPTheyGive(String jumpJet, int cost) throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(100);
        mek.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
        mek.setEngine(new Engine(400, Engine.NORMAL_ENGINE, 0));
        mek.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        mek.setFrankenMek(true);
        mek.setFrankenMekStructureTonnage(Mek.LOC_CENTER_TORSO, 100);
        mek.setFrankenMekStructureTonnage(Mek.LOC_LEFT_TORSO, 50);
        for (int jet = 0; jet < 4; jet++) {
            mek.addEquipment(EquipmentType.get(jumpJet), Mek.LOC_LEFT_TORSO);
        }
        RecordingCalculationReport report = new RecordingCalculationReport();

        assertEquals(1, mek.getOriginalJumpMP(true));
        mek.getCost(report, true);
        assertEquals(NumberFormat.getInstance().format(cost), report.result("Jump Jets"));
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

    private static void assertReportTotalMatchesReturnedCost(double cost, RecordingCalculationReport report) {
        assertEquals(NumberFormat.getInstance().format(cost), report.total());
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
            if ("Total Cost (C-bills)".equals(type)) {
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
