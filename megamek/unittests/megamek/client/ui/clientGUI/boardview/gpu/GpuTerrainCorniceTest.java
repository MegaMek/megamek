/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** One skirt hangs from an exposed edge at the height its family configures. */
class GpuTerrainCorniceTest {
    /** A strip drawn as its art is spans one hex edge, so its height follows the art's own aspect. */
    private static float artHeight(float scale, float aspect) {
        return (float) Math.hypot(BoardGeometry.TILE_WIDTH / 4, BoardGeometry.TILE_HEIGHT / 2) * scale / aspect;
    }

    /** Configured levels resize the strip; a family that configures none keeps its art's own scale. */
    private static float skirtHeight(BoardScene.Surface surface, float scale, float aspect) {
        return surface.corniceLevels > 0 ? surface.corniceLevels * BoardGeometry.LEVEL : artHeight(scale, aspect);
    }

    @ParameterizedTest
    @ValueSource(floats = { 0.5f, 1, 2 })
    void skirtHangsItsConfiguredHeightFromTheUpperEdge(float scale) {
        BoardGeometry.Tuning previous = BoardGeometry.tuning();
        try {
            BoardGeometry.tune(new BoardGeometry.Tuning(scale, 0.6f, 0.87f, 18, 0.8f));
            // A wall deeper than the strip: the strip's own height ends the skirt, and its art fades it out.
            BoardSurface.Side side = side(scale, 80, 80);
            for (BoardScene.Surface surface : BoardScene.Surface.values()) {
                float height = skirtHeight(surface, scale, 1);
                List<List<VertexInfo>> quads = capture(tile(surface), side);
                assertTrue(!quads.isEmpty() && quads.size() <= 16, "Keep the exposed edge low-poly");
                for (List<VertexInfo> quad : quads) {
                    VertexInfo topA = quad.get(0), bottomA = quad.get(1), bottomB = quad.get(2), topB = quad.get(3);
                    assertEquals(0, topA.uv.y, 0.00001f, "The strip starts at the cliff top");
                    assertEquals(0, topB.uv.y, 0.00001f);
                    assertEquals(height, topA.position.z - bottomA.position.z, 0.001f,
                          "A deep wall hangs one whole strip, never more");
                    assertEquals(height, topB.position.z - bottomB.position.z, 0.001f);
                    assertEquals(1, bottomA.uv.y, 0.00001f, "A whole strip samples its art end to end");
                    assertEquals(1, bottomB.uv.y, 0.00001f);
                    assertEquals(topA.uv.x, bottomA.uv.x, 0.00001f);
                    assertEquals(topB.uv.x, bottomB.uv.x, 0.00001f);
                    float width = (float) Math.hypot(topB.position.x - topA.position.x,
                          topB.position.y - topA.position.y);
                    assertEquals(width / height, topB.uv.x - topA.uv.x, 0.00001f,
                          "One strip width of art tiles per strip width, so square texels stay square");
                    for (VertexInfo vertex : quad) {
                        assertEquals(1, vertex.color.a, 0.00001f, "The mask, not the tint, carries the fade");
                    }
                }
            }
        } finally {
            BoardGeometry.tune(previous);
        }
    }

    @Test
    void configuredLevelsResizeTheStripWhileZeroKeepsTheArtScale() {
        BoardGeometry.Tuning previous = BoardGeometry.tuning();
        try {
            BoardGeometry.tune(new BoardGeometry.Tuning(1, 0.6f, 0.87f, 18, 0.8f));
            // Art four times wider than tall is shorter than one level, which separates the two behaviours.
            BoardSurface.Side side = side(1, 80, 80);
            float aspect = 4;
            assertTrue(artHeight(1, aspect) < BoardGeometry.LEVEL, "The fixture art must be shorter than a level");
            for (BoardScene.Surface surface : BoardScene.Surface.values()) {
                List<List<VertexInfo>> quads = capture(tile(surface), side, aspect);
                assertTrue(!quads.isEmpty(), "A skirt still covers the wall");
                for (List<VertexInfo> quad : quads) {
                    VertexInfo topA = quad.get(0), bottomA = quad.get(1), bottomB = quad.get(2), topB = quad.get(3);
                    float height = surface.corniceLevels > 0
                          ? surface.corniceLevels * BoardGeometry.LEVEL : artHeight(1, aspect);
                    assertEquals(height, topA.position.z - bottomA.position.z, 0.001f,
                          "The strip hangs the height its family configures");
                    assertEquals(height, topB.position.z - bottomB.position.z, 0.001f);
                    assertEquals(1, bottomA.uv.y, 0.00001f, "The strip samples its art end to end");
                    float width = (float) Math.hypot(topB.position.x - topA.position.x,
                          topB.position.y - topA.position.y);
                    assertEquals(width / (height * aspect), topB.uv.x - topA.uv.x, 0.00001f,
                          "Resizing keeps the art's aspect, so texels stay square");
                }
            }
        } finally {
            BoardGeometry.tune(previous);
        }
    }

    @Test
    void aShortOrSlopingWallCropsTheSkirtWithoutSqueezingIt() {
        BoardGeometry.Tuning previous = BoardGeometry.tuning();
        try {
            BoardGeometry.tune(new BoardGeometry.Tuning(1, 0.6f, 0.87f, 18, 0.8f));
            // A sloping road shoulder with a short exposed face at one end.
            BoardSurface.Side side = side(1, 2, 80);
            for (BoardScene.Surface surface : BoardScene.Surface.values()) {
                float height = skirtHeight(surface, 1, 1);
                boolean cropped = false;
                for (List<VertexInfo> quad : capture(tile(surface), side, 1)) {
                    for (int[] pair : new int[][] { { 0, 1 }, { 3, 2 } }) {
                        VertexInfo top = quad.get(pair[0]), bottom = quad.get(pair[1]);
                        float along = (top.position.x - side.a().x) / (side.b().x - side.a().x);
                        float wall = side.lowA() + along * (side.lowB() - side.lowA());
                        float depth = top.position.z - bottom.position.z;
                        assertTrue(bottom.position.z >= wall - 0.001f, "The skirt never reaches past the wall");
                        assertEquals(Math.min(height, top.position.z - wall) / height, bottom.uv.y,
                              0.00001f, "A clipped skirt loses rows instead of being scaled");
                        assertTrue(depth <= height + 0.001f, "A skirt never hangs past its own art");
                        cropped |= Math.abs(bottom.position.z - wall) < 0.001f;
                    }
                }
                assertTrue(cropped, "The short end of the wall must cut the skirt short");
            }
        } finally {
            BoardGeometry.tune(previous);
        }
    }

    @Test
    void theSkirtWearsTheColorOfTheTopLayerItHangsFrom() {
        BoardGeometry.Tuning previous = BoardGeometry.tuning();
        try {
            BoardGeometry.tune(new BoardGeometry.Tuning(1, 0.6f, 0.87f, 18, 0.8f));
            BoardSurface.Side side = side(1, 80, 80);
            for (List<VertexInfo> quad : capture(painted(0xff3366cc), side)) {
                for (VertexInfo vertex : quad) {
                    assertEquals(0x33 / 255f, vertex.color.r, 0.004f, "The mask is tinted by the hex top");
                    assertEquals(0x66 / 255f, vertex.color.g, 0.004f);
                    assertEquals(0xcc / 255f, vertex.color.b, 0.004f);
                }
            }
            // Without top-layer artwork there is nothing to sample, so the mask stays neutral.
            for (List<VertexInfo> quad : capture(tile(BoardScene.Surface.GRASS), side)) {
                for (VertexInfo vertex : quad) {
                    assertEquals(new Color(Color.WHITE), vertex.color, "A hex without art leaves the mask neutral");
                }
            }
        } finally {
            BoardGeometry.tune(previous);
        }
    }

    private static BoardSurface.Side side(float scale, float wallA, float wallB) {
        Coords coords = new Coords(1, 1);
        Vector3 a = BoardGeometry.corner(coords, 3, 4);
        Vector3 b = BoardGeometry.corner(coords, 3, 5);
        b.z -= 4 * scale;
        return new BoardSurface.Side(a, b, a.z - wallA * scale, b.z - wallB * scale, 4);
    }

    /** A water hex hangs a skirt from its shore and none from the bed an open mouth exposes. */
    @Test
    void aWaterHexHangsItsSkirtExceptAcrossAnOpenMouth() {
        BoardScene scene = waterScene();
        BoardSurface surface = new BoardSurface(scene, scene.tile(RISING));
        List<BoardSurface.Side> sides = surface.sides(scene, BoardGeometry.floor(scene));
        BoardSurface.Side shore = sides.stream().filter(side -> side.edge() == SHORE_EDGE).findFirst()
              .orElseThrow(() -> new AssertionError("The fixture must expose a side toward the lower dry hex"));
        BoardSurface.Side mouth = sides.stream().filter(side -> side.edge() == MOUTH_EDGE).findFirst()
              .orElseThrow(() -> new AssertionError("The fixture must expose a side toward the lower pool"));
        assertTrue(surface.mouth(MOUTH_EDGE), "The fixture must open an mouth into the lower pool");
        assertTrue(GpuTerrain.hangsSkirt(surface, shore), "Its shore hangs one");
        assertFalse(GpuTerrain.hangsSkirt(surface, mouth),
              "A wall the water flows away across starts at the bed, where the fall's own sheet hangs");
        assertTrue(GpuTerrain.hangsSkirt(new BoardSurface(scene, scene.tile(DRY)), shore),
              "A dry hex hangs its own on every wall");
    }

    /** One water hex one level above a second and beside a lower dry hex; the water connects the two pools. */
    private static final Coords RISING = new Coords(0, 0);
    private static final Coords FALLING = RISING.translated(2);
    private static final Coords DRY = RISING.translated(3);
    private static final int MOUTH_EDGE = 5;
    private static final int SHORE_EDGE = 4;

    private static BoardScene waterScene() {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                Coords coords = new Coords(x, y);
                boolean water = coords.equals(RISING) || coords.equals(FALLING);
                tiles.add(new BoardScene.Tile(coords, coords.equals(RISING) ? 1 : 0, water ? 1 : -1, false, 0,
                      BoardScene.Surface.GRASS, null, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 2, 2, tiles, List.of(), List.of(), -1, "", List.of());
    }

    private static BoardScene.Tile tile(BoardScene.Surface surface) {
        // No ground artwork: the skirt carries its own color, so it cannot depend on the hex art any more.
        return new BoardScene.Tile(new Coords(1, 1), 3, -1, false, 0, surface, null, null, null, List.of(), List.of());
    }

    /** A tile whose top layer is one flat color, which is what the skirt samples its tint from. */
    private static BoardScene.Tile painted(int argb) {
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, argb);
            }
        }
        return new BoardScene.Tile(new Coords(1, 1), 3, -1, false, 0, BoardScene.Surface.GRASS,
              new BoardScene.Pixels(image), null, null, List.of(), List.of());
    }

    private static List<List<VertexInfo>> capture(BoardScene.Tile tile, BoardSurface.Side side) {
        return capture(tile, side, 1);
    }

    private static List<List<VertexInfo>> capture(BoardScene.Tile tile, BoardSurface.Side side, float aspect) {
        List<List<VertexInfo>> quads = new ArrayList<>();
        MeshPartBuilder mesh = mock(MeshPartBuilder.class);
        doAnswer(call -> {
            List<VertexInfo> vertices = new ArrayList<>();
            for (int index = 0; index < 4; index++) {
                VertexInfo vertex = call.getArgument(index);
                vertices.add(new VertexInfo().setPos(vertex.position).setNor(vertex.normal)
                      .setCol(vertex.color).setUV(vertex.uv.x, vertex.uv.y));
            }
            quads.add(vertices);
            return null;
        }).when(mesh).rect(any(VertexInfo.class), any(VertexInfo.class), any(VertexInfo.class), any(VertexInfo.class));
        GpuTerrain.cornice(mesh, tile, side, aspect);
        return quads;
    }
}
