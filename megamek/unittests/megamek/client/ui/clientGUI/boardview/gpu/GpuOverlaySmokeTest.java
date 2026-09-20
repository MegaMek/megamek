/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.overlay.KeyBindingsOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.OverlayImage;
import megamek.client.ui.clientGUI.boardview.overlay.PlanetaryConditionsOverlay;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Verifies native overlay pixels survive the Swing-to-Scene2D handoff at large and small window sizes. */
@Tag("on-demand")
class GpuOverlaySmokeTest {
    @Test
    void nativeFadesAdvanceEveryFrameWithoutRecapturingOrReplacingTextures() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            var configuration = GpuBoardWindow.configuration(false);
            configuration.setWindowedMode(640, 480);
            new Lwjgl3Application(new ApplicationAdapter() {
                private GpuBoardUi ui;
                private GpuBoardSource.Hud snapshot;
                private Texture texture;
                private int sample;
                private int previous = 256;
                private int whiteLevel;
                private float previousY = Float.POSITIVE_INFINITY;

                @Override
                public void create() {
                    ui = new GpuBoardUi(fixture.source, new BoardCamera(), () -> { });
                    ui.resize(640, 480, 1);
                    BufferedImage white = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                    int[] pixels = new int[32 * 32];
                    java.util.Arrays.fill(pixels, 0xffffffff);
                    white.setRGB(0, 0, 32, 32, pixels, 0, 32);
                    BoardScene.Pixels artwork = new BoardScene.Pixels(white);
                    snapshot = new GpuBoardSource.Hud(640, 296, List.of(
                          new GpuBoardSource.HudLayer(artwork, 280, 130, new OverlayImage.Fade(0, 1, 0)),
                          new GpuBoardSource.HudLayer(artwork, 360, 130, new OverlayImage.Fade(0, 0, 1),
                                new OverlayImage.Transition(0, 0, 50, 200_000_000))));
                }

                @Override
                public void render() {
                    try {
                        ScreenUtils.clear(0, 0, 0, 1);
                        if (sample == 6) {
                            var added = new java.util.ArrayList<>(snapshot.layers());
                            added.add(new GpuBoardSource.HudLayer(snapshot.layers().getFirst().pixels(),
                                  20, 20, OverlayImage.Fade.OPAQUE));
                            snapshot = new GpuBoardSource.Hud(snapshot.width(), snapshot.height(), List.copyOf(added));
                        } else if (sample == 9) {
                            snapshot = new GpuBoardSource.Hud(snapshot.width(), snapshot.height(),
                                  List.copyOf(snapshot.layers().subList(0, 2)));
                        }
                        // Keep both timelines and their artwork unchanged while another panel appears and disappears.
                        ui.updateHud(snapshot, sample * 16_666_667L);
                        ui.stage.getViewport().apply();
                        ui.stage.draw();
                        Group hud = (Group) ui.stage.getRoot().getChildren().first();
                        Image fadingOut = (Image) hud.getChildren().get(0);
                        Texture current = ((TextureRegionDrawable) fadingOut.getDrawable()).getRegion().getTexture();
                        if (texture == null) {
                            texture = current;
                        }
                        assertSame(texture, current, "Animating or adding another panel must preserve this texture");
                        int out = redAt(ui, fadingOut);
                        Image fadingIn = (Image) hud.getChildren().get(1);
                        int in = redAt(ui, fadingIn);
                        assertTrue(out < previous, "The rendered fade must advance on all 13 frames");
                        assertTrue(fadingIn.getY() < previousY, "Translation advances on every frame too");
                        previousY = fadingIn.getY();
                        if (sample == 0) {
                            whiteLevel = out;
                            assertTrue(whiteLevel >= 250);
                        } else if (sample == 6) {
                            assertEquals(whiteLevel / 2f, out, 3);
                        } else if (sample == 12) {
                            assertEquals(0, out);
                            Gdx.app.exit();
                        }
                        // Scene2D packs vertex alpha into a float color; allow its byte quantization.
                        assertEquals(whiteLevel, out + in, 4, "The overlays animate independently from the same artwork");
                        previous = out;
                        sample++;
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                @Override
                public void dispose() {
                    ui.dispose();
                }
            }, configuration);
        }
        if (failure.get() != null) {
            throw new AssertionError("Native overlay fade failed", failure.get());
        }
    }

    private static int redAt(GpuBoardUi ui, Image actor) {
        Vector2 point = actor.localToStageCoordinates(new Vector2(actor.getWidth() / 2, actor.getHeight() / 2));
        ui.stage.getViewport().project(point);
        Pixmap pixel = Pixmap.createFromFrameBuffer(
              Math.round(point.x * Gdx.graphics.getBackBufferWidth() / Gdx.graphics.getWidth()),
              Math.round(point.y * Gdx.graphics.getBackBufferHeight() / Gdx.graphics.getHeight()), 1, 1);
        try {
            return pixel.getPixel(0, 0) >>> 24;
        } finally {
            pixel.dispose();
        }
    }

    @Test
    void keyboardAndConditionsPanelsUseNativePixelsAfterResizing() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        GUIPreferences preferences = GUIPreferences.getInstance();
        float originalScale = preferences.getGUIScale();
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setValue(GUIPreferences.GUI_SCALE, 1f);
                BoardView painterView = spy(fixture.view);
                doReturn(mock(ClientGUI.class)).when(painterView).getClientgui();
                KeyBindingsOverlay keys = new KeyBindingsOverlay(painterView);
                PlanetaryConditionsOverlay conditions = new PlanetaryConditionsOverlay(painterView);
                keys.setVisible(true);
                conditions.setVisible(true);
                fixture.view.addOverlay(keys);
                fixture.view.addOverlay(conditions);
            });
            var configuration = GpuBoardWindow.configuration(false);
            configuration.setWindowedMode(3840, 2160);
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private int tick;

                @Override
                public void render() {
                    try {
                        super.render();
                        tick++;
                        if (tick == 5 || tick == 12) {
                            SwingUtilities.invokeAndWait(fixture.source::refresh);
                        } else if (tick == 6 || tick == 13) {
                            checkNativePixels(fixture.source.takeFrame().hud());
                            GpuBoardTestUi.capture(new File(output, "native-overlays-" + Gdx.graphics.getWidth() + ".png"));
                            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        } else if (tick == 7) {
                            assertTrue(Gdx.graphics.setWindowedMode(1280, 800));
                        } else if (tick == 14) {
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }
            }, configuration);
        } finally {
            SwingUtilities.invokeAndWait(() -> preferences.setValue(GUIPreferences.GUI_SCALE, originalScale));
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static void checkNativePixels(GpuBoardSource.Hud snapshot) {
        var stage = GpuBoardTestUi.stage();
        float scale = Gdx.graphics.getWidth() / stage.getWidth();
        int boardHeight = Gdx.graphics.getHeight() - Math.round(GpuBoardUi.TOP_HEIGHT * scale)
              - Math.round(GpuBoardUi.TURN_HEIGHT * scale);
        assertEquals(Gdx.graphics.getBackBufferWidth(), snapshot.width());
        assertEquals(Math.round(boardHeight * Gdx.graphics.getBackBufferHeight() / (float) Gdx.graphics.getHeight()),
              snapshot.height());
        Group hud = (Group) stage.getRoot().getChildren().first();
        assertEquals(snapshot.layers().size(), hud.getChildren().size);
        float densityX = Gdx.graphics.getBackBufferWidth() / stage.getWidth();
        float densityY = Gdx.graphics.getBackBufferHeight() / stage.getHeight();
        for (int index = 0; index < snapshot.layers().size(); index++) {
            GpuBoardSource.HudLayer layer = snapshot.layers().get(index);
            Image actor = (Image) hud.getChildren().get(index);
            var region = ((TextureRegionDrawable) actor.getDrawable()).getRegion();
            assertEquals(layer.pixels().width(), region.getRegionWidth());
            assertEquals(layer.pixels().height(), region.getRegionHeight());
            assertEquals(layer.pixels().width(), actor.getWidth() * densityX, 0.001f);
            assertEquals(layer.pixels().height(), actor.getHeight() * densityY, 0.001f);
            assertEquals(layer.x(), actor.getX() * densityX, 0.001f);
            assertEquals(layer.y(), (hud.getHeight() - actor.getTop()) * densityY, 0.001f);
        }
    }
}
