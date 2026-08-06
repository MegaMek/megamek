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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;

import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Roundtrip save/load test for ALL units in the MekSummaryCache.
 * <p>
 * The test for each unit is: - Load the original unit from cache - Save it to a temp file (A) - Load file A, save it
 * again to file (B) - Compare A and B byte-for-byte, they must be identical (no data loss across the cycle)
 * <p>
 * Run with: ./gradlew :megamek:roundTripSaveUnitsTest
 */
@Tag("on-demand")
public class BulkRoundTripTest {

    @BeforeAll
    public static void init() {
        // MekSummaryCache.getInstance(true) initializes equipment types internally
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
    void roundTripSaveLoad(MekSummary summary) throws Exception {
        // Step 1: Load original from cache
        Entity original = summary.loadEntity();
        assertNotNull(original, "Failed to load entity: " + summary.getName());

        String suffix = (original instanceof Mek) ? ".mtf" : ".blk";

        // Step 2: Save to file A
        File fileA = File.createTempFile("roundtrip_A_", suffix);
        fileA.deleteOnExit();
        persistUnit(fileA, original);

        // Step 3: Load file A, save to file B
        Entity reloaded;
        try {
            MekFileParser mfp = new MekFileParser(fileA);
            reloaded = mfp.getEntity();
        } catch (Exception ex) {
            fail("Failed to reload unit " + summary.getName() + " from file A: " + ex.getMessage());
            return;
        }
        assertNotNull(reloaded, "Reloaded entity is null for: " + summary.getName());

        File fileB = File.createTempFile("roundtrip_B_", suffix);
        fileB.deleteOnExit();
        persistUnit(fileB, reloaded);

        // Step 4: Compare A and B, must be byte-identical
        byte[] bytesA = Files.readAllBytes(fileA.toPath());
        byte[] bytesB = Files.readAllBytes(fileB.toPath());

        if (!Arrays.equals(bytesA, bytesB)) {
            String contentA = new String(bytesA);
            String contentB = new String(bytesB);
            fail("Roundtrip data loss detected for " + summary.getName()
                  + "\n--- File A (first save) ---\n" + contentA
                  + "\n--- File B (second save) ---\n" + contentB);
        }
    }

    private static void persistUnit(File outFile, Entity entity) throws EntitySavingException {
        if (entity instanceof Mek mek) {
            try (BufferedWriter out = new BufferedWriter(new FileWriter(outFile))) {
                out.write(mek.getMtf());
            } catch (Exception e) {
                fail("Failed to save MTF for " + entity.getDisplayName() + ": " + e.getMessage());
            }
        } else {
            BLKFile.encode(outFile, entity);
        }
    }
}
