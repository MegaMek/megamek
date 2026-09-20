/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.client.ui.clientGUI.boardview.BoardTactical;
import megamek.client.ui.clientGUI.boardview.sprite.SensorRangeSprite;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Compare the original painter and GPU range walls with and without map-edge outlines in both cameras. */
@Tag("on-demand")
class GpuRangeWallSmokeTest {
    private static final String[] MODES = { "sensor", "air-sensor", "visual", "dark-visual" };

    private static Board board() {
        int[] levels = { 0, 0, 2, 2, 0, 0, -1, -1, 0 };
        Hex[] hexes = new Hex[81];
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                Hex hex = new Hex(x < 3 ? levels[y] : 0);
                if (x < 2 && (y == 4 || y == 5)) {
                    hex.addTerrain(new Terrain(Terrains.WATER, 4));
                    if (y == 5) {
                        hex.addTerrain(new Terrain(Terrains.ICE, 1));
                    }
                }
                hexes[y * 9 + x] = hex;
            }
        }
        return new Board(9, 9, hexes);
    }

    private static List<SensorRangeSprite> markers(GpuBoardFixture fixture, int mode, boolean before, boolean includeMapBorder) {
        List<SensorRangeSprite> sprites = new ArrayList<>();
        for (int x : new int[] { 0, 4 }) {
            for (int y = 0; y < 9; y++) {
                Coords coords = new Coords(x, y);
                sprites.add(new SensorRangeSprite(fixture.view, mode, coords, x == 0 ? 48 : 6) {
                    @Override
                    protected void paintTactical(Graphics2D graph) {
                        paintRange(graph, includeMapBorder);
                    }

                    @Override
                    protected void drawBorderXC(Graphics2D graphics, Shape fillShape, Shape lineShape) {
                        if (before) {
                            // The original painter: retain its palette, band opacity and inherited dashed stroke.
                            Color color = getColor(getRangeBracket());
                            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), getBorderOpacity()));
                            graphics.fill(fillShape);
                            graphics.setColor(lineColor);
                            graphics.draw(lineShape);
                        } else {
                            super.drawBorderXC(graphics, fillShape, lineShape);
                        }
                    }
                });
            }
        }
        fixture.view.addSprites(sprites);
        fixture.source.refresh();
        return sprites;
    }

    @Test
    void dashesScrollInBothPresentationsAndZeroSpeedRemainsStatic() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            SwingUtilities.invokeAndWait(() -> markers(fixture, SensorRangeSprite.VISUAL, false, false));
            BoardScene scene = fixture.source.takeFrame().scene();
            new Lwjgl3Application(new ApplicationAdapter() {
                @Override
                public void create() {
                    GpuTactical moving = new GpuTactical(4f), stopped = new GpuTactical(0f);
                    GpuTactical splitFrames = new GpuTactical(4f), reverse = new GpuTactical(-4f);
                    var controls = List.of(moving, stopped, splitFrames, reverse);
                    try {
                        controls.forEach(control -> control.update(scene));
                        long builds = moving.builds();
                        BoardCamera camera = new BoardCamera();
                        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        camera.center(BoardGeometry.center(new Coords(1, 4), 0));
                        camera.zoom(0.65f);
                        for (float tilt : new float[] { 0, GpuMarkers.FLAT_TILT_DEGREES - 0.1f,
                              GpuMarkers.FLAT_TILT_DEGREES, GpuMarkers.FLAT_TILT_DEGREES + 0.1f, 55 }) {
                            camera.setIsometric(false);
                            camera.orbit(45, tilt);
                            byte[] initial = draw(moving, camera.camera, 0);
                            byte[] advanced = draw(moving, camera.camera, 0.25f);
                            int changes = 0;
                            for (int i = 0; i < initial.length; i++) {
                                if (initial[i] != advanced[i]) {
                                    changes++;
                                }
                            }
                            assertTrue(changes > 100, "Dashes must visibly move at tilt " + tilt);
                            assertArrayEquals(initial, draw(stopped, camera.camera, 10), "0f must keep the static pattern");
                            draw(splitFrames, camera.camera, 0.125f);
                            assertArrayEquals(advanced, draw(splitFrames, camera.camera, 0.125f),
                                  "Travel must depend on elapsed time rather than frame count");
                            assertArrayEquals(advanced, draw(reverse, camera.camera, 0.75f),
                                  "Negative speed must reverse travel along the same dash pattern");
                            assertArrayEquals(initial, draw(moving, camera.camera, 0.75f), "The pattern repeats seamlessly");
                            draw(splitFrames, camera.camera, 0.75f);
                            draw(reverse, camera.camera, 0.25f);
                            moving.update(scene);
                            assertEquals(builds, moving.builds(), "Animation and camera changes reuse the meshes");
                        }
                        byte[] advanced = draw(moving, camera.camera, 0.25f);
                        moving.update(new BoardScene(scene.boardId(), scene.width(), scene.height(), scene.tiles(),
                              List.of(), List.of(), -1, "", List.of(), null, List.of(), List.of(), List.of(), BoardTactical.EMPTY));
                        moving.update(scene);
                        assertArrayEquals(advanced, draw(moving, camera.camera, 0), "Rebuilding must retain the dash phase");
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                    } catch (Throwable error) {
                        failure.set(error);
                    } finally {
                        controls.forEach(GpuTactical::dispose);
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static byte[] draw(GpuTactical control, Camera camera, float seconds) {
        ScreenUtils.clear(0, 0, 0, 1, true);
        control.render(camera, seconds);
        Pixmap image = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            byte[] pixels = new byte[image.getPixels().remaining()];
            image.getPixels().get(pixels);
            return pixels;
        } finally {
            image.dispose();
        }
    }

    @Test
    void comparesEveryRangeModeInBothGpuCameras() throws Exception {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"), "range-walls");
        assertTrue(output.isDirectory() || output.mkdirs());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private int tick;
                private int comparison;
                private List<SensorRangeSprite> sprites = List.of();
                private GpuTactical probe;
                private long builds;

                private void selectComparison() throws Exception {
                    int mode = comparison / 6;
                    boolean before = comparison % 3 == 0;
                    boolean includeMapBorder = comparison % 3 != 2;
                    SwingUtilities.invokeAndWait(() -> {
                        fixture.view.removeSprites(sprites);
                        sprites = markers(fixture, mode, before, includeMapBorder);
                    });
                    boardCamera.setIsometric(comparison % 6 < 3);
                    var scene = fixture.source.takeFrame().scene();
                    assertEquals(before || mode < SensorRangeSprite.VISUAL ? 0 : includeMapBorder ? 18 : 9,
                          scene.tactical().walls().size());
                    probe.update(scene);
                    builds = probe.builds();
                }

                @Override
                public void render() {
                    try {
                        super.render();
                        tick++;
                        if (tick == 1) {
                            probe = new GpuTactical();
                            boardCamera.center(BoardGeometry.center(new Coords(1, 4), 0));
                            boardCamera.zoom(0.65f);
                            selectComparison();
                        } else if (tick % 5 == 0) {
                            probe.update(fixture.source.takeFrame().scene());
                            assertEquals(builds, probe.builds(), "Camera changes must reuse the range meshes");
                            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                            String name = MODES[comparison / 6] + (comparison % 6 < 3 ? "-isometric" : "-top")
                                  + switch (comparison % 3) {
                                      case 0 -> "-before.png";
                                      case 1 -> "-border-enabled.png";
                                      default -> "-border-disabled.png";
                                  };
                            GpuBoardTestUi.capture(new File(output, name));
                            if (++comparison == MODES.length * 6) {
                                Gdx.app.exit();
                            } else {
                                selectComparison();
                            }
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                @Override
                public void dispose() {
                    if (probe != null) {
                        probe.dispose();
                    }
                    super.dispose();
                }
            }, GpuBoardWindow.configuration(false));
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }
}
