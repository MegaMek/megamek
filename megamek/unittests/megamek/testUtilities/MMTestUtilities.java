
/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.testUtilities;

import java.io.File;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MekFileParser;
import megamek.common.annotations.Nullable;

public class MMTestUtilities {
    private static final String TEST_RESOURCES_DIR = "testresources/";
    private static final String TEST_DATA_DIR = TEST_RESOURCES_DIR + "data/";
    private static final String TEST_UNIT_DATA_DIR = TEST_DATA_DIR + "mekfiles/";
    private static final String TEST_BLK = ".blk";
    private static final String TEST_MTF = ".mtf";

    /**
     * Loads an {@link Entity} object from a file after ensuring equipment types are initialized.
     *
     * <p>This method constructs the path to the unit data file using the provided unit name and file type, parses
     * the file with a {@link MekFileParser}, and returns the resulting {@code Entity}. If the entity cannot be loaded
     * or is {@code null}, an error message is printed to standard output and {@code null} is returned.</p>
     *
     * @param unitFileName the base name of the unit file (without file extension)
     * @param isBLK        {@code true} to load a BLK format file; {@code false} for MTF format
     *
     * @return the parsed {@link Entity}, or {@code null} if loading fails or the entity is not found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable Entity getEntityForUnitTesting(String unitFileName, boolean isBLK) {
        EquipmentType.initializeTypes();

        try {
            File file = new File(TEST_UNIT_DATA_DIR + unitFileName + (isBLK ? TEST_BLK : TEST_MTF));
            MekFileParser fileParser = new MekFileParser(file);
            if (fileParser.getEntity() == null) {
                System.out.println("Failed to load entity " + file.getAbsolutePath());
                return null;
            }

            return fileParser.getEntity();
        } catch (Exception ex) {
            System.out.println("Failed to load entity " + unitFileName + (isBLK ? TEST_BLK : TEST_MTF));
            return null;
        }
    }
}
