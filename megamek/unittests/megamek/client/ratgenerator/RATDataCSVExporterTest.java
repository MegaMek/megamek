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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import megamek.common.loaders.MekSummary;
import megamek.common.units.UnitType;

class RATDataCSVExporterTest {

    @Test
    void resolveFactionNameUsesFirstEraWithAvailability() {
        RATGenerator ratGenerator = new RATGenerator();
        FactionRecord faction = new FactionRecord("FS", "Federated Suns");
        faction.setName(3085, "Federated Commonwealth");
        ratGenerator.addFaction(faction);

        assertEquals("Federated Commonwealth",
              RATDataCSVExporter.resolveFactionName(
                    ratGenerator,
                    new Integer[] { 3050, 3085 },
                  new ArrayList<>(java.util.Arrays.asList(null, "7%")),
                    "FS"));
    }

    @Test
    void formatCalculatedAvailabilityUsesFractionalFormat() {
        assertEquals("0.125", RATDataCSVExporter.formatCalculatedAvailability(12.5));
    }

    @Test
    void formatCalculatedAvailabilitySupportsOneTenthPercent() {
        assertEquals("0.001", RATDataCSVExporter.formatCalculatedAvailability(0.1));
    }

    @Test
    void buildRawCsvIncludesAc2Carrier() {
        String csv = RATDataCSVExporter.buildRawCsv(createRawTestRatGenerator());

        assertTrue(csv.contains("AC/2 Carrier"));
    }

    @Test
    void buildRawCsvDoesNotUseExcelFormatting() {
        String csv = RATDataCSVExporter.buildRawCsv(createRawTestRatGenerator());

        assertFalse(csv.contains("\"=\"\""));
    }

    @Test
    void buildCalculatedCsvIncludesAc2Carrier() {
        String csv = buildCalculatedCsvForSingleModel("AC/2 Carrier");

        assertTrue(csv.contains("AC/2 Carrier"));
    }

    @Test
    void buildCalculatedCsvDoesNotUseExcelFormatting() {
        String csv = buildCalculatedCsvForSingleModel("AC/2 Carrier");

        assertFalse(csv.contains("\"=\"\""));
    }

    @Test
    void buildCalculatedCsvAddsSeparateSalvageColumns() {
        String csv = buildCalculatedCsvForSingleModel("AC/2 Carrier");
        String header = csv.substring(0, csv.indexOf('\n'));

        assertTrue(header.contains(":S"));
    }

    @Test
    void buildCalculatedCsvExpandsMultipleSalvageSourcesIntoFinalModelPercentages() {
        FactionRecord generatingFaction = createAlwaysActiveFaction("GEN", "General");
        FactionRecord salvageFactionOne = createAlwaysActiveFaction("SALV1", "Salvage One");
        FactionRecord salvageFactionTwo = createAlwaysActiveFaction("SALV2", "Salvage Two");
        ModelRecord localModel = createModelRecord("LocalChassis", "Local", "LocalChassis Local", UnitType.TANK, 1, 3025);
        ModelRecord salvageA1 = createModelRecord("SalvageA", "A1", "SalvageA A1", UnitType.TANK, 2, 3025);
        ModelRecord salvageA2 = createModelRecord("SalvageA", "A2", "SalvageA A2", UnitType.TANK, 3, 3025);
        ModelRecord salvageB1 = createModelRecord("SalvageB", "B1", "SalvageB B1", UnitType.TANK, 4, 3025);
        ModelRecord salvageB2 = createModelRecord("SalvageB", "B2", "SalvageB B2", UnitType.TANK, 5, 3025);

        RATGenerator ratGenerator = mock(RATGenerator.class);
        when(ratGenerator.getEraSet()).thenReturn(new TreeSet<>(List.of(3025)));
        when(ratGenerator.getModelList()).thenReturn(List.of(localModel, salvageA1, salvageA2, salvageB1, salvageB2));
        when(ratGenerator.getFactionList()).thenReturn(List.of(generatingFaction));
        when(ratGenerator.getFaction("GEN")).thenReturn(generatingFaction);

        Map<String, UnitTable> tablesByFaction = new HashMap<>();
        tablesByFaction.put("GEN", createUnitTableWithEntries(List.of(
              createUnitEntry(60, "LocalChassis Local"),
              createSalvageEntry(30, salvageFactionOne),
              createSalvageEntry(10, salvageFactionTwo))));
        tablesByFaction.put("SALV1", createUnitTableWithEntries(List.of(
              createUnitEntry(3, "SalvageA A1"),
              createUnitEntry(1, "SalvageA A2"))));
        tablesByFaction.put("SALV2", createUnitTableWithEntries(List.of(
              createUnitEntry(1, "SalvageB B1"),
              createUnitEntry(1, "SalvageB B2"))));

        String csv;
        try (MockedStatic<UnitTable> unitTable = mockStatic(UnitTable.class)) {
            unitTable.when(() -> UnitTable.findTable(any(Parameters.class)))
                  .thenAnswer(invocation -> {
                      Parameters params = invocation.getArgument(0);
                      return tablesByFaction.get(params.getFaction().getKey());
                  });
            csv = RATDataCSVExporter.buildCalculatedCsv(ratGenerator);
        }

        assertTrue(csv.contains("LocalChassis;Local;1;Tank;3025;GEN;General;0.600;;"));
        assertTrue(csv.contains("SalvageA;A1;2;Tank;3025;GEN;General;;0.225;"));
        assertTrue(csv.contains("SalvageA;A2;3;Tank;3025;GEN;General;;0.075;"));
        assertTrue(csv.contains("SalvageB;B1;4;Tank;3025;GEN;General;;0.050;"));
        assertTrue(csv.contains("SalvageB;B2;5;Tank;3025;GEN;General;;0.050;"));
    }

    @Test
    void calculatedModelExportOrderSortsEmptyModelBeforeNamedModels() {
        ModelRecord emptyModel = createModelRecord("AC/2 Carrier", "", UnitType.TANK, 1, 3025);
        when(emptyModel.getKey()).thenReturn("AC/2 Carrier");
        ModelRecord modelA = createModelRecord("AC/2 Carrier", "A", UnitType.TANK, 2, 3025);
        when(modelA.getKey()).thenReturn("AC/2 Carrier A");
        ModelRecord modelB = createModelRecord("AC/2 Carrier", "B", UnitType.TANK, 3, 3025);
        when(modelB.getKey()).thenReturn("AC/2 Carrier B");

        List<ModelRecord> sorted = new ArrayList<>(java.util.Arrays.asList(modelB, modelA, emptyModel));
        sorted.sort(RATDataCSVExporter.calculatedModelExportOrder());

        assertEquals(java.util.Arrays.asList(emptyModel, modelA, modelB), sorted);
    }

    @Test
    void resolveFactionNameUsesSalvageColumnWhenNormalColumnEmpty() {
        RATGenerator ratGenerator = new RATGenerator();
        FactionRecord faction = new FactionRecord("FS", "Federated Suns");
        faction.setName(3085, "Federated Commonwealth");
        ratGenerator.addFaction(faction);

        assertEquals("Federated Commonwealth",
              RATDataCSVExporter.resolveFactionName(
                    ratGenerator,
                    new Integer[] { 3050, 3085 },
                    new ArrayList<>(java.util.Arrays.asList(null, null, null, "7%")),
                    "FS"));
    }

    @Test
    void buildRawCsvIncludesModelVisibleOnlyThroughModelList() {
        RATGenerator ratGenerator = mock(RATGenerator.class);
        ChassisRecord chassis = new ChassisRecord("Tracked Test");
        chassis.setUnitType(UnitType.TANK);

        ModelRecord model = createModelRecord("Tracked Test", "", UnitType.TANK, 42, 3025);
        when(ratGenerator.getEraSet()).thenReturn(new TreeSet<>(List.of(3025)));
        when(ratGenerator.getChassisList()).thenReturn(List.of(chassis));
        when(ratGenerator.getModelList()).thenReturn(List.of(model));
        when(ratGenerator.getModelFactionRatings(3025, "Tracked Test")).thenReturn(
              List.of(new AvailabilityRating("Tracked Test", 3025, "General:6")));

        String csv = RATDataCSVExporter.buildRawCsv(ratGenerator);

        assertTrue(csv.contains("Model Data"));
        assertTrue(csv.contains("Tracked Test;;Model Data"));
    }

    @Test
    void buildRawCsvUsesRatingsIndexInsteadOfIncludedFactionsCache() {
        RATGenerator ratGenerator = mock(RATGenerator.class);
        ChassisRecord chassis = new ChassisRecord("Tracked Test");
        chassis.setUnitType(UnitType.TANK);

        ModelRecord model = createModelRecord("Tracked Test", "", UnitType.TANK, 42, 3025);
        chassis.addModel(model);

        when(ratGenerator.getEraSet()).thenReturn(new TreeSet<>(List.of(3025)));
        when(ratGenerator.getChassisList()).thenReturn(List.of(chassis));
        when(ratGenerator.getModelList()).thenReturn(List.of(model));
        when(ratGenerator.getModelFactionRatings(3025, "Tracked Test")).thenReturn(
              List.of(new AvailabilityRating("Tracked Test", 3025, "General:6")));

        String csv = RATDataCSVExporter.buildRawCsv(ratGenerator);

        assertTrue(csv.contains("General"));
        assertTrue(csv.contains("Tracked Test;;Model Data;42;Tank;3025;General;General;6;"));
    }

    private static ModelRecord createModelRecord(String chassis, String modelName, int unitType, int mulId, int year) {
        return createModelRecord(chassis, modelName, chassis, unitType, mulId, year);
    }

    private static ModelRecord createModelRecord(String chassis,
          String modelName,
          String key,
          int unitType,
          int mulId,
          int year) {
        ModelRecord model = mock(ModelRecord.class);
        MekSummary mekSummary = mock(MekSummary.class);
        when(model.getKey()).thenReturn(key);
        when(model.getChassis()).thenReturn(chassis);
        when(model.getModel()).thenReturn(modelName);
        when(model.getUnitType()).thenReturn(unitType);
        when(model.getMekSummary()).thenReturn(mekSummary);
        when(mekSummary.getMulId()).thenReturn(mulId);
        when(mekSummary.getYear()).thenReturn(year);
        return model;
    }

    private static RATGenerator createRawTestRatGenerator() {
        RATGenerator ratGenerator = mock(RATGenerator.class);
        ChassisRecord chassis = new ChassisRecord("AC/2 Carrier");
        chassis.setUnitType(UnitType.TANK);
        ModelRecord model = createModelRecord("AC/2 Carrier", "", UnitType.TANK, 42, 3025);
        FactionRecord faction = createAlwaysActiveFaction("General", "General");

        when(ratGenerator.getEraSet()).thenReturn(new TreeSet<>(List.of(3025)));
        when(ratGenerator.getChassisList()).thenReturn(List.of(chassis));
        when(ratGenerator.getModelList()).thenReturn(List.of(model));
        when(ratGenerator.getFaction("General")).thenReturn(faction);
        when(ratGenerator.getModelFactionRatings(3025, "AC/2 Carrier")).thenReturn(
              List.of(new AvailabilityRating("AC/2 Carrier", 3025, "General:6")));
        return ratGenerator;
    }

    private static String buildCalculatedCsvForSingleModel(String modelName) {
        RATGenerator ratGenerator = createCalculatedTestRatGenerator(modelName);
        try (MockedStatic<UnitTable> unitTable = mockStatic(UnitTable.class)) {
            unitTable.when(() -> UnitTable.findTable(any(Parameters.class)))
                  .thenAnswer(invocation -> createUnitTableWithSingleModel(modelName));
            return RATDataCSVExporter.buildCalculatedCsv(ratGenerator);
        }
    }

    private static RATGenerator createCalculatedTestRatGenerator(String modelName) {
        RATGenerator ratGenerator = mock(RATGenerator.class);
        FactionRecord faction = createAlwaysActiveFaction("General", "General");
        ModelRecord model = createModelRecord(modelName, "", UnitType.TANK, 42, 3025);

        when(ratGenerator.getEraSet()).thenReturn(new TreeSet<>(List.of(3025)));
        when(ratGenerator.getModelList()).thenReturn(List.of(model));
        when(ratGenerator.getFactionList()).thenReturn(List.of(faction));
        when(ratGenerator.getFaction("General")).thenReturn(faction);
        return ratGenerator;
    }

    private static UnitTable createUnitTableWithSingleModel(String modelName) {
        return createUnitTableWithEntries(List.of(createUnitEntry(100, modelName)));
    }

    private static UnitTable createUnitTableWithEntries(List<UnitTable.TableEntry> entries) {
        UnitTable unitTable = mock(UnitTable.class);
        when(unitTable.getEntries()).thenReturn(entries);
        return unitTable;
    }

    private static UnitTable.TableEntry createUnitEntry(int weight, String modelName) {
        MekSummary mekSummary = mock(MekSummary.class);
        when(mekSummary.getName()).thenReturn(modelName);
        return new UnitTable.TableEntry(weight, mekSummary);
    }

    private static UnitTable.TableEntry createSalvageEntry(int weight, FactionRecord factionRecord) {
        return new UnitTable.TableEntry(weight, factionRecord);
    }

    private static FactionRecord createAlwaysActiveFaction(String key, String name) {
        FactionRecord faction = new FactionRecord(key, name);
        try {
            faction.setYears(FactionRecord.ALWAYS_ACTIVE);
        } catch (ParseException ex) {
            throw new IllegalStateException("Unable to create test faction", ex);
        }
        return faction;
    }
}