/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author nderwin
 */
class GameOptionsTest {

    private GameOptions testMe;

    @TempDir
    private Path tempDirectory;

    @BeforeEach
    void beforeEach() {
        testMe = new GameOptions();
    }

    @Test
    void testSaveAndLoadOptions() throws IOException {
        assertTrue(Files.isDirectory(tempDirectory));
        final Path createdFilePath = Files.createFile(tempDirectory.resolve("test-game-options.xml"));
        final File file = createdFilePath.toFile();

        Vector<IBasicOption> options = new Vector<>();
        Enumeration<IOption> opts = testMe.getOptions();
        int count = 0;
        while (opts.hasMoreElements()) {
            IOption io = opts.nextElement();

            switch (io.getType()) {
                case IOption.STRING:
                case IOption.CHOICE:
                    io.setValue("" + count);
                    break;

                case IOption.BOOLEAN:
                    if (count % 2 == 0) {
                        io.setValue(Boolean.TRUE);
                    } else {
                        io.setValue(Boolean.FALSE);
                    }
                    break;

                case IOption.INTEGER:
                    io.setValue(count);
                    break;

                case IOption.FLOAT:
                    io.setValue(Float.valueOf("" + count));
                    break;
            }

            options.add(io);
            count++;
        }

        GameOptions.saveOptions(options, file.getAbsolutePath());

        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        testMe.loadOptions(file, true);
        opts = testMe.getOptions();
        count = 0;
        while (opts.hasMoreElements()) {
            IOption io = opts.nextElement();

            switch (io.getType()) {
                case IOption.STRING:
                case IOption.CHOICE:
                case IOption.INTEGER:
                    assertEquals(io.getValue().toString(), "" + count);
                    break;
                case IOption.BOOLEAN:
                    if ((count % 2) == 0) {
                        assertTrue(io.booleanValue());
                    } else {
                        assertFalse(io.booleanValue());
                    }
                    break;
                case IOption.FLOAT:
                    assertEquals(Float.parseFloat("" + count), io.floatValue(), 0.0f);
                    break;
            }

            count++;
        }
    }
}
