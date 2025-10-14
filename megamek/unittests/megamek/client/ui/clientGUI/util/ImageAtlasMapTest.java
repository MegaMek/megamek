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

package megamek.client.ui.clientGUI.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import megamek.client.ui.util.ImageAtlasMap;
import org.junit.jupiter.api.Assertions;
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
    void setUp() {
        imageAtlasMap = new ImageAtlasMap();
    }

    @Test
    void testPutWithUnixFilePaths() {
        Path originalFilePath = Path.of("data/images/atlas/foo.png");
        Path atlasFilePath = Path.of("data/images/foo.png");
        imageAtlasMap.put(originalFilePath, atlasFilePath);

        String result = imageAtlasMap.get(originalFilePath);
        assertEquals("data/images/foo.png", result);
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
        assertTrue(result);
    }

    @Test
    void testContainsKeyFalse() {
        Path originalFilePath = Path.of("data/images/atlas/foo.png");
        Path atlasFilePath = Path.of("data/images/foo.png");
        imageAtlasMap.put(atlasFilePath, originalFilePath);

        boolean result = imageAtlasMap.containsKey(originalFilePath);
        assertFalse(result);
    }

    @Test
    void writingAndReadingAMap() {
        Path originalFilePath = Path.of("data/images/foo.png");
        Path atlasFilePath = Path.of("data/images/atlas/foo.png");
        imageAtlasMap.put(originalFilePath, atlasFilePath);

        boolean contains = imageAtlasMap.containsKey(originalFilePath);
        assertTrue(contains);

        File testFilePath = new File("testresources/tmp/atlas_test.yml");

        imageAtlasMap.writeToFile(testFilePath);

        ImageAtlasMap readImageAtlasMap = ImageAtlasMap.readFromFile(testFilePath);
        Assertions.assertNotNull(readImageAtlasMap);
        String result = readImageAtlasMap.get(originalFilePath);
        assertEquals("data/images/atlas/foo.png", result);
    }
}
