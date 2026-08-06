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

package megamek.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that resaving a unit carries its existing copyright year forward instead of overwriting it with the current
 * year. A resave that drops the earlier year erases the year the unit's content was first published, which happened
 * across the whole unit catalog once already.
 */
class UnitFileResaverTest {

    private static final int CURRENT_YEAR = Year.now().getValue();

    @TempDir
    private Path tempDir;

    private File writeUnitFile(String headerLine) throws IOException {
        Path unitFile = tempDir.resolve("TestUnit.mtf");
        Files.writeString(unitFile, headerLine + "\nchassis:Marauder\nmodel:MAD-3R\n", StandardCharsets.UTF_8);
        return unitFile.toFile();
    }

    @Test
    void earlierSingleYearBecomesRange() throws IOException {
        int earlierYear = CURRENT_YEAR - 1;
        File sourceFile = writeUnitFile("# MegaMek Data (C) " + earlierYear + " by The MegaMek Team is licensed"
              + " under CC BY-NC-SA 4.0.");

        assertEquals(earlierYear + "-" + CURRENT_YEAR, UnitFileResaver.copyrightYears(sourceFile));
    }

    @Test
    void existingRangeKeepsItsStartYear() throws IOException {
        int earlierYear = CURRENT_YEAR - 1;
        File sourceFile = writeUnitFile("# MegaMek Data (C) " + earlierYear + "-" + CURRENT_YEAR
              + " by The MegaMek Team is licensed under CC BY-NC-SA 4.0.");

        assertEquals(earlierYear + "-" + CURRENT_YEAR, UnitFileResaver.copyrightYears(sourceFile));
    }

    @Test
    void currentYearStaysASingleYear() throws IOException {
        File sourceFile = writeUnitFile("# MegaMek Data (C) " + CURRENT_YEAR + " by The MegaMek Team is licensed"
              + " under CC BY-NC-SA 4.0.");

        assertEquals(String.valueOf(CURRENT_YEAR), UnitFileResaver.copyrightYears(sourceFile));
    }

    @Test
    void fileWithoutHeaderGetsCurrentYear() throws IOException {
        File sourceFile = writeUnitFile("chassis:Archer");

        assertEquals(String.valueOf(CURRENT_YEAR), UnitFileResaver.copyrightYears(sourceFile));
    }

    @Test
    void archiveSourceGetsCurrentYearWithoutBeingRead() throws IOException {
        // A unit stored in an archive reports the archive as its source file. Even when the archive's bytes happen to
        // contain something that looks like a header, it must not be scanned as text.
        int earlierYear = CURRENT_YEAR - 1;
        Path archiveFile = tempDir.resolve("units.zip");
        Files.writeString(archiveFile, "# MegaMek Data (C) " + earlierYear + " by The MegaMek Team is licensed"
              + " under CC BY-NC-SA 4.0.\n", StandardCharsets.UTF_8);

        assertEquals(String.valueOf(CURRENT_YEAR), UnitFileResaver.copyrightYears(archiveFile.toFile()));
    }

    @Test
    void missingSourceGetsCurrentYear() {
        assertEquals(String.valueOf(CURRENT_YEAR), UnitFileResaver.copyrightYears(null));
        assertEquals(String.valueOf(CURRENT_YEAR),
              UnitFileResaver.copyrightYears(tempDir.resolve("DoesNotExist.mtf").toFile()));
    }
}
