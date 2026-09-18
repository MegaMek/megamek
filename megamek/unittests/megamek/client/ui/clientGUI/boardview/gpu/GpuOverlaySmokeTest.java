/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.overlay.KeyBindingsOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.PlanetaryConditionsOverlay;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Verifies native overlay pixels survive the Swing-to-Scene2D handoff at large and small window sizes. */
@Tag("on-demand")
class GpuOverlaySmokeTest {
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

    private static void checkNativePixels(BoardScene.Pixels pixels) {
        var stage = GpuBoardTestUi.stage();
        float scale = Gdx.graphics.getWidth() / stage.getWidth();
        int boardHeight = Gdx.graphics.getHeight() - Math.round(GpuBoardUi.TOP_HEIGHT * scale)
              - Math.round(GpuBoardUi.TURN_HEIGHT * scale);
        assertEquals(Gdx.graphics.getBackBufferWidth(), pixels.width());
        assertEquals(Math.round(boardHeight * Gdx.graphics.getBackBufferHeight() / (float) Gdx.graphics.getHeight()),
              pixels.height());
        Image hud = (Image) stage.getRoot().getChildren().first();
        var region = ((TextureRegionDrawable) hud.getDrawable()).getRegion();
        assertEquals(pixels.width(), region.getRegionWidth());
        assertEquals(pixels.height(), region.getRegionHeight());
        assertEquals(pixels.width(), hud.getWidth() * Gdx.graphics.getBackBufferWidth() / stage.getWidth(), 0.001f);
        assertEquals(pixels.height(), hud.getHeight() * Gdx.graphics.getBackBufferHeight() / stage.getHeight(), 0.001f);
    }
}
