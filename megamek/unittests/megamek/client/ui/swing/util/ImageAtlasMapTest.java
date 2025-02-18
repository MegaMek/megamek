/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.swing.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * Test for the ImageAtlasMap parent class and the children classes.
 *
 * @author rjhancock
 */
class ImageAtlasMapTest {
    private ImageAtlasMap imageAtlasMap;

    @BeforeEach
    public void setUp() {
        imageAtlasMap = new ImageAtlasMap();
    }

    @Test
    void testPutWithUnixFilePaths() {
        Path originalFilePath = Path.of("data/images/atlas/foo.png");
        Path atlasFilePath = Path.of("data/images/foo.png");
        imageAtlasMap.put(originalFilePath, atlasFilePath);

        String result = imageAtlasMap.get(originalFilePath);
        assertEquals(atlasFilePath.toString(), result);
    }

    @Test
    void testPutWithWindowsFilePaths() {
        Path originalFilePath = Path.of("data\\images\\atlas\\foo.png");
        Path atlasFilePath = Path.of("data\\images\\foo.png");
        imageAtlasMap.put(originalFilePath, atlasFilePath);

        String result = imageAtlasMap.get(originalFilePath);
        assertEquals("data/images/foo.png", result);
    }

    @Test
    void testContainsKeyTrue() {
        Path originalFilePath = Path.of("data/images/atlas/foo.png");
        Path atlasFilePath = Path.of("data/images/foo.png");
        imageAtlasMap.put(originalFilePath, atlasFilePath);

        boolean result = imageAtlasMap.containsKey(originalFilePath);
        assertEquals(true, result);
    }

    @Test
    void testContainsKeyFalse() {
        Path originalFilePath = Path.of("data/images/atlas/foo.png");
        Path atlasFilePath = Path.of("data/images/foo.png");
        imageAtlasMap.put(atlasFilePath, originalFilePath);

        boolean result = imageAtlasMap.containsKey(originalFilePath);
        assertEquals(false, result);
    }

    @Test
    void writingAndReadingAMap() {
        Path originalFilePath = Path.of("data/images/foo.png");
        Path atlasFilePath = Path.of("data/images/atlas/foo.png");
        imageAtlasMap.put(originalFilePath, atlasFilePath);

        boolean contains = imageAtlasMap.containsKey(originalFilePath);
        assertEquals(true, contains);

        File testFilePath = new File("testresources/atlas_test.yml");

        imageAtlasMap.writeToFile(testFilePath);

        ImageAtlasMap readImageAtlasMap = ImageAtlasMap.readFromFile(testFilePath);
        String result = readImageAtlasMap.get(originalFilePath);
        assertEquals(originalFilePath.toString(), result);
    }
}
