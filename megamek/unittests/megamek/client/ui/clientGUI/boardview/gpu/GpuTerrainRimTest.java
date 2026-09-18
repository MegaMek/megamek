/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GpuTerrainRimTest {
    @ParameterizedTest
    @ValueSource(floats = { 0.5f, 1, 2 })
    void unevenAndClippedRimsKeepTheirTexelScaleAndFollowTheSelectedArtwork(float scale) {
        BoardGeometry.Tuning previous = BoardGeometry.tuning();
        try {
            BoardGeometry.tune(new BoardGeometry.Tuning(scale, 0.6f, 0.87f, 18, 0.8f));
            Coords coords = new Coords(1, 1);
            Vector3 a = BoardGeometry.corner(coords, 3, 4);
            Vector3 b = BoardGeometry.corner(coords, 3, 5);
            b.z -= 4 * scale; // A sloping road shoulder, with a short exposed face at one end.
            BoardSurface.Side side = new BoardSurface.Side(a, b, a.z - 2 * scale, b.z - 40 * scale, 4);
            BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 72; y++) {
                for (int x = 0; x < 84; x++) {
                    image.setRGB(x, y, 0x80669944);
                }
            }
            for (BoardScene.Surface surface : BoardScene.Surface.values()) {
                BoardScene.Tile tile = new BoardScene.Tile(coords, 3, -1, false, 0, surface,
                      new BoardScene.Pixels(image), null, null, List.of(), List.of());
                List<List<VertexInfo>> quads = capture(tile, side);
                assertTrue(!quads.isEmpty() && quads.size() <= 16, "Keep the edge low-poly");
                float shallowest = Float.POSITIVE_INFINITY, deepest = Float.NEGATIVE_INFINITY;
                for (List<VertexInfo> quad : quads) {
                    for (int[] pair : new int[][] { { 0, 1 }, { 3, 2 } }) {
                        VertexInfo upper = quad.get(pair[0]), lower = quad.get(pair[1]);
                        float height = upper.position.z - lower.position.z;
                        assertEquals(height / (96 * scale), lower.uv.y - upper.uv.y, 0.00001f,
                              "Clipping and varying rim depth must crop, never stretch, the material");
                        assertEquals(upper.uv.x, lower.uv.x, 0.00001f);
                    }
                    VertexInfo left = quad.get(0), right = quad.get(3);
                    float width = (float) Math.hypot(right.position.x - left.position.x,
                          right.position.y - left.position.y);
                    assertEquals(width / (96 * scale), right.uv.x - left.uv.x, 0.00001f);
                    for (VertexInfo vertex : quad) {
                        assertEquals(0x66 / 255f, vertex.color.r, 0.00001f);
                        assertEquals(0x99 / 255f, vertex.color.g, 0.00001f);
                        assertEquals(0x44 / 255f, vertex.color.b, 0.00001f);
                        assertTrue(vertex.color.a >= 0 && vertex.color.a <= 1);
                        float along = (vertex.position.x - a.x) / (b.x - a.x);
                        assertTrue(vertex.position.z >= side.lowA() + along * (side.lowB() - side.lowA()) - 0.01f);
                        if (vertex.color.a < 0.00001f) {
                            float depth = a.z + along * (b.z - a.z) - vertex.position.z;
                            shallowest = Math.min(shallowest, depth);
                            deepest = Math.max(deepest, depth);
                        }
                    }
                }
                assertTrue(quads.stream().flatMap(List::stream).anyMatch(vertex -> vertex.color.a == 0),
                      "A full rim fades into the geology at its bottom");
                if (surface == BoardScene.Surface.CONCRETE) {
                    assertEquals(shallowest, deepest, 0.001f, "Concrete's rim must stay parallel to the upper edge");
                } else {
                    assertTrue(deepest - shallowest > 0.1f * scale, "Natural terrain must retain its jagged rim");
                }
                assertTrue(quads.stream().flatMap(List::stream).anyMatch(vertex ->
                      Math.abs(vertex.position.z - side.lowA()) < 0.001f && vertex.color.a == 1),
                      "A short wall clips an opaque portion of the rim without squeezing in the full fade");
            }
        } finally {
            BoardGeometry.tune(previous);
        }
    }

    private static List<List<VertexInfo>> capture(BoardScene.Tile tile, BoardSurface.Side side) {
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
        GpuTerrain.cornice(mesh, tile, side);
        return quads;
    }
}
