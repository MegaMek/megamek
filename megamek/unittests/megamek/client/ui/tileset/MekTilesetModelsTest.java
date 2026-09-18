/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.tileset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import megamek.common.units.BipedMek;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MekTilesetModelsTest {
    @TempDir
    Path directory;

    @Test
    void spriteOverridesInheritChassisModelsAndMissingChassisUseTheDefault() throws Exception {
        Files.writeString(directory.resolve("mekset.txt"), """
              exact "default_medium" "medium.png" "units/fallback/biped.json"
              chassis "Test" "test.png" "units/test/model.json"
              exact "Test A" "variant.png"
              exact "Test B" "variant-b.png" "units/custom/model.json"
              """);
        MekTileset tileset = new MekTileset(directory.toFile());
        tileset.loadFromFile("mekset.txt");
        BipedMek mek = new BipedMek();
        mek.setWeight(50);
        mek.setChassis("Test");
        mek.setModel("A");
        assertEquals("variant.png", tileset.entryFor(mek, -1).getImageFile());
        assertEquals("units/test/model.json", tileset.modelFor(mek, -1));
        mek.setModel("B");
        assertEquals("units/custom/model.json", tileset.modelFor(mek, -1));
        mek.setChassis("Unknown");
        assertEquals("units/fallback/biped.json", tileset.modelFor(mek, -1));
        assertNull(new MekTileset(directory.toFile()).modelFor(mek, -1));
    }

    @Test
    void includesAndLegacyThreeFieldEntriesRemainValid() throws Exception {
        Files.writeString(directory.resolve("included.txt"), "chassis \"Test\" \"test.png\" \"units/test/model.json\"\n");
        Files.writeString(directory.resolve("mekset.txt"), "include \"included.txt\"\nexact \"Test A\" \"a.png\"\n");
        MekTileset tileset = new MekTileset(directory.toFile());
        tileset.loadFromFile("mekset.txt");
        BipedMek mek = new BipedMek();
        mek.setChassis("Test");
        mek.setModel("A");
        assertEquals("a.png", tileset.entryFor(mek, -1).getImageFile());
        assertEquals("units/test/model.json", tileset.modelFor(mek, -1));
        assertTrue(MekSetTest.isValidContentLine(List.of("exact", "Legacy", "sprite.png")));
        assertTrue(MekSetTest.isValidContentLine(List.of("exact", "Modern", "sprite.png", "units/model.json")));
    }
}
