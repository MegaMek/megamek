/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.test;

import java.io.IOException;

import megamek.client.ui.tileset.GenerateGenericIconList;
import megamek.client.ui.tileset.MekSetTest;
import megamek.logging.MMLogger;

/**
 * Executes both {@link MekSetTest} and {@link GenerateGenericIconList} for a combined icon problems result.
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
