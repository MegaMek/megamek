/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/**
 *
 * @author nderwin
 */
public class MapSettingsTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    
    @Test
    public void testSaveAndLoad() throws UnsupportedEncodingException, IOException {
        File f = tmpFolder.newFile("test-map-settings.xml");
        OutputStream os = new FileOutputStream(f);
        
        MapSettings testMe = MapSettings.getInstance();
        testMe.setBoardSize(32, 34);
        testMe.save(os);
        
        assertTrue(f.exists());
        assertTrue(f.length() > 0);
        
        InputStream is = new FileInputStream(f);
        testMe = MapSettings.getInstance(is);

        assertEquals(32, testMe.getBoardWidth());
        assertEquals(34, testMe.getBoardHeight());
        assertEquals("", testMe.getTheme());
        assertEquals(0, testMe.getInvertNegativeTerrain());
        assertEquals(40, testMe.getHilliness());
        assertEquals(5, testMe.getRange());
        assertEquals(5, testMe.getProbInvert());
        assertEquals(0, testMe.getAlgorithmToUse());
        assertEquals(0, testMe.getCliffs());
        assertEquals(3, testMe.getMinForestSpots());
        assertEquals(8, testMe.getMaxForestSpots());
        assertEquals(4, testMe.getMinForestSize());
        assertEquals(12, testMe.getMaxForestSize());
        assertEquals(30, testMe.getProbHeavy());
        assertEquals(2, testMe.getMinRoughSpots());
        assertEquals(10, testMe.getMaxRoughSpots());
        assertEquals(1, testMe.getMinRoughSize());
        assertEquals(2, testMe.getMaxRoughSize());
        assertEquals(2, testMe.getMinSandSpots());
        assertEquals(10, testMe.getMaxSandSpots());
        assertEquals(1, testMe.getMinSandSize());
        assertEquals(2, testMe.getMaxSandSize());
        assertEquals(2, testMe.getMinPlantedFieldSpots());
        assertEquals(10, testMe.getMaxPlantedFieldSpots());
        assertEquals(1, testMe.getMinPlantedFieldSize());
        assertEquals(2, testMe.getMaxPlantedFieldSize());
        assertEquals(2, testMe.getMinSwampSpots());
        assertEquals(10, testMe.getMaxSwampSpots());
        assertEquals(1, testMe.getMinSwampSize());
        assertEquals(2, testMe.getMaxSwampSize());
        assertEquals(0, testMe.getProbRoad());
        assertEquals(1, testMe.getMinWaterSpots());
        assertEquals(3, testMe.getMaxWaterSpots());
        assertEquals(5, testMe.getMinWaterSize());
        assertEquals(10, testMe.getMaxWaterSize());
        assertEquals(33, testMe.getProbDeep());
        assertEquals(0, testMe.getProbRiver());
        assertEquals(1, testMe.getMinCraters());
        assertEquals(2, testMe.getMaxCraters());
        assertEquals(2, testMe.getMinRadius());
        assertEquals(7, testMe.getMaxRadius());
        assertEquals(0, testMe.getProbCrater());
        assertEquals(0, testMe.getMinPavementSpots());
        assertEquals(0, testMe.getMaxPavementSpots());
        assertEquals(1, testMe.getMinPavementSize());
        assertEquals(6, testMe.getMaxPavementSize());
        assertEquals(0, testMe.getMinRubbleSpots());
        assertEquals(0, testMe.getMaxRubbleSpots());
        assertEquals(1, testMe.getMinRubbleSize());
        assertEquals(6, testMe.getMaxRubbleSize());
        assertEquals(0, testMe.getMinFortifiedSpots());
        assertEquals(0, testMe.getMaxFortifiedSpots());
        assertEquals(1, testMe.getMinFortifiedSize());
        assertEquals(2, testMe.getMaxFortifiedSize());
        assertEquals(0, testMe.getMinIceSpots());
        assertEquals(0, testMe.getMaxIceSpots());
        assertEquals(1, testMe.getMinIceSize());
        assertEquals(6, testMe.getMaxIceSize());
        assertEquals(0, testMe.getFxMod());
        assertEquals(0, testMe.getProbFreeze());
        assertEquals(0, testMe.getProbFlood());
        assertEquals(0, testMe.getProbForestFire());
        assertEquals(0, testMe.getProbDrought());
        assertEquals("NONE", testMe.getCityType());
        assertEquals(16, testMe.getCityBlocks());
        assertEquals(75, testMe.getCityDensity());
        assertEquals(10, testMe.getCityMinCF());
        assertEquals(100, testMe.getCityMaxCF());
        assertEquals(1, testMe.getCityMinFloors());
        assertEquals(6, testMe.getCityMaxFloors());
        assertEquals(60, testMe.getTownSize());
        assertEquals(0, testMe.getMountainPeaks());
        assertEquals(7, testMe.getMountainWidthMin());
        assertEquals(20, testMe.getMountainWidthMax());
        assertEquals(5, testMe.getMountainHeightMin());
        assertEquals(8, testMe.getMountainHeightMax());
        assertEquals(0, testMe.getMountainStyle());
    }

}
