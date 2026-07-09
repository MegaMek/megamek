/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.eras.Eras;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class EntityTechTest {

    @BeforeAll
    public static void setupClass() {
        EquipmentType.initializeTypes();
        MekSummaryCache.getInstance(true);
        Eras.getInstance();
    }

    @Test
    public void testDateRangesChippewa() {
        AeroSpaceFighter entity = (AeroSpaceFighter) getEntityForUnitTesting("Chippewa CHP-W7", true);
        assertNotNull(entity, "Chippewa CHP-W7 not found");
        printEntity(entity);

        Assertions.assertEquals(2735, entity.getIntroductionDate());
        Assertions.assertEquals("-", entity.getPrototypeRangeDate());
        Assertions.assertTrue(entity.getProductionDateRange().contains("2735-2814") && entity.getProductionDateRange()
              .contains("3041-3054"));
        Assertions.assertTrue(entity.getCommonDateRange().contains("3055+"));
        Assertions.assertTrue(entity.getExtinctionRange().contains("2815-3040"));
    }

    @Test
    public void testDateRangesMuseEarth() {
        // Devastator DVS-X10 MUSE EARTH.mtf
        // Create a Mek for normal movement
        Mek entity = (Mek) getEntityForUnitTesting("Devastator DVS-X10 MUSE EARTH", false);
        assertNotNull(entity, "Devastator DVS-X10 MUSE EARTH not found");
        printEntity(entity);

        Assertions.assertEquals(3075, entity.getIntroductionDate());
        Assertions.assertTrue(entity.getPrototypeRangeDate().contains("3075-3119"));
        Assertions.assertTrue(entity.getProductionDateRange().contains("3120+"));
        Assertions.assertEquals("-",
              entity.getCommonDateRange());
        Assertions.assertEquals("-",
              entity.getExtinctionRange());
    }

    @Test
    public void mixedTechWithClanProgressionIsAvailableToClansBefore3050() {
        TechAdvancement advancement = new TechAdvancement(TechBase.ALL)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3051)
              .setClanAdvancement(2865, 2868, 2870);

        Assertions.assertEquals(AvailabilityValue.D, advancement.calcYearAvailability(3032, true));
    }

    @Test
    public void mixedTechWithoutClanProgressionIsUnavailableToClansBefore3050() {
        TechAdvancement advancement = new TechAdvancement(TechBase.ALL)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3051);

        Assertions.assertEquals(AvailabilityValue.X, advancement.calcYearAvailability(3032, true));
    }

    @Test
    public void mixedTechWithClanEraProgressionIsUnavailableToClansBefore3050() {
        TechAdvancement advancement = new TechAdvancement(TechBase.ALL)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3051)
              .setClanAdvancement(3050, 3050, 3051);

        Assertions.assertEquals(AvailabilityValue.X, advancement.calcYearAvailability(3032, true));
    }

    @Test
    public void innerSphereTechWithoutClanProgressionIsUnavailableToClansBefore3050() {
        TechAdvancement advancement = new TechAdvancement(TechBase.IS)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(2865, 2868, 2870);

        Assertions.assertEquals(AvailabilityValue.X, advancement.calcYearAvailability(3032, true));
    }

    @Test
    public void innerSphereTechWithSharedIntroductionDateIsUnavailableToClansBefore3050() {
        TechAdvancement advancement = new TechAdvancement(TechBase.IS) {
            @Override
            public int getIntroductionDate(boolean clan) {
                return getIntroductionDate();
            }
        }.setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(2865, 2868, 2870);

        Assertions.assertEquals(AvailabilityValue.X, advancement.calcYearAvailability(3032, true));
    }

    @Test
    public void compositeInnerSphereTechWithSharedIntroductionDateIsUnavailableToClansBefore3050() {
        TechAdvancement advancement = new TechAdvancement(TechBase.IS)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(2865, 2868, 2870);
        CompositeTechLevel techLevel = new CompositeTechLevel(advancement, false, true, 2865, Faction.NONE);

        Assertions.assertEquals(AvailabilityValue.X, techLevel.calcYearAvailability(3032, true));
    }

    @Test
    public void innerSphereTechIsAvailableToInnerSphereBefore3050() {
        TechAdvancement advancement = new TechAdvancement(TechBase.IS)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(2865, 2868, 2870);

        Assertions.assertEquals(AvailabilityValue.D, advancement.calcYearAvailability(3032, false));
    }

    @Test
    public void innerSphereTechUsesBaseAvailabilityForClansAfter3050() {
        TechAdvancement advancement = new TechAdvancement(TechBase.IS)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(2865, 2868, 2870);

        Assertions.assertEquals(AvailabilityValue.C, advancement.calcYearAvailability(3050, true));
    }

    @Test
    public void clanTechIsUnavailableToInnerSphereBefore3050() {
        TechAdvancement advancement = new TechAdvancement(TechBase.CLAN)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(2865, 2868, 2870);

        Assertions.assertEquals(AvailabilityValue.X, advancement.calcYearAvailability(3032, false));
    }

    @Test
    public void clanTechIsHarderForInnerSphereAfter3050() {
        TechAdvancement advancement = new TechAdvancement(TechBase.CLAN)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(2865, 2868, 2870);

        Assertions.assertEquals(AvailabilityValue.D, advancement.calcYearAvailability(3050, false));
    }

    @Test
    public void elementalBattleArmorComponentsAreAvailableToClansBefore3050() {
        AmmoType baSrm2Ammo = (AmmoType) EquipmentType.get("BA-SRM2 Ammo");
        assertNotNull(baSrm2Ammo, "BA SRM 2 ammo not found");

        Assertions.assertEquals(AvailabilityValue.D,
              BattleArmor.getConstructionTechAdvancement(EntityWeightClass.WEIGHT_MEDIUM)
                    .calcYearAvailability(3032, true));
        Assertions.assertEquals(AvailabilityValue.D, baSrm2Ammo.calcYearAvailability(3032, true));
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
