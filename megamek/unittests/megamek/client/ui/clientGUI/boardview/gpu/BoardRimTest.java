/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BoardRimTest {
    private static final Coords CENTER = new Coords(3, 3);
    private final BoardScene.Pixels ground = pixels(0xff505050);
    private final BoardScene.Pixels neutral = pixels(0xff8080ff);

    @AfterEach
    void restoreGeometry() { BoardGeometry.tune(BoardGeometry.DEFAULTS); }

    @Test
    void neutralDetailPreservesBaseReliefAndNeutralBaseAcceptsDetail() {
        Vector3 base = new Vector3(0.4f, -0.2f, 1).nor();
        Vector3 detail = new Vector3(-0.3f, 0.5f, 1).nor();
        Vector3 result = new Vector3();
        for (float alpha : new float[] { 0, 0.2f, 0.62f, 1 }) {
            BoardRim.blendNormal(base, Vector3.Z, alpha, result);
            assertTrue(base.epsilonEquals(result, 0.00001f));
        }
        BoardRim.blendNormal(Vector3.Z, detail, 1, result);
        assertTrue(detail.epsilonEquals(result, 0.00001f));
        BoardRim.blendNormal(base, detail, 0, result);
        assertTrue(base.epsilonEquals(result, 0.00001f));
    }

    @Test
    void rotatesDetailOnAllSixEdgesAndKeepsTheCentreUntouchedAtDifferentScales() {
        GpuAssets assets = assets(pixels(0xff606060), pixels(0xffc080ee));
        for (float scale : new float[] { 0.5f, 1, 2 }) {
            BoardGeometry.tune(new BoardGeometry.Tuning(scale, 0.7f, 1, 18, 0.8f));
            for (int edge = 0; edge < 6; edge++) {
                BoardScene scene = scene(edge, false, false);
                BoardRim.Images material = new BoardRim().material(scene, scene.tile(CENTER), BoardGeometry.floor(scene), assets);
                int index = probe(edge, 0.5f, 9);
                assertEquals(Math.round(0x50 * shade(0x60, 1)), material.color().rgba(index) >>> 24, 1,
                      "A dark mask shades the top layer by its lightness about mid gray");
                int encoded = material.normal().rgba(index);
                Vector3 normal = new Vector3((encoded >>> 24) - 128, (encoded >>> 16 & 255) - 128,
                      (encoded >>> 8 & 255) - 128).nor();
                Vector3 along = BoardGeometry.corner(CENTER, 2, edge + 1).sub(BoardGeometry.corner(CENTER, 2, edge)).nor();
                assertTrue(new Vector3(normal.x, normal.y, 0).nor().dot(new Vector3(along.x, -along.y, 0)) > 0.999f,
                      "A rotated rim must rotate its normal directions, not only its image");
                int centre = 36 * 84 + 42;
                assertEquals(ground.rgba(centre), material.color().rgba(centre));
                assertEquals(neutral.rgba(centre), material.normal().rgba(centre));
            }
        }
    }

    @Test
    void midGrayLeavesTheTopLayerAloneAndCoverageWeightsTheShade() {
        BoardScene scene = scene(0, false, false);
        int index = probe(0, 0.5f, 9);
        BoardRim.Images neutral = new BoardRim().material(scene, scene.tile(CENTER), BoardGeometry.floor(scene),
              assets(pixels(0xff808080), null));
        assertEquals(ground.rgba(index), neutral.color().rgba(index), "Mid gray must leave the top layer as it is");
        BoardRim.Images faded = new BoardRim().material(scene, scene.tile(CENTER), BoardGeometry.floor(scene),
              assets(pixels(0x80606060), null));
        assertEquals(Math.round(0x50 * shade(0x60, 0x80 / 255f)), faded.color().rgba(index) >>> 24, 1,
              "Half coverage takes half the shade");
    }

    /** What one rim sample does to a top-layer channel: gray about mid gray, weighted by coverage and opacity. */
    private static float shade(int gray, float coverage) {
        return 1 + coverage * BoardRim.BLEND_OPACITY * (gray / 128f - 1);
    }

    @Test
    void roadMouthStaysOpenAndRemovingTheCliffRestoresOriginalMaps() {
        GpuAssets assets = assets(pixels(0xff606060), neutral);
        BoardRim rims = new BoardRim();
        for (int edge = 0; edge < 6; edge++) {
            BoardScene scene = scene(edge, true, false);
            BoardRim.Images material = rims.material(scene, scene.tile(CENTER), BoardGeometry.floor(scene), assets);
            int middle = probe(edge, 0.5f, 3);
            assertEquals(ground.rgba(middle), material.color().rgba(middle), "Keep the road approach clear at edge " + edge);
            assertNotEquals(ground.rgba(probe(edge, 0.12f, 3)), material.color().rgba(probe(edge, 0.12f, 3)),
                  "The exposed shoulder keeps its rim at edge " + edge);
        }
        BoardScene level = scene(0, false, true);
        BoardRim.Images material = rims.material(level, level.tile(CENTER), BoardGeometry.floor(level), assets);
        assertSame(ground, material.color());
        assertSame(neutral, material.normal());
    }

    @Test
    void reusesMaterialsForUnchangedInputsAndReleasesUnusedCombinations() {
        GpuAssets assets = assets(pixels(0xfff0f0f0), neutral);
        BoardRim rims = new BoardRim();
        BoardScene scene = scene(0, false, false);
        BoardRim.Images first = rims.material(scene, scene.tile(CENTER), BoardGeometry.floor(scene), assets);
        rims.retainUsed();
        BoardScene equivalent = scene(0, false, false);
        assertSame(first, rims.material(equivalent, equivalent.tile(CENTER), BoardGeometry.floor(equivalent), assets));
        rims.retainUsed();
        rims.retainUsed();
        assertNotSame(first, rims.material(scene, scene.tile(CENTER), BoardGeometry.floor(scene), assets));
    }

    @Test
    void smallCustomTexturesKeepRimCoverageAndMissingDetailNormalsPreserveBaseRelief() {
        BoardScene.Pixels smallGround = pixels(0xff505050, 2, 2);
        BoardScene.Pixels smallNormal = pixels(0xffb060ee, 2, 2);
        BoardScene scene = scene(0, false, false, smallGround, smallNormal);
        BoardRim.Images material = new BoardRim().material(scene, scene.tile(CENTER), BoardGeometry.floor(scene),
              assets(pixels(0xff606060), null));
        assertEquals(84, material.color().width());
        assertEquals(72, material.color().height());
        assertEquals(Math.round(0x50 * shade(0x60, 1)), material.color().rgba(probe(0, 0.5f, 9)) >>> 24, 1);
        assertEquals(smallGround.rgba(0), material.color().rgba(36 * 84 + 42));
        for (int index = 0; index < 84 * 72; index++) {
            assertEquals(smallNormal.rgba(0), material.normal().rgba(index));
        }
    }

    private BoardScene scene(int edge, boolean road, boolean level) {
        return scene(edge, road, level, ground, neutral);
    }

    private BoardScene scene(int edge, boolean road, boolean level, BoardScene.Pixels color, BoardScene.Pixels normal) {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        int direction = BoardGeometry.edgeDirection(edge);
        Coords low = CENTER.translated(direction);
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                Coords coords = new Coords(x, y);
                tiles.add(new BoardScene.Tile(coords, !level && coords.equals(low) ? 0 : 2, -1, false,
                      road && coords.equals(CENTER) ? 1 << direction : 0, BoardScene.Surface.GRASS,
                      color, normal, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 7, 7, tiles, List.of(), List.of(), -1, "", List.of());
    }

    private static int probe(int edge, float along, float inward) {
        Vector3 a = BoardGeometry.corner(CENTER, 2, edge), b = BoardGeometry.corner(CENTER, 2, edge + 1);
        Vector3 direction = b.cpy().sub(a).nor();
        Vector3 point = a.lerp(b, along).mulAdd(new Vector3(-direction.y, direction.x, 0), inward * BoardGeometry.HEX_SCALE);
        float u = 0.5f + (point.x - BoardGeometry.centerX(CENTER)) / BoardGeometry.WIDTH * BoardRim.GROUND_UV_SCALE;
        float v = 0.5f - (point.y - BoardGeometry.centerY(CENTER)) / BoardGeometry.HEIGHT * BoardRim.GROUND_UV_SCALE;
        return (int) (v * 72) * 84 + (int) (u * 84);
    }

    private static GpuAssets assets(BoardScene.Pixels mask, BoardScene.Pixels normal) {
        GpuAssets assets = mock(GpuAssets.class);
        when(assets.inclineMask()).thenReturn(new BoardRim.Images(mask, normal));
        return assets;
    }

    private static BoardScene.Pixels pixels(int color) {
        return pixels(color, 84, 72);
    }

    private static BoardScene.Pixels pixels(int color, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) { image.setRGB(x, y, color); }
        }
        return new BoardScene.Pixels(image);
    }
}
