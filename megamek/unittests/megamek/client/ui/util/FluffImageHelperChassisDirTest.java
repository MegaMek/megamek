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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the chassis and model subdirectory matching rules for fluff images, i.e. the contents of
 * fluff/[unittype]/[chassis] and fluff/[unittype]/[chassis]/[chassis model].
 */
class FluffImageHelperChassisDirTest {

    private static Mek mek(String chassis, String model) {
        Mek mek = new BipedMek();
        mek.setChassis(chassis);
        mek.setModel(model);
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
    void findsEveryImageInTheChassisDirectoryRegardlessOfFileName(@TempDir Path fluffDir) throws IOException {
        writeImage(fluffDir.resolve("Atlas"), "anything at all.png");
        writeImage(fluffDir.resolve("Atlas"), "second.jpg");

        List<File> found = FluffImageHelper.getFluffInChassisDirs(mek("Atlas", "AS7-D"), fluffDir.toFile());

        assertEquals(List.of("anything at all.png", "second.jpg"), fileNames(found));
    }

    @Test
    void modelDirectoryWinsOverChassisDirectory(@TempDir Path fluffDir) throws IOException {
        writeImage(fluffDir.resolve("Atlas"), "generic atlas.png");
        writeImage(fluffDir.resolve("Atlas").resolve("Atlas AS7-D"), "specific.png");

        List<File> found = FluffImageHelper.getFluffInChassisDirs(mek("Atlas", "AS7-D"), fluffDir.toFile());

        assertEquals(List.of("specific.png"), fileNames(found));
    }

    @Test
    void chassisDirectoryIsUsedWhenNoModelDirectoryMatches(@TempDir Path fluffDir) throws IOException {
        writeImage(fluffDir.resolve("Atlas"), "generic atlas.png");
        writeImage(fluffDir.resolve("Atlas").resolve("Atlas AS7-K"), "other model.png");

        List<File> found = FluffImageHelper.getFluffInChassisDirs(mek("Atlas", "AS7-D"), fluffDir.toFile());

        assertEquals(List.of("generic atlas.png"), fileNames(found));
    }

    @Test
    void emptyModelMatchesTheEmptyModelDirectory(@TempDir Path fluffDir) throws IOException {
        writeImage(fluffDir.resolve("Atlas"), "generic atlas.png");
        writeImage(fluffDir.resolve("Atlas").resolve("Atlas " + FluffImageHelper.EMPTY_MODEL_DIR_NAME),
              "no model.png");

        List<File> found = FluffImageHelper.getFluffInChassisDirs(mek("Atlas", ""), fluffDir.toFile());

        assertEquals(List.of("no model.png"), fileNames(found));
    }

    @Test
    void multipleImagesAreReturnedInDeterministicOrder(@TempDir Path fluffDir) throws IOException {
        writeImage(fluffDir.resolve("Atlas"), "charlie.png");
        writeImage(fluffDir.resolve("Atlas"), "alpha.png");
        writeImage(fluffDir.resolve("Atlas"), "bravo.png");

        List<File> firstCall = FluffImageHelper.getFluffInChassisDirs(mek("Atlas", "AS7-D"), fluffDir.toFile());
        List<File> secondCall = FluffImageHelper.getFluffInChassisDirs(mek("Atlas", "AS7-D"), fluffDir.toFile());

        assertEquals(List.of("alpha.png", "bravo.png", "charlie.png"), fileNames(firstCall));
        assertEquals(fileNames(firstCall), fileNames(secondCall));
    }

    @Test
    void nonImageFilesAreIgnored(@TempDir Path fluffDir) throws IOException {
        writeImage(fluffDir.resolve("Atlas"), "picture.png");
        writeImage(fluffDir.resolve("Atlas"), "picture data.yaml");
        writeImage(fluffDir.resolve("Atlas"), "readme.txt");

        List<File> found = FluffImageHelper.getFluffInChassisDirs(mek("Atlas", "AS7-D"), fluffDir.toFile());

        assertEquals(List.of("picture.png"), fileNames(found));
    }

    @Test
    void clanChassisVariantDirectoriesAreFound(@TempDir Path fluffDir) throws IOException {
        Mek timberWolf = mek("Mad Cat", "Prime");
        timberWolf.setClanChassisName("Timber Wolf");
        writeImage(fluffDir.resolve("Timber Wolf (Mad Cat)"), "timberwolf.png");

        List<File> found = FluffImageHelper.getFluffInChassisDirs(timberWolf, fluffDir.toFile());

        assertEquals(List.of("timberwolf.png"), fileNames(found));
    }

    @Test
    void aFileWhereAChassisDirectoryWouldBeIsIgnored(@TempDir Path fluffDir) throws IOException {
        // A stray file named like a chassis must not be treated as a directory to scan
        writeImage(fluffDir, "Atlas");

        List<File> found = FluffImageHelper.getFluffInChassisDirs(mek("Atlas", "AS7-D"), fluffDir.toFile());

        assertTrue(found.isEmpty(), "A regular file named like the chassis must not yield fluff images");
    }

    @Test
    void missingChassisDirectoryYieldsNoImages(@TempDir Path fluffDir) {
        List<File> found = FluffImageHelper.getFluffInChassisDirs(mek("Atlas", "AS7-D"), fluffDir.toFile());

        assertTrue(found.isEmpty());
    }
}
