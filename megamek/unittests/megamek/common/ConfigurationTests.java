/*
 * Tests for the class {@link Configuration}
 * 
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License,
 * version 2, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, it is available online at:
 * http://www.gnu.org/licenses/gpl-2.0.html
 */
package megamek.common;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Edward Cullen
 */
public class ConfigurationTests {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link megamek.common.Configuration#configDir()}.
     */
    @Test
    public final void testConfigDir() {
        assertEquals("mmconf", Configuration.configDir().toString());
    }

    /**
     * Test method for
     * {@link megamek.common.Configuration#setConfigDir(java.io.File)}.
     */
    @Test
    public final void testSetConfigDir() {
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
    public final void testDataDir() {
        assertEquals("data", Configuration.dataDir().toString());
    }

    /**
     * Test method for
     * {@link megamek.common.Configuration#setDataDir(java.io.File)}.
     */
    @Test
    public final void testSetDataDir() {
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
    public final void testDocsDir() {
        assertEquals("docs", Configuration.docsDir().toString());
    }

    /**
     * Test method for
     * {@link megamek.common.Configuration#setDocsDir(java.io.File)}.
     */
    @Test
    public final void testSetDocsDir() {
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
    public final void testArmyTablesDir() {
        assertEquals(new File("data", "rat").toString(), Configuration
                .armyTablesDir().toString());
    }

    /**
     * Test method for
     * {@link megamek.common.Configuration#setArmyTablesDir(java.io.File)}.
     */
    @Test
    public final void testSetArmyTablesDir() {
        Configuration.setArmyTablesDir(new File("my_armies"));
        assertEquals("my_armies", Configuration.armyTablesDir().toString());
        // Should reset to default.
        Configuration.setArmyTablesDir(null);
        assertEquals(new File("data", "rat").toString(), Configuration
                .armyTablesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#boardsDir()}.
     */
    @Test
    public final void testBoardsDir() {
        assertEquals(new File("data", "boards").toString(), Configuration
                .boardsDir().toString());
    }

    /**
     * Test method for
     * {@link megamek.common.Configuration#setBoardsDir(java.io.File)}.
     */
    @Test
    public final void testSetBoardsDir() {
        Configuration.setBoardsDir(new File("my_boards"));
        assertEquals("my_boards", Configuration.boardsDir().toString());
        // Should reset to default.
        Configuration.setBoardsDir(null);
        assertEquals(new File("data", "boards").toString(), Configuration
                .boardsDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#unitsDir()}.
     */
    @Test
    public final void testUnitsDir() {
        assertEquals(new File("data", "mechfiles").toString(), Configuration
                .unitsDir().toString());
    }

    /**
     * Test method for
     * {@link megamek.common.Configuration#setUnitsDir(java.io.File)}.
     */
    @Test
    public final void testSetUnitsDir() {
        Configuration.setUnitsDir(new File("my_units"));
        assertEquals("my_units", Configuration.unitsDir().toString());
        // Should reset to default.
        Configuration.setUnitsDir(null);
        assertEquals(new File("data", "mechfiles").toString(), Configuration
                .unitsDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#scenariosDir()}.
     */
    @Test
    public final void testScenariosDir() {
        assertEquals(new File("data", "scenarios").toString(), Configuration
                .scenariosDir().toString());
    }

    /**
     * Test method for
     * {@link megamek.common.Configuration#setScenariosDir(java.io.File)}.
     */
    @Test
    public final void testSetScenariosDir() {
        Configuration.setScenariosDir(new File("my_scenarios"));
        assertEquals("my_scenarios", Configuration.scenariosDir().toString());
        // Should reset to default.
        Configuration.setScenariosDir(null);
        assertEquals(new File("data", "scenarios").toString(), Configuration
                .scenariosDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#soundsDir()}.
     */
    @Test
    public final void testSoundsDir() {
        assertEquals(new File("data", "sounds").toString(), Configuration
                .soundsDir().toString());
    }

    /**
     * Test method for
     * {@link megamek.common.Configuration#setSoundsDir(java.io.File)}.
     */
    @Test
    public final void testSetSoundsDir() {
        Configuration.setSoundsDir(new File("my_sounds"));
        assertEquals("my_sounds", Configuration.soundsDir().toString());
        // Should reset to default.
        Configuration.setSoundsDir(null);
        assertEquals(new File("data", "sounds").toString(), Configuration
                .soundsDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#imagesDir()}.
     */
    @Test
    public final void testImagesDir() {
        assertEquals(new File("data", "images").toString(), Configuration
                .imagesDir().toString());
    }

    /**
     * Test method for
     * {@link megamek.common.Configuration#setImagesDir(java.io.File)}.
     */
    @Test
    public final void testSetImagesDir() {
        Configuration.setImagesDir(new File("my_images"));
        assertEquals("my_images", Configuration.imagesDir().toString());
        // Should reset to default.
        Configuration.setImagesDir(null);
        assertEquals(new File("data", "images").toString(), Configuration
                .imagesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#camoDir()}.
     */
    @Test
    public final void testCamoDir() {
        assertEquals(new File(Configuration.imagesDir(), "camo").toString(),
                Configuration.camoDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#hexesDir()}.
     */
    @Test
    public final void testHexesDir() {
        assertEquals(new File(Configuration.imagesDir(), "hexes").toString(),
                Configuration.hexesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#fluffImagesDir()}.
     */
    @Test
    public final void testFluffImagesDir() {
        assertEquals(new File(Configuration.imagesDir(), "fluff").toString(),
                Configuration.fluffImagesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#miscImagesDir()}.
     */
    @Test
    public final void testMiscImagesDir() {
        assertEquals(new File(Configuration.imagesDir(), "misc").toString(),
                Configuration.miscImagesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#portraitImagesDir()}.
     */
    @Test
    public final void testPortraitImagesDir() {
        assertEquals(
                new File(Configuration.imagesDir(), "portraits").toString(),
                Configuration.portraitImagesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#unitImagesDir()}.
     */
    @Test
    public final void testUnitImagesDir() {
        assertEquals(new File(Configuration.imagesDir(), "units").toString(),
                Configuration.unitImagesDir().toString());
    }

    /**
     * Test method for {@link megamek.common.Configuration#widgetsDir()}.
     */
    @Test
    public final void testWidgetsDir() {
        assertEquals(new File(Configuration.imagesDir(), "widgets").toString(),
                Configuration.widgetsDir().toString());
    }
}
