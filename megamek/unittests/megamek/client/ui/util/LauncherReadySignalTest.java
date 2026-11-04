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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LauncherReadySignalTest {

    @TempDir
    Path temp;

    @Test
    void signalCreatesAnEmptyReadyFile() throws IOException {
        Path readyFile = temp.resolve("ready");

        LauncherReadySignal.signal(readyFile.toString());

        assertTrue(Files.isRegularFile(readyFile));
        assertEquals(0, Files.size(readyFile));
    }

    @Test
    void signalIgnoresAFileThatCannotBeCreated() {
        Path unreachable = temp.resolve("missing").resolve("ready");

        assertDoesNotThrow(() -> LauncherReadySignal.signal(unreachable.toString()));
    }

    @Test
    void installDoesNothingWithoutTheProperty() {
        String previous = System.clearProperty(LauncherReadySignal.READY_FILE_PROPERTY);
        try {
            assertDoesNotThrow(LauncherReadySignal::install);
        } finally {
            if (previous != null) {
                System.setProperty(LauncherReadySignal.READY_FILE_PROPERTY, previous);
            }
        }
    }
}
