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

package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.ConvInfantry;
import megamek.common.util.BuildingBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that a unit file naming a withdrawn Inferno SRM launcher (TechManual pp. 350-352 errata) still loads. The
 * old name loads as the plain launcher, and the platoon starts the battle declared as Inferno, which is what it was
 * built to carry. Beast Infantry (Elephant) (Needler/SRM) is the stock unit this covers.
 */
class BLKInfantryLegacyInfernoSrmTest {

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    private static ConvInfantry load(String primaryWeapon, @Nullable String secondaryWeapon) throws Exception {
        return (ConvInfantry) new BLKInfantryFile(block(primaryWeapon, secondaryWeapon)).getEntity();
    }

    private static BuildingBlock block(String primaryWeapon, @Nullable String secondaryWeapon) {
        BuildingBlock block = new BuildingBlock();
        block.writeBlockData("UnitType", "Infantry");
        block.writeBlockData("Name", "Test SRM Platoon");
        block.writeBlockData("Model", "");
        block.writeBlockData("year", 3075);
        block.writeBlockData("type", "IS Level 2");
        block.writeBlockData("motion_type", "Leg");
        block.writeBlockData("squad_size", 2);
        block.writeBlockData("squadn", 5);
        block.writeBlockData("Primary", primaryWeapon);
        if (secondaryWeapon != null) {
            block.writeBlockData("secondn", 1);
            block.writeBlockData("Secondary", secondaryWeapon);
        }
        return block;
    }

    @Test
    @DisplayName("the Elephant's withdrawn Inferno launcher loads as the plain launcher, declared Inferno")
    void legacySecondaryLauncherLoadsDeclaredInferno() throws Exception {
        ConvInfantry platoon = load("Needler Rifle", "InfantryStandardSRMInferno");

        assertEquals("InfantryStandardSRM", platoon.getSecondaryWeapon().getInternalName(),
              "The withdrawn launcher should load as the plain two-shot SRM launcher");
        assertTrue(platoon.firesInfernoSrms(), "A unit built with the Inferno launcher should start on Inferno");
    }

    @Test
    @DisplayName("a withdrawn Inferno launcher as the primary weapon also loads declared Inferno")
    void legacyPrimaryLauncherLoadsDeclaredInferno() throws Exception {
        ConvInfantry platoon = load("SRM Launcher (Heavy) w/ Inferno", null);

        assertEquals("InfantryHeavySRM", platoon.getPrimaryWeapon().getInternalName(),
              "The withdrawn launcher should load as the plain heavy SRM launcher");
        assertTrue(platoon.firesInfernoSrms(), "A unit built with the Inferno launcher should start on Inferno");
    }

    @Test
    @DisplayName("a plain SRM launcher loads declared Standard")
    void plainLauncherLoadsDeclaredStandard() throws Exception {
        ConvInfantry platoon = load("InfantryAssaultRifle", "InfantryHeavySRM");

        assertTrue(platoon.hasSrmLauncher(), "The heavy SRM launcher is an SRM launcher");
        assertFalse(platoon.firesInfernoSrms(), "An ordinary SRM platoon should start on Standard");
    }

    @Test
    @DisplayName("an srmMunition block declares Inferno for a plain launcher")
    void srmMunitionBlockDeclaresInferno() throws Exception {
        BuildingBlock block = block("Needler Rifle", "InfantryStandardSRM");
        block.writeBlockData(BLKInfantryFile.SRM_MUNITION, BLKInfantryFile.SRM_MUNITION_INFERNO);

        ConvInfantry platoon = (ConvInfantry) new BLKInfantryFile(block).getEntity();

        assertTrue(platoon.firesInfernoSrms(), "The unit file declares Inferno munitions");
    }

    @Test
    @DisplayName("an srmMunition block of Standard overrides a withdrawn launcher's Inferno default")
    void srmMunitionBlockOverridesWithdrawnDefault() throws Exception {
        BuildingBlock block = block("Needler Rifle", "InfantryStandardSRMInferno");
        block.writeBlockData(BLKInfantryFile.SRM_MUNITION, "Standard");

        ConvInfantry platoon = (ConvInfantry) new BLKInfantryFile(block).getEntity();

        assertFalse(platoon.firesInfernoSrms(), "The unit file's own declaration wins over the old weapon name");
    }

    @Test
    @DisplayName("re-saving a unit built with the withdrawn launcher keeps it on Inferno")
    void resavedWithdrawnLauncherUnitStaysInferno() throws Exception {
        ConvInfantry original = load("Needler Rifle", "InfantryStandardSRMInferno");

        // As MegaMekLab does when the unit is opened and saved again
        BuildingBlock written = BLKFile.getBlock(original);
        assertEquals("InfantryStandardSRM", written.getDataAsString("Secondary")[0],
              "The re-saved file should name the plain launcher");
        assertEquals(BLKInfantryFile.SRM_MUNITION_INFERNO, written.getDataAsString(BLKInfantryFile.SRM_MUNITION)[0],
              "The re-saved file should carry the Inferno declaration");

        ConvInfantry reloaded = (ConvInfantry) new BLKInfantryFile(written).getEntity();
        assertTrue(reloaded.firesInfernoSrms(), "The re-saved unit should still start on Inferno");
    }

    @Test
    @DisplayName("a Standard SRM platoon writes no srmMunition block")
    void standardPlatoonWritesNoSrmMunition() throws Exception {
        BuildingBlock written = BLKFile.getBlock(load("InfantryAssaultRifle", "InfantryHeavySRM"));

        assertFalse(written.exists(BLKInfantryFile.SRM_MUNITION), "Standard is the default and is not written");
    }
}
