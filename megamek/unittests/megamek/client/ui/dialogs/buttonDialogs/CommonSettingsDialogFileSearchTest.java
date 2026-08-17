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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
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
package megamek.client.ui.dialogs.buttonDialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommonSettingsDialogFileSearchTest {
    @TempDir
    private Path tempDirectory;

    @Test
    void recursiveSearchReturnsMatchingFiles() throws IOException {
        Path nestedDirectory = Files.createDirectories(tempDirectory.resolve("nested"));
        Path rootMatch = Files.createFile(tempDirectory.resolve("root.xml"));
        Path nestedMatch = Files.createFile(nestedDirectory.resolve("nested.xml"));
        Files.createFile(nestedDirectory.resolve("ignored.txt"));

        List<String> matches = CommonSettingsDialog.filteredFilesWithSubDirs(tempDirectory.toFile(), ".xml");

        assertEquals(Set.of(rootMatch.toString(), nestedMatch.toString()), Set.copyOf(matches));
    }

    @Test
    void recursiveSearchContinuesAfterAccessFailure() throws IOException {
        List<String> matches = new ArrayList<>();
        SimpleFileVisitor<Path> visitor = CommonSettingsDialog.filteredFileVisitor(".ttf", matches);
        Path protectedDirectory = tempDirectory.resolve("protected");
        AccessDeniedException accessDenied = new AccessDeniedException(protectedDirectory.toString());
        Path readableFont = Files.createFile(tempDirectory.resolve("readable.ttf"));
        BasicFileAttributes attributes = Files.readAttributes(readableFont, BasicFileAttributes.class);

        assertEquals(FileVisitResult.CONTINUE, visitor.visitFileFailed(protectedDirectory, accessDenied));
        assertEquals(FileVisitResult.CONTINUE, visitor.visitFile(readableFont, attributes));

        assertEquals(List.of(readableFont.toString()), matches);
    }
}
