/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.overlay.UnitOverviewOverlay;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("on-demand")
class GpuUnitHudSmokeTest {
    @Test
    void rendersSmallCardsAndKeepsTheirTexturesWhenCameraOrOtherUnitsChange() throws Exception {
        var prefs = GUIPreferences.getInstance();
        boolean wasVisible = prefs.getShowUnitOverview();
        float originalScale = prefs.getGUIScale();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<UnitOverviewOverlay> overview = new AtomicReference<>();
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            var second = GpuUnitHudTest.addUnits(fixture, 1).getFirst();
            SwingUtilities.invokeAndWait(() -> {
                prefs.setShowUnitOverview(true);
                prefs.setValue(GUIPreferences.GUI_SCALE, 1f);
                overview.set(new UnitOverviewOverlay(GpuUnitHudTest.gui(fixture)));
                fixture.view.addOverlay(overview.get());
            });
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private int tick;
                private Texture firstTexture;
                private BoardScene.Pixels firstCard;
                private BoardScene.Pixels secondCard;
                private BoardScene.Pixels label;

                @Override
                public void render() {
                    try {
                        super.render();
                        tick++;
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        if (tick == 15 || tick == 30 || tick == 45) {
                            SwingUtilities.invokeAndWait(fixture.source::refresh);
                        } else if (tick == 16) {
                            var frame = fixture.source.takeFrame();
                            assertEquals(2, frame.hud().layers().size());
                            for (var layer : frame.hud().layers()) {
                                assertTrue(layer.pixels().width() < frame.hud().width() / 4);
                                assertTrue(layer.pixels().height() < frame.hud().height() / 4);
                            }
                            firstTexture = firstTexture();
                            firstCard = frame.hud().layers().getFirst().pixels();
                            secondCard = frame.hud().layers().get(1).pixels();
                            label = frame.scene().units().getFirst().annotations();
                            boardCamera.setIsometric(!boardCamera.isIsometric());
                            boardCamera.zoom(0.8f);
                        } else if (tick == 31) {
                            var frame = fixture.source.takeFrame();
                            assertSame(firstTexture, firstTexture());
                            assertSame(firstCard, frame.hud().layers().getFirst().pixels());
                            assertSame(label, frame.scene().units().getFirst().annotations());
                            SwingUtilities.invokeAndWait(() -> second.heat = 20);
                        } else if (tick == 46) {
                            var frame = fixture.source.takeFrame();
                            assertSame(firstTexture, firstTexture(), "A neighbouring card update must retain the GPU texture");
                            assertSame(firstCard, frame.hud().layers().getFirst().pixels());
                            assertNotSame(secondCard, frame.hud().layers().get(1).pixels());
                            assertSame(label, frame.scene().units().getFirst().annotations());
                            GpuBoardTestUi.capture(new File(output, "cached-unit-hud.png"));
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                private Texture firstTexture() {
                    Group hud = (Group) GpuBoardTestUi.stage().getRoot().getChildren().first();
                    Image card = (Image) hud.getChildren().first();
                    return ((TextureRegionDrawable) card.getDrawable()).getRegion().getTexture();
                }
            }, GpuBoardWindow.configuration(false));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (overview.get() != null) {
                    prefs.removePreferenceChangeListener(overview.get());
                }
                prefs.setShowUnitOverview(wasVisible);
                prefs.setValue(GUIPreferences.GUI_SCALE, originalScale);
            });
        }
        if (failure.get() != null) {
            throw new AssertionError("Native unit HUD cache failed", failure.get());
        }
    }
}
