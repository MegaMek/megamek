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

package megamek.client.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that fluff image lookups reuse cached directory listings, so that showing a unit doesn't read the fluff
 * directories again each time.
 */
class FluffImageHelperDirectoryCacheTest {

    @BeforeEach
    @AfterEach
    void clearCache() {
        FluffImageHelper.clearDirectoryCache();
    }

    private static Mek atlas() {
        Mek mek = new BipedMek();
        mek.setChassis("Atlas");
        mek.setModel("AS7-D");
        return mek;
    }

    private static void writeImage(Path directory, String fileName) throws IOException {
        Files.createDirectories(directory);
        Files.write(directory.resolve(fileName), new byte[] { 1, 2, 3 });
    }

    private static List<String> fileNames(List<File> files) {
        return files.stream().map(File::getName).toList();
    }

    @Test
    void cachedListingIsReusedUntilTheCacheIsCleared(@TempDir Path fluffDir) throws IOException {
        writeImage(fluffDir.resolve("Atlas"), "first.png");
        assertEquals(List.of("first.png"), fileNames(FluffImageHelper.getFluffInChassisDirs(atlas(), fluffDir.toFile())));

        writeImage(fluffDir.resolve("Atlas"), "second.png");
        assertEquals(List.of("first.png"), fileNames(FluffImageHelper.getFluffInChassisDirs(atlas(), fluffDir.toFile())));

        FluffImageHelper.clearDirectoryCache();
        assertEquals(List.of("first.png", "second.png"),
              fileNames(FluffImageHelper.getFluffInChassisDirs(atlas(), fluffDir.toFile())));
    }

    @Test
    void missingDirectoryIsRememberedUntilTheCacheIsCleared(@TempDir Path fluffDir) throws IOException {
        assertTrue(FluffImageHelper.getFluffInChassisDirs(atlas(), fluffDir.toFile()).isEmpty());

        writeImage(fluffDir.resolve("Atlas"), "late.png");
        assertTrue(FluffImageHelper.getFluffInChassisDirs(atlas(), fluffDir.toFile()).isEmpty());

        FluffImageHelper.clearDirectoryCache();
        assertEquals(List.of("late.png"), fileNames(FluffImageHelper.getFluffInChassisDirs(atlas(), fluffDir.toFile())));
    }

    @Test
    void subdirectoriesWithImageNamesAreNotReturnedAsImages(@TempDir Path fluffDir) throws IOException {
        writeImage(fluffDir.resolve("Atlas"), "real.png");
        Files.createDirectories(fluffDir.resolve("Atlas").resolve("folder.png"));

        assertEquals(List.of("real.png"), fileNames(FluffImageHelper.getFluffInChassisDirs(atlas(), fluffDir.toFile())));
    }

    @Test
    void aFileWhereAChassisDirectoryIsExpectedIsIgnored(@TempDir Path fluffDir) throws IOException {
        Files.write(fluffDir.resolve("Atlas"), new byte[] { 1, 2, 3 });

        assertTrue(FluffImageHelper.getFluffInChassisDirs(atlas(), fluffDir.toFile()).isEmpty());
    }
}
