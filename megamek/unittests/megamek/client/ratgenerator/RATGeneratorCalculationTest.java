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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import megamek.common.loaders.MekSummary;
import megamek.common.units.UnitType;

class RATGeneratorCalculationTest {

    @Test
    void generateTableDistributesWeightsAcrossMultipleChassisAndModels() {
        GeneratorFixture fixture = createCompetingFixture();

        Map<String, Integer> weightsByName;
        try (MockedStatic<RATGenerator> ratGeneratorStatic = mockStatic(RATGenerator.class)) {
            ratGeneratorStatic.when(RATGenerator::getInstance).thenReturn(fixture.ratGenerator());
            List<UnitTable.TableEntry> entries = fixture.ratGenerator().generateTable(fixture.generatingFaction(),
                  UnitType.TANK,
                  3025,
                  null,
                  List.of(),
                  0,
                  List.of(),
                  List.of(),
                  0,
                  fixture.generatingFaction());
            weightsByName = extractWeightsByName(entries);
        }

        assertEquals(Map.of(
              "Alpha A1", 44,
              "Alpha A2", 22,
              "Beta B1", 27,
              "Beta B2", 7),
              weightsByName);
    }

    @Test
    void generateTableAddsCompetingSalvageWeightsAlongsideUnitWeights() {
        GeneratorFixture fixture = createCompetingFixture();
        fixture.generatingFaction().setPctSalvage(3025, 20);
        fixture.generatingFaction().getSalvage(3025);
        fixture.generatingFaction().setSalvage(3025, fixture.salvageFactionOne().getKey(), 3);
        fixture.generatingFaction().setSalvage(3025, fixture.salvageFactionTwo().getKey(), 1);

        Map<String, Integer> weightsByName;
        try (MockedStatic<RATGenerator> ratGeneratorStatic = mockStatic(RATGenerator.class)) {
            ratGeneratorStatic.when(RATGenerator::getInstance).thenReturn(fixture.ratGenerator());
            List<UnitTable.TableEntry> entries = fixture.ratGenerator().generateTable(fixture.generatingFaction(),
                  UnitType.TANK,
                  3025,
                  null,
                  List.of(),
                  0,
                  List.of(),
                  List.of(),
                  0,
                  fixture.generatingFaction());
            weightsByName = extractWeightsByName(entries);
        }

        assertEquals(Map.of(
              "Alpha A1", 44,
              "Alpha A2", 22,
              "Beta B1", 27,
              "Beta B2", 7,
              "SALV1", 15,
              "SALV2", 5),
              weightsByName);
    }

    private static GeneratorFixture createCompetingFixture() {
        RATGenerator ratGenerator = new RATGenerator();
        initializeEra(ratGenerator, 3025);

        FactionRecord generatingFaction = createAlwaysActiveFaction("GEN", "General");
        FactionRecord salvageFactionOne = createAlwaysActiveFaction("SALV1", "Salvage One");
        FactionRecord salvageFactionTwo = createAlwaysActiveFaction("SALV2", "Salvage Two");
        ratGenerator.addFaction(generatingFaction);
        ratGenerator.addFaction(salvageFactionOne);
        ratGenerator.addFaction(salvageFactionTwo);

        ChassisRecord alpha = createChassisRecord("Alpha", UnitType.TANK, 3025);
        ChassisRecord beta = createChassisRecord("Beta", UnitType.TANK, 3025);
        TestModelRecord alphaA1 = createModel("Alpha", "A1", UnitType.TANK, 3025);
        TestModelRecord alphaA2 = createModel("Alpha", "A2", UnitType.TANK, 3025);
        TestModelRecord betaB1 = createModel("Beta", "B1", UnitType.TANK, 3025);
        TestModelRecord betaB2 = createModel("Beta", "B2", UnitType.TANK, 3025);

        alpha.addModel(alphaA1);
        alpha.addModel(alphaA2);
        beta.addModel(betaB1);
        beta.addModel(betaB2);

        registerChassis(ratGenerator, alpha);
        registerChassis(ratGenerator, beta);
        registerModel(ratGenerator, alphaA1);
        registerModel(ratGenerator, alphaA2);
        registerModel(ratGenerator, betaB1);
        registerModel(ratGenerator, betaB2);

        ratGenerator.setChassisFactionRating(3025, alpha.getKey(), new AvailabilityRating(alpha.getKey(), 3025, "GEN:6"));
        ratGenerator.setChassisFactionRating(3025, beta.getKey(), new AvailabilityRating(beta.getKey(), 3025, "GEN:4"));
        ratGenerator.setModelFactionRating(3025, alphaA1.getKey(), new AvailabilityRating(alphaA1.getKey(), 3025, "GEN:6"));
        ratGenerator.setModelFactionRating(3025, alphaA2.getKey(), new AvailabilityRating(alphaA2.getKey(), 3025, "GEN:4"));
        ratGenerator.setModelFactionRating(3025, betaB1.getKey(), new AvailabilityRating(betaB1.getKey(), 3025, "GEN:6"));
        ratGenerator.setModelFactionRating(3025, betaB2.getKey(), new AvailabilityRating(betaB2.getKey(), 3025, "GEN:2"));

        return new GeneratorFixture(ratGenerator, generatingFaction, salvageFactionOne, salvageFactionTwo);
    }

    private static ChassisRecord createChassisRecord(String chassisName, int unitType, int introYear) {
        ChassisRecord chassisRecord = new ChassisRecord(chassisName);
        chassisRecord.setUnitType(unitType);
        chassisRecord.setIntroYear(introYear);
        return chassisRecord;
    }

    private static TestModelRecord createModel(String chassisName, String modelName, int unitType, int introYear) {
        return new TestModelRecord(chassisName, modelName, unitType, introYear);
    }

    private static void initializeEra(RATGenerator ratGenerator, int era) {
        ratGenerator.getEraSet().add(era);
        getChassisIndex(ratGenerator).put(era, new HashMap<>());
        getModelIndex(ratGenerator).put(era, new HashMap<>());
    }

    private static void registerChassis(RATGenerator ratGenerator, ChassisRecord chassisRecord) {
        getChassisMap(ratGenerator).put(chassisRecord.getKey(), chassisRecord);
    }

    private static void registerModel(RATGenerator ratGenerator, ModelRecord modelRecord) {
        getModelMap(ratGenerator).put(modelRecord.getKey(), modelRecord);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(RATGenerator ratGenerator, String fieldName) {
        try {
            Field field = RATGenerator.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(ratGenerator);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access RATGenerator field " + fieldName, ex);
        }
    }

    private static Map<String, ChassisRecord> getChassisMap(RATGenerator ratGenerator) {
        return getPrivateField(ratGenerator, "chassis");
    }

    private static Map<String, ModelRecord> getModelMap(RATGenerator ratGenerator) {
        return getPrivateField(ratGenerator, "models");
    }

    private static Map<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> getChassisIndex(
          RATGenerator ratGenerator) {
        return getPrivateField(ratGenerator, "chassisIndex");
    }

    private static Map<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> getModelIndex(
          RATGenerator ratGenerator) {
        return getPrivateField(ratGenerator, "modelIndex");
    }

    private static Map<String, Integer> extractWeightsByName(List<UnitTable.TableEntry> entries) {
        Map<String, Integer> weightsByName = new HashMap<>();
        for (UnitTable.TableEntry entry : entries) {
            if (entry.isUnit()) {
                weightsByName.put(entry.getUnitEntry().getName(), entry.getWeight());
            } else {
                weightsByName.put(entry.getSalvageFaction().getKey(), entry.getWeight());
            }
        }
        return weightsByName;
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

    private record GeneratorFixture(RATGenerator ratGenerator,
                                    FactionRecord generatingFaction,
                                    FactionRecord salvageFactionOne,
                                    FactionRecord salvageFactionTwo) {
    }

    private static final class TestModelRecord extends ModelRecord {
        private final String key;
        private final String modelName;
        private final MekSummary mekSummary;

        private TestModelRecord(String chassisName, String modelName, int unitType, int introYear) {
            super(chassisName, modelName);
            this.key = (chassisName + " " + modelName).trim();
            this.modelName = modelName;
            setUnitType(unitType);
            setIntroYear(introYear);
            mekSummary = mock(MekSummary.class);
            when(mekSummary.getName()).thenReturn(key);
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getModel() {
            return modelName;
        }

        @Override
        public MekSummary getMekSummary() {
            return mekSummary;
        }
    }
}