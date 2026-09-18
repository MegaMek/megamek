/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.tileset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HexTilesetMatchCacheTest {
    @TempDir
    Path directory;

    private HexTileset tileset() throws Exception {
        for (String name : List.of("a", "b", "c", "hill", "desert", "north", "east")) {
            ImageIO.write(new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB), "png",
                  directory.resolve(name + ".png").toFile());
        }
        Files.writeString(directory.resolve("test.tileset"), """
              super * "road:1:1" "" "north.png"
              super * "road:1:2" "" "east.png"
              base 2 "" "" "hill.png"
              base * "" "desert" "desert.png"
              base * "" "" "a.png;b.png;c.png"
              """);
        HexTileset result = new HexTileset(new Game(), directory.toFile());
        result.loadFromFile("test.tileset");
        return result;
    }

    @Test
    void reusingRulesPreservesCoordinateVariantsAndAllMatchingInputs() throws Exception {
        HexTileset cached = tileset();
        Set<String> variants = new HashSet<>();
        for (int x = 0; x < 12; x++) {
            Hex hex = new Hex(0, "", "", new Coords(x, 2));
            HexTileset fresh = tileset();
            String expected = fresh.imageSource(fresh.getBase(hex));
            String actual = cached.imageSource(cached.getBase(hex));
            assertEquals(expected, actual, "A reused rule must retain this coordinate's exact image variant");
            variants.add(actual);
        }
        assertTrue(variants.size() > 1);
        for (int exit : new int[] { 1, 2, 1 }) {
            Hex hex = new Hex(0);
            hex.addTerrain(new Terrain(Terrains.ROAD, 1, true, exit));
            assertEquals(List.of(exit == 1 ? "north.png" : "east.png"),
                  cached.getSupers(hex).stream().map(cached::imageSource).toList());
        }
        assertEquals("hill.png", cached.imageSource(cached.getBase(new Hex(2))));
        assertEquals("desert.png", cached.imageSource(cached.getBase(new Hex(0, "", "desert"))));
    }
}
