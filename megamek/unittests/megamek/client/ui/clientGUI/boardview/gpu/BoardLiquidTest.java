/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Test;

class BoardLiquidTest {
    @Test
    void terrainControlsTheLiquidAndHazardLevelsNeverBecomeWaterDepths() {
        Hex hex = new Hex(5);
        hex.setTheme("mars");
        hex.addTerrain(new Terrain(Terrains.WATER, 8));
        assertEquals("saxarba/theme_mars/water_anim_mars_4.gif", BoardLiquid.capture(hex).textures(8, 5).base());
        for (int level = 0; level <= 3; level++) {
            hex.addTerrain(new Terrain(Terrains.HAZARDOUS_LIQUID, level));
            BoardLiquid liquid = BoardLiquid.capture(hex);
            assertEquals(BoardLiquid.Kind.HAZARDOUS, liquid.kind());
            assertEquals("saxarba/anim_water_4.gif", liquid.textures(8, 5).base());
        }
        hex.removeAllTerrains();
        hex.addTerrain(new Terrain(Terrains.MAGMA, 1));
        assertEquals(BoardLiquid.NONE, BoardLiquid.capture(hex), "Magma crust stays solid");
        hex.addTerrain(new Terrain(Terrains.MAGMA, 2));
        BoardLiquid lava = BoardLiquid.capture(hex);
        assertTrue(lava.molten());
        assertEquals("saxarba/base/base_magma_anim_-3.gif", lava.textures(-1, -8).base());
        assertEquals("saxarba/base/base_magma_anim_10.gif", lava.textures(-1, 20).base());
        for (int type : new int[] { Terrains.MUD, Terrains.SWAMP }) {
            hex.removeAllTerrains();
            hex.addTerrain(new Terrain(type, 1));
            assertEquals(BoardLiquid.NONE, BoardLiquid.capture(hex), "Wet ground keeps its authored terrain surface");
        }
    }

    @Test
    void hazardousPoolsShareWaterGeometryAndMoltenEdgesConnectOnlyToMoltenNeighbors() {
        BoardLiquid toxic = new BoardLiquid(BoardLiquid.Kind.HAZARDOUS, "", 0);
        BoardLiquid lava = new BoardLiquid(BoardLiquid.Kind.MAGMA, "", 0);
        BoardScene waterScene = scene(BoardLiquid.WATER, BoardLiquid.WATER, 2);
        BoardScene toxicScene = scene(toxic, BoardLiquid.WATER, 2);
        Coords high = new Coords(0, 0);
        BoardSurface water = new BoardSurface(waterScene, waterScene.tile(high));
        BoardSurface hazardous = new BoardSurface(toxicScene, toxicScene.tile(high));
        assertEquals(water.faces, hazardous.faces);
        assertEquals(water.waterFaces, hazardous.waterFaces);
        assertEquals(water.waterfalls, hazardous.waterfalls);

        BoardScene lavaScene = scene(lava, lava, -1);
        BoardSurface molten = new BoardSurface(lavaScene, lavaScene.tile(high));
        assertEquals(1, molten.waterfalls.size(), "Adjacent lava levels share a flowing face");
        BoardScene mixedScene = scene(lava, BoardLiquid.WATER, -1);
        assertTrue(new BoardSurface(mixedScene, mixedScene.tile(high)).waterfalls.isEmpty(),
              "A rocky shoreline separates lava from water");
        Vector3 point = BoardGeometry.center(high, 2);
        assertEquals(high, BoardGeometry.pick(lavaScene, new Ray(point.cpy().add(0, 0, 100), new Vector3(0, 0, -1))));
        assertEquals(point.z - BoardGeometry.HEX_SCALE, BoardGeometry.surfaceZ(lavaScene.tile(high)), 0.001f);
    }

    @Test
    void captureRetainsLiquidAndFlowAcrossTacticalRefreshesAndTerrainEdits() throws Exception {
        Hex pool = new Hex(1);
        pool.addTerrain(new Terrain(Terrains.WATER, 2));
        pool.addTerrain(new Terrain(Terrains.HAZARDOUS_LIQUID, 3));
        pool.addTerrain(new Terrain(Terrains.RAPIDS, 2));
        try (GpuBoardFixture fixture = GpuBoardFixture.create(new Board(1, 1, new Hex[] { pool }))) {
            Coords coords = new Coords(0, 0);
            BoardScene.Tile first = fixture.source.takeFrame().scene().tile(coords);
            assertEquals(2, first.waterDepth());
            assertEquals(BoardLiquid.Kind.HAZARDOUS, first.liquid().kind());
            assertEquals(2, first.liquid().rapids());
            assertNull(first.decals(), "Static 2D hazardous and rapids overlays must not cover the animated surface");
            SwingUtilities.invokeAndWait(() -> {
                fixture.source.setVisibleArea(new Rectangle(0, 0, 1, 1));
                fixture.source.refresh();
            });
            assertEquals(first.liquid(), fixture.source.takeFrame().scene().tile(coords).liquid());
            SwingUtilities.invokeAndWait(() -> {
                Hex changed = pool.duplicate();
                changed.removeTerrain(Terrains.HAZARDOUS_LIQUID);
                changed.setTheme("volcano");
                fixture.game.getBoard().setHex(coords, changed);
                fixture.source.refresh();
            });
            var last = fixture.source.takeFrame().scene().tile(coords);
            assertEquals(BoardLiquid.Kind.WATER, last.liquid().kind());
            assertEquals("volcano", last.liquid().theme());
            assertEquals(2, last.waterDepth());
            assertFalse(last.liquid().textures(2, 1).foam().isEmpty());
        }
    }

    private static BoardScene scene(BoardLiquid high, BoardLiquid low, int depth) {
        BoardScene.Pixels pixels = new BoardScene.Pixels(new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB));
        return new BoardScene(0, 1, 2, List.of(
              new BoardScene.Tile(new Coords(0, 0), 2, depth, false, 0, BoardScene.Surface.ROCK,
                    pixels, null, null, null, null, List.of(), List.of(), high),
              new BoardScene.Tile(new Coords(0, 1), 0, depth, false, 0, BoardScene.Surface.ROCK,
                    pixels, null, null, null, null, List.of(), List.of(), low)),
              List.of(), List.of(), -1, "", List.of());
    }
}
