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
package megamek.client.ui.dialogs.advancedsearch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedSearchDialogTest {

    @Test
    void testSearchesShouldParseWithoutException() throws IOException, URISyntaxException {
        // Tests the searches in the testresources/searches folder

        Path resourceDir = Path.of(getClass().getClassLoader().getResource("searches").toURI());

        // When this test fails, any searches of players will also become useless. Consider providing migration code
        // for old searches!
        try (Stream<Path> files = Files.walk(resourceDir)) {
            files.filter(Files::isRegularFile)
                  .filter(p -> p.getFileName().toString().endsWith(".json"))
                  .forEach(p ->
                        assertDoesNotThrow(
                              () -> AdvancedSearchDialog.load(p.toFile()),
                              () -> "Failed for JSON file: " + p
                        )
                  );
        }
    }
}



