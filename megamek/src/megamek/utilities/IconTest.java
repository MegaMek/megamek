/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.utilities;

import java.io.IOException;

import megamek.client.ui.swing.tileset.GenerateGenericIconList;
import megamek.client.ui.swing.tileset.MekSetTest;
import megamek.logging.MMLogger;

/**
 * Executes both {@link MekSetTest} and {@link GenerateGenericIconList} for a
 * combined icon problems result.
 */
public final class IconTest {
    private static final MMLogger logger = MMLogger.create(IconTest.class);

    public static void main(final String[] args) throws IOException {
        logger.info("Executing MekSetTest");

        try {
            MekSetTest.main(new String[0]);
        } catch (final Exception e) {
            logger.error(e, "Error with MekSetTest");
        }

        logger.info("\nExecuting GenerateGenericIconList");

        try {
            GenerateGenericIconList.main(new String[0]);
        } catch (final Exception e) {
            logger.error(e, "Error with GenerateGenericIconList");
        }
    }

    private IconTest() {
    }
}
