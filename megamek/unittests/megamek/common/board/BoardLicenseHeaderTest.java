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
package megamek.common.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import megamek.common.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Board#save(java.io.OutputStream, boolean)} license header functionality. Verifies that CC BY-NC-SA
 * 4.0 headers are correctly added to board files (Issue #7936).
 */
class BoardLicenseHeaderTest {

    private Board board;

    @BeforeEach
    void setUp() {
        // Create a minimal 2x2 board for testing
        board = new Board(2, 2);
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                board.setHex(x, y, new Hex(0, "", ""));
            }
        }
    }

    @Nested
    @DisplayName("License Header Inclusion Tests")
    class LicenseHeaderInclusionTests {

        @Test
        @DisplayName("save(os, true) includes license header at start of file")
        void saveWithLicenseIncludesHeader() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            board.save(outputStream, true);

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(output.startsWith("# MegaMek Data"),
                  "Board file should start with license header when includeLicense=true");
            assertTrue(output.contains("CC BY-NC-SA 4.0"),
                  "License header should mention CC BY-NC-SA 4.0");
        }

        @Test
        @DisplayName("save(os, false) does not include license header")
        void saveWithoutLicenseExcludesHeader() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            board.save(outputStream, false);

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(output.startsWith("size "),
                  "Board file should start with 'size' when includeLicense=false");
            assertFalse(output.contains("# MegaMek Data"),
                  "Board file should not contain license header when includeLicense=false");
        }

        @Test
        @DisplayName("License header contains current year")
        void licenseHeaderContainsCurrentYear() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);

            board.save(outputStream, true);

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("(C) " + currentYear),
                  "License header should contain current year: " + currentYear);
        }
    }

    @Nested
    @DisplayName("Round-Trip Tests")
    class RoundTripTests {

        @Test
        @DisplayName("Board data survives save with header and reload")
        void boardDataSurvivesRoundTrip() {
            // Set up board with specific data
            board.setHex(0, 0, new Hex(2, "woods:1", "grass"));
            board.setHex(1, 1, new Hex(-1, "water:2", ""));
            board.addTag("TestTag");

            // Save with license header
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            board.save(outputStream, true);

            // Reload the board
            Board reloadedBoard = new Board();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            List<String> errors = new ArrayList<>();
            reloadedBoard.load(inputStream, errors, true);

            // Verify no load errors
            assertTrue(errors.isEmpty(), "Board should load without errors");

            // Verify board dimensions
            assertEquals(2, reloadedBoard.getWidth(), "Width should be preserved");
            assertEquals(2, reloadedBoard.getHeight(), "Height should be preserved");

            // Verify hex data
            assertEquals(2, reloadedBoard.getHex(0, 0).getLevel(), "Hex level should be preserved");
            assertEquals(-1, reloadedBoard.getHex(1, 1).getLevel(), "Negative hex level should be preserved");

            // Verify tags
            assertTrue(reloadedBoard.getTags().contains("TestTag"), "Tags should be preserved");
        }

        @Test
        @DisplayName("Header is not duplicated on re-save")
        void headerNotDuplicatedOnResave() {
            // Save with header
            ByteArrayOutputStream firstSave = new ByteArrayOutputStream();
            board.save(firstSave, true);

            // Reload
            Board reloadedBoard = new Board();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(firstSave.toByteArray());
            reloadedBoard.load(inputStream, null, true);

            // Save again with header
            ByteArrayOutputStream secondSave = new ByteArrayOutputStream();
            reloadedBoard.save(secondSave, true);

            // Count occurrences of header start
            String output = secondSave.toString(StandardCharsets.UTF_8);
            int headerCount = countOccurrences(output, "# MegaMek Data (C)");

            assertEquals(1, headerCount, "License header should appear exactly once after re-save");
        }

        private int countOccurrences(String text, String pattern) {
            int count = 0;
            int index = 0;
            while ((index = text.indexOf(pattern, index)) != -1) {
                count++;
                index += pattern.length();
            }
            return count;
        }
    }
}
