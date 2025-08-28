/*
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Edward Cullen
 */
class ConfigurationTests {

    @BeforeEach
    void setUp() {
        Configuration.setDataDir(new File("testresources/data"));
    }

    /**
     * Test method for {@link megamek.common.Configuration#configDir()}.
     */
    @Test
    final void testConfigDir() {
        assertEquals("mmconf", Configuration.configDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#setConfigDir(java.io.File)}.
     */
    @Test
    final void testSetConfigDir() {
        Configuration.setConfigDir(new File("config"));
        assertEquals("config", Configuration.configDir().toString());
        // Should rest to default.
        Configuration.setConfigDir(null);
        assertEquals("mmconf", Configuration.configDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#dataDir()}.
     */
    @Test
    final void testDataDir() {
        assertEquals("testresources/data", Configuration.dataDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#setDataDir(java.io.File)}.
     */
    @Test
    final void testSetDataDir() {
        Configuration.setDataDir(new File("mydata"));
        assertEquals("mydata", Configuration.dataDir().toString());
        // Should rest to default.
        Configuration.setDataDir(null);
        assertEquals("data", Configuration.dataDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#docsDir()}.
     */
    @Test
    final void testDocsDir() {
        assertEquals("docs", Configuration.docsDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#setDocsDir(java.io.File)}.
     */
    @Test
    final void testSetDocsDir() {
        Configuration.setDocsDir(new File("mydocs"));
        assertEquals("mydocs", Configuration.docsDir().toString());
        // Should rest to default.
        Configuration.setDocsDir(null);
        assertEquals("docs", Configuration.docsDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#armyTablesDir()}.
     */
    @Test
    final void testArmyTablesDir() {
        assertEquals(new File(Configuration.dataDir(), "rat").toString(), Configuration
              .armyTablesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#setArmyTablesDir(java.io.File)}.
     */
    @Test
    final void testSetArmyTablesDir() {
        Configuration.setArmyTablesDir(new File("my_armies"));
        assertEquals("my_armies", Configuration.armyTablesDir().toString());
        // Should reset to default.
        Configuration.setArmyTablesDir(null);
        assertEquals(new File(Configuration.dataDir(), "rat").toString(), Configuration
              .armyTablesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#boardsDir()}.
     */
    @Test
    final void testBoardsDir() {
        assertEquals(new File(Configuration.dataDir(), "boards").toString(), Configuration
              .boardsDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#setBoardsDir(java.io.File)}.
     */
    @Test
    final void testSetBoardsDir() {
        Configuration.setBoardsDir(new File("my_boards"));
        assertEquals("my_boards", Configuration.boardsDir().toString());
        // Should reset to default.
        Configuration.setBoardsDir(null);
        assertEquals(new File(Configuration.dataDir(), "boards").toString(), Configuration
              .boardsDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#unitsDir()}.
     */
    @Test
    final void testUnitsDir() {
        assertEquals(new File(Configuration.dataDir(), "mekfiles").toString(), Configuration
              .unitsDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#setUnitsDir(java.io.File)}.
     */
    @Test
    final void testSetUnitsDir() {
        Configuration.setUnitsDir(new File("my_units"));
        assertEquals("my_units", Configuration.unitsDir().toString());
        // Should reset to default.
        Configuration.setUnitsDir(null);
        assertEquals(new File(Configuration.dataDir(), "mekfiles").toString(), Configuration
              .unitsDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#scenariosDir()}.
     */
    @Test
    final void testScenariosDir() {
        assertEquals(new File(Configuration.dataDir(), "scenarios").toString(), Configuration
              .scenariosDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#setScenariosDir(java.io.File)}.
     */
    @Test
    final void testSetScenariosDir() {
        Configuration.setScenariosDir(new File("my_scenarios"));
        assertEquals("my_scenarios", Configuration.scenariosDir().toString());
        // Should reset to default.
        Configuration.setScenariosDir(null);
        assertEquals(new File(Configuration.dataDir(), "scenarios").toString(), Configuration
              .scenariosDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#soundsDir()}.
     */
    @Test
    final void testSoundsDir() {
        assertEquals(new File(Configuration.dataDir(), "sounds").toString(), Configuration
              .soundsDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#setSoundsDir(java.io.File)}.
     */
    @Test
    final void testSetSoundsDir() {
        Configuration.setSoundsDir(new File("my_sounds"));
        assertEquals("my_sounds", Configuration.soundsDir().toString());
        // Should reset to default.
        Configuration.setSoundsDir(null);
        assertEquals(new File(Configuration.dataDir(), "sounds").toString(), Configuration
              .soundsDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#imagesDir()}.
     */
    @Test
    final void testImagesDir() {
        assertEquals(new File(Configuration.dataDir(), "images").toString(), Configuration
              .imagesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#setImagesDir(java.io.File)}.
     */
    @Test
    final void testSetImagesDir() {
        Configuration.setImagesDir(new File("my_images"));
        assertEquals("my_images", Configuration.imagesDir().toString());
        // Should reset to default.
        Configuration.setImagesDir(null);
        assertEquals(new File(Configuration.dataDir(), "images").toString(), Configuration
              .imagesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#camoDir()}.
     */
    @Test
    final void testCamoDir() {
        assertEquals(new File(Configuration.imagesDir(), "camo").toString(),
              Configuration.camoDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#hexesDir()}.
     */
    @Test
    final void testHexesDir() {
        assertEquals(new File(Configuration.imagesDir(), "hexes").toString(),
              Configuration.hexesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#fluffImagesDir()}.
     */
    @Test
    final void testFluffImagesDir() {
        assertEquals(new File(Configuration.imagesDir(), "fluff").toString(),
              Configuration.fluffImagesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#miscImagesDir()}.
     */
    @Test
    final void testMiscImagesDir() {
        assertEquals(new File(Configuration.imagesDir(), "misc").toString(),
              Configuration.miscImagesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#portraitImagesDir()}.
     */
    @Test
    final void testPortraitImagesDir() {
        assertEquals(
              new File(Configuration.imagesDir(), "portraits").toString(),
              Configuration.portraitImagesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#unitImagesDir()}.
     */
    @Test
    final void testUnitImagesDir() {
        assertEquals(new File(Configuration.imagesDir(), "units").toString(),
              Configuration.unitImagesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#widgetsDir()}.
     */
    @Test
    final void testWidgetsDir() {
        assertEquals(new File(Configuration.imagesDir(), "widgets").toString(),
              Configuration.widgetsDir().toString());
    }
}
