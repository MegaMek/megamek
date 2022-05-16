/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nderwin
 */
public class MapSettingsTest {

    @TempDir
    private Path tempDirectory;
    
    @Test
    public void testSaveAndLoad() throws IOException {
        assertTrue(Files.isDirectory(tempDirectory));
        final Path createdFilePath = Files.createFile(tempDirectory.resolve("test-map-settings.xml"));
        final File file = createdFilePath.toFile();

        MapSettings testMe = MapSettings.getInstance();
        try (OutputStream os = new FileOutputStream(file)) {
            testMe.setBoardSize(32, 34);
            testMe.save(os);
        }

        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        try (InputStream is = new FileInputStream(file)) {
            testMe = MapSettings.getInstance(is);
        }

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
