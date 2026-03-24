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
package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import megamek.common.units.Entity;
import megamek.common.verifier.TestEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import megamek.logging.MMLogger;

/**
 * Author: Drake
 * 
 * Bulk validation test for ALL units in the MekSummaryCache.
 *
 * Loads every unit, runs {@link TestEntity#correctEntity(StringBuffer)} on it,
 * and writes a report file listing failed units first, then passed units.
 * Failed units are also logged.
 *
 * Run with:
 *   ./gradlew :megamek:validateAllUnits
 */
@Tag("on-demand")
public class BulkValidationTest {

    private static final MMLogger logger = MMLogger.create(BulkValidationTest.class);

    private static final File REPORT_FILE = new File("build/reports/validateAllUnits.txt");

    private record ValidationResult(String unitName, String filePath, boolean passed, String reasons) {}

    private static final List<ValidationResult> results = java.util.Collections.synchronizedList(new ArrayList<>());

    @BeforeAll
    public static void init() {
        // MekSummaryCache.getInstance(true) initializes equipment types internally
        results.clear();
    }

    static Stream<MekSummary> allUnits() {
        MekSummaryCache cache = MekSummaryCache.getInstance(true);
        MekSummary[] allMeks = cache.getAllMeks();
        assertNotNull(allMeks, "MekSummaryCache returned null");
        assertTrue(allMeks.length > 0, "MekSummaryCache is empty: no units found");
        return Arrays.stream(allMeks);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allUnits")
    void validateUnit(MekSummary summary) {
        Entity entity = summary.loadEntity();
        assertNotNull(entity, "Failed to load entity: " + summary.getName());

        String filePath = summary.getSourceFile().getPath();
        String unitName = summary.getName();

        TestEntity testEntity = TestEntity.getEntityVerifier(entity);
        if (testEntity == null) {
            // No verifier available for this unit type — treat as passed
            results.add(new ValidationResult(unitName, filePath, true, ""));
            return;
        }

        StringBuffer buff = new StringBuffer();
        boolean valid = testEntity.correctEntity(buff);
        String reasons = buff.toString().trim();

        results.add(new ValidationResult(unitName, filePath, valid, reasons));

        if (!valid) {
            logger.warn("FAILED: {} [{}]\n{}", unitName, filePath, reasons);
        }
    }

    @AfterAll
    public static void writeReport() throws IOException {
        REPORT_FILE.getParentFile().mkdirs();

        List<ValidationResult> failed = results.stream().filter(r -> !r.passed()).toList();
        List<ValidationResult> passed = results.stream().filter(ValidationResult::passed).toList();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REPORT_FILE))) {
            writer.write("=== VALIDATION REPORT ===");
            writer.newLine();
            writer.write("Total: " + results.size() + "  Failed: " + failed.size() + "  Passed: " + passed.size());
            writer.newLine();
            writer.newLine();

            // Failed units first
            if (!failed.isEmpty()) {
                writer.write("=== FAILED ===");
                writer.newLine();
                writer.newLine();
                for (ValidationResult r : failed) {
                    writer.write(r.unitName() + " [" + r.filePath() + "] FAILED");
                    writer.newLine();
                    writer.write(r.reasons());
                    writer.newLine();
                    writer.newLine();
                }
            }

            // Passed units
            if (!passed.isEmpty()) {
                writer.write("=== PASSED ===");
                writer.newLine();
                writer.newLine();
                for (ValidationResult r : passed) {
                    writer.write(r.unitName() + " [" + r.filePath() + "] PASSED");
                    writer.newLine();
                }
            }
        }

        logger.info("Validation report written to {}", REPORT_FILE.getAbsolutePath());
        logger.info("Total: {}  Failed: {}  Passed: {}", results.size(), failed.size(), passed.size());
    }
}
