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
package megamek.common;

import megamek.utils.EntityLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;


class EntityTechTest {

    @BeforeAll
    public static void setupClass() {
        EquipmentType.initializeTypes();
        MekSummaryCache.getInstance();
    }

    @Test
    public void testDateRangesChippewa() {
        AeroSpaceFighter entity = EntityLoader.loadFromFile("Chippewa CHP-W7.blk", AeroSpaceFighter.class);
        printEntity(entity);
        Assertions.assertEquals("2735 (Star League)", entity.getIntroductionDateAndEra());
        Assertions.assertEquals("-",
              entity.getPrototypeRangeDate());
        Assertions.assertEquals("2735-2814 (Star League to Early Succession Wars), 3041-3054 (Late Succession Wars - Renaissance to Clan Invasion)",
              entity.getProductionDateRange());
        Assertions.assertEquals("3055+ (Clan Invasion and onwards)",
              entity.getCommonDateRange());
        Assertions.assertEquals("2815-3040",
              entity.getExtinctionRange());
    }

    @Test
    public void testDateRangesMuseEarth() {
        // Devastator DVS-X10 MUSE EARTH.mtf
        // Create a Mek for normal movement
        Mek entity = EntityLoader.loadFromFile("Devastator DVS-X10 MUSE EARTH.mtf", Mek.class);
        printEntity(entity);

        Assertions.assertEquals("3075 (Jihad)", entity.getIntroductionDateAndEra());
        Assertions.assertEquals("3075-3119 (Jihad to Late Republic)",
              entity.getPrototypeRangeDate());
        Assertions.assertEquals("3120+ (Late Republic and onwards)",
              entity.getProductionDateRange());
        Assertions.assertEquals("-",
              entity.getCommonDateRange());
        Assertions.assertEquals("-",
              entity.getExtinctionRange());
    }

    private static void printEntity(Entity entity) {
        System.out.println(String.join("\n", List.of("INTRO:" + entity.getIntroductionDateAndEra(),
              "PROTOTYPE: " + entity.getPrototypeRangeDate(),
              "PRODUCTION: " + entity.getProductionDateRange(),
              "COMMON: " + entity.getCommonDateRange(),
              "EXTINCT: " + entity.getExtinctionRange(),
              "EARLIEST: " + entity.getEarliestTechDateAndEra())));
    }
}
