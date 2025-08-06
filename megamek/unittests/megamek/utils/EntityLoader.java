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
package megamek.utils;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MekFileParser;

public class EntityLoader {

    private static final String RESOURCE_PATH = "testresources/megamek/common/units/";
    public static final File RESOURCE_FOLDER = new File(RESOURCE_PATH);

    private EntityLoader() {
    }


    /**
     * Load the entity from the provided filename, the file must be present in the folder at
     * {@link EntityLoader#RESOURCE_PATH}.
     *
     * @param filename name of the file with the file type extension
     *
     * @return instantiated entity in the specified class of the unit
     */
    public static Entity loadFromFile(String filename) {
        EquipmentType.initializeTypes();
        try {
            File file = new File(RESOURCE_PATH + filename);
            MekFileParser mfParser = new MekFileParser(file);
            return mfParser.getEntity();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        throw new RuntimeException();
    }

    /**
     * Load the entity from the provided filename, the file must be present in the folder at
     * {@link EntityLoader#RESOURCE_PATH}.
     *
     * @param filename name of the file with the file type extension
     * @param <UNIT>   the specific class of the unit
     *
     * @return instantiated entity in the specified class of the unit
     */
    @SuppressWarnings("unchecked")
    public static <UNIT extends Entity> UNIT loadFromFile(String filename, Class<UNIT> classType) {
        EquipmentType.initializeTypes();
        try {
            File file = new File(RESOURCE_PATH + filename);
            MekFileParser mfParser = new MekFileParser(file);
            Entity entity = mfParser.getEntity();
            if (classType.isInstance(entity)) {
                return (UNIT) entity;
            }
            fail("Entity for " + filename + " is of type " + entity.getClass().getSimpleName() +
                  " instead of " + classType.getSimpleName());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        throw new RuntimeException();
    }
}
